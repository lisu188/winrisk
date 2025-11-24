package com.winrisk.game;

import com.winrisk.game.data.Params;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

public class Play {

    private final Params params;
    private final int maxTurns;

    public Play(Params params) {
        this(params, 5000);
    }

    public Play(Params params, int maxTurns) {
        this.params = params;
        this.maxTurns = maxTurns;
    }

    public Player play() {
        System.setProperty("java.awt.headless", "true");
        Game game = new Game(params);
        int turns = 0;
        while (!game.end()) {
            if (turns++ >= maxTurns) {
                throw new IllegalStateException(
                        "Game did not finish within " + maxTurns + " turns");
            }
            game.onAction();
        }
        return getWinner(game);
    }

    public static Player playHeadless(Params params) {
        return new Play(params).play();
    }

    public static Player playWithDefaults() {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(6);
        return playHeadless(params);
    }

    private Player getWinner(Game game) {
        return game.getWinner();
    }

    public static void main(String[] args) {
        playWithDefaults();
    }
}
