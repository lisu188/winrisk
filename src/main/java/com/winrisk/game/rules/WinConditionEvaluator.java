package com.winrisk.game.rules;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.util.List;
import java.util.stream.Collectors;

public class WinConditionEvaluator {
    private String reason;

    public Player evaluate(Game game) {
        reason = null;
        GameMode mode = game.getParams().getGameMode();
        if (mode == GameMode.SECRET_MISSION) {
            return evaluateMissionWin(game);
        }
        if (mode == GameMode.CAPITAL) {
            return evaluateCapitalWin(game);
        }
        return evaluateClassicWin(game);
    }

    public String getReason() {
        return reason;
    }

    private Player evaluateMissionWin(Game game) {
        for (Player player : activePlayers(game)) {
            Mission mission = player.getMission();
            if (mission != null && !player.isDead(game) && mission.isCompleted(game, player)) {
                reason = "secret mission completed";
                return player;
            }
        }
        return null;
    }

    private Player evaluateCapitalWin(Game game) {
        List<Player> active = activePlayers(game);
        for (Player player : active) {
            Field ownHeadquarters = player.getHeadquarters();
            if (ownHeadquarters == null || !player.equals(ownHeadquarters.getPlayer())) {
                continue;
            }
            boolean controlsOpposingHeadquarters = active.stream()
                    .filter(other -> !other.equals(player))
                    .map(Player::getHeadquarters)
                    .allMatch(field -> field != null && player.equals(field.getPlayer()));
            if (controlsOpposingHeadquarters) {
                reason = "captured all opposing headquarters";
                return player;
            }
        }
        return null;
    }

    private Player evaluateClassicWin(Game game) {
        List<Player> alive = activePlayers(game).stream()
                .filter(player -> !player.isDead(game))
                .collect(Collectors.toList());
        if (alive.size() == 1) {
            reason = game.getNeutralPlayer() == null
                    ? "all opponents eliminated"
                    : "opponent eliminated";
            return alive.get(0);
        }
        return null;
    }

    private List<Player> activePlayers(Game game) {
        return game.getPlayers().stream()
                .filter(player -> !player.isNeutral())
                .collect(Collectors.toList());
    }
}
