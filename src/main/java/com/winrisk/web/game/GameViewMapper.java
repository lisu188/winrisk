package com.winrisk.web.game;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.RulesOptions;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.util.Colors;
import com.winrisk.game.view.Game;
import com.winrisk.web.api.dto.GameView;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a {@link Game} to the client-facing {@link GameView}. Pure and
 * stateless; must be called while holding the session lock.
 */
public final class GameViewMapper {

    private GameViewMapper() {
    }

    public static GameView map(String id, long version, int humanPlayerIndex, Game game) {
        GameView view = new GameView();
        view.id = id;
        view.version = version;
        view.mode = game.getParams().getGameMode().toCliValue();
        view.phase = String.valueOf(game.getPhase());
        view.currentPlayerIndex = game.getCurPlayer();
        view.humanPlayerIndex = humanPlayerIndex;
        view.reinforcements = game.getPlayer().getCurrentReinforcements();
        view.maneuverUsed = game.isManeuverUsed();
        view.commanderDieUsed = game.isCommanderDieUsed();
        boolean over = game.end();
        if (over) {
            view.winnerIndex = game.getPlayers().indexOf(game.getWinner());
            view.winReason = game.getWinReason();
        }
        view.draw = false;
        view.hasBackgroundImage = game.getMap().getImage() != null;
        view.rules = mapRules(game);
        view.fields = mapFields(game, humanPlayerIndex);
        view.continents = mapContinents(game);
        view.links = mapLinks(game);
        view.players = mapPlayers(game);
        mapHumanExtras(game, humanPlayerIndex, view);
        return view;
    }

    private static GameView.RulesView mapRules(Game game) {
        GameView.RulesView rules = new GameView.RulesView();
        rules.attackWithAll = game.getParams().isAttackWithAll();
        rules.fogOfWar = game.getParams().isFogOfWar();
        rules.skynet = game.getParams().isSkynetMode();
        RulesOptions options = game.getParams().getRulesOptions();
        rules.incrementalCardSetValues = options.isIncrementalCardSetValues();
        rules.expandedManeuver = options.isExpandedManeuver();
        rules.attackCardReroll = options.isAttackCardReroll();
        rules.commanderDie = options.isCommanderDie();
        return rules;
    }

    private static List<GameView.FieldView> mapFields(Game game, int humanPlayerIndex) {
        FieldList visible = null;
        if (game.getParams().isFogOfWar() && humanPlayerIndex >= 0
                && humanPlayerIndex < game.getPlayers().size()) {
            visible = game.getPlayers().get(humanPlayerIndex).getVis(game);
        }
        List<GameView.FieldView> fields = new ArrayList<>();
        for (Field field : game.getFields()) {
            if (visible != null && !visible.contains(field)) {
                continue;
            }
            GameView.FieldView fv = new GameView.FieldView();
            fv.index = field.getFieldIndex();
            fv.name = field.getDisplayName();
            fv.x = field.getPoint().x;
            fv.y = field.getPoint().y;
            fv.ownerIndex = field.getPlayer() == null
                    ? -1 : game.getPlayers().indexOf(field.getPlayer());
            fv.army = field.getArmy();
            fv.continentIndex = field.getContinent() == null
                    ? -1 : game.getMap().getContinents().indexOf(field.getContinent());
            fields.add(fv);
        }
        return fields;
    }

    private static List<GameView.ContinentView> mapContinents(Game game) {
        List<GameView.ContinentView> continents = new ArrayList<>();
        for (int i = 0; i < game.getMap().getContinents().size(); i++) {
            Continent continent = game.getMap().getContinents().get(i);
            GameView.ContinentView cv = new GameView.ContinentView();
            cv.index = i;
            cv.bonus = continent.getBonus();
            cv.colorRgb = continent.getColor().getRGB();
            continents.add(cv);
        }
        return continents;
    }

    private static List<int[]> mapLinks(Game game) {
        List<int[]> links = new ArrayList<>();
        for (Field field : game.getFields()) {
            for (Field next : field.getNext()) {
                if (field.getFieldIndex() < next.getFieldIndex()) {
                    links.add(new int[]{field.getFieldIndex(), next.getFieldIndex()});
                }
            }
        }
        return links;
    }

    private static List<GameView.PlayerView> mapPlayers(Game game) {
        List<GameView.PlayerView> players = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            GameView.PlayerView pv = new GameView.PlayerView();
            pv.colorRgb = player.getColor().getRGB();
            pv.textColorRgb = Colors.textColorFor(player.getColor()).getRGB();
            pv.neutral = player.isNeutral();
            pv.ai = !player.getPlayerInterface().isInteractive();
            pv.dead = player.isDead(game);
            pv.territories = player.getFieldState(game);
            pv.cardCount = player.getRiskCards().size();
            pv.reinforcements = player.getCurrentReinforcements();
            players.add(pv);
        }
        return players;
    }

    private static void mapHumanExtras(Game game, int humanPlayerIndex, GameView view) {
        view.hand = new ArrayList<>();
        if (humanPlayerIndex < 0 || humanPlayerIndex >= game.getPlayers().size()) {
            return;
        }
        Player human = game.getPlayers().get(humanPlayerIndex);
        for (RiskCard card : human.getRiskCards()) {
            GameView.CardView cv = new GameView.CardView();
            cv.fieldIndex = card.getFieldIndex();
            cv.symbol = card.getSymbol().name();
            cv.territoryName = card.getTerritoryName();
            view.hand.add(cv);
        }
        view.tradeForced = game.getCardService().isTradeForced(human);
        if (game.getParams().getGameMode() == GameMode.SECRET_MISSION
                && human.getMission() != null) {
            view.missionText = human.getMission().getDescription();
        }
        if (game.getParams().getGameMode() == GameMode.CAPITAL
                && human.getHeadquarters() != null) {
            view.hqName = human.getHeadquarters().getDisplayName();
            view.hqIndex = human.getHeadquarters().getFieldIndex();
        }
    }
}
