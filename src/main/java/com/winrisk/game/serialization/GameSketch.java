package com.winrisk.game.serialization;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.data.RulesOptions;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.mission.MissionSpec;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.view.Game;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of a full {@link Game} in progress, used to save and
 * resume games. Object references (owners, headquarters, card territories,
 * mission targets) are captured as list indices so the object graph can be
 * rebuilt from scratch on load. The reconstruction logic lives in
 * {@link Game#fromSketch}.
 */
public class GameSketch implements Sketch {

    public MapSketch map;
    public ParamsData params;
    public List<PlayerData> players = new ArrayList<>();
    public int[] fieldOwner;
    public int[] fieldArmy;
    public int curPlayer;
    public GamePhase phase;
    public boolean commanderDieUsed;
    public boolean maneuverUsed;
    public int neutralPlayerIndex = -1;
    public int tradeCount;
    public List<CardData> drawPile = new ArrayList<>();
    public List<CardData> discardPile = new ArrayList<>();

    public GameSketch() {
    }

    public GameSketch(Game game) {
        this.map = new MapSketch(game);
        this.params = new ParamsData(game.getParams());

        List<Field> fields = game.getFields();
        List<Player> roster = game.getPlayers();

        for (Player player : roster) {
            players.add(new PlayerData(player, fields));
        }

        int fieldCount = fields.size();
        fieldOwner = new int[fieldCount];
        fieldArmy = new int[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            Field field = fields.get(i);
            fieldArmy[i] = field.getArmy();
            fieldOwner[i] = field.getPlayer() == null ? -1 : roster.indexOf(field.getPlayer());
        }

        this.curPlayer = game.getCurPlayer();
        this.phase = game.getPhase();
        this.commanderDieUsed = game.isCommanderDieUsed();
        this.maneuverUsed = game.isManeuverUsed();
        this.neutralPlayerIndex = game.getNeutralPlayer() == null
                ? -1 : roster.indexOf(game.getNeutralPlayer());
        this.tradeCount = game.getCardService().getTradeCount();
        for (RiskCard card : game.getCardService().getDeck().getDrawPile()) {
            drawPile.add(new CardData(card));
        }
        for (RiskCard card : game.getCardService().getDeck().getDiscardPile()) {
            discardPile.add(new CardData(card));
        }
    }

    public static class ParamsData {
        public GameMode gameMode;
        public boolean fogOfWar;
        public boolean attackWithAll;
        public boolean skynetMode;
        public boolean incrementalCardSetValues;
        public boolean expandedManeuver;
        public boolean attackCardReroll;
        public boolean commanderDie;
        public int humanPlayers;
        public int aiPlayers;
        public Long randomSeed;
        public String builtinMap;

        public ParamsData() {
        }

        public ParamsData(Params source) {
            this.gameMode = source.getGameMode();
            this.fogOfWar = source.isFogOfWar();
            this.attackWithAll = source.isAttackWithAll();
            this.skynetMode = source.isSkynetMode();
            RulesOptions options = source.getRulesOptions();
            this.incrementalCardSetValues = options.isIncrementalCardSetValues();
            this.expandedManeuver = options.isExpandedManeuver();
            this.attackCardReroll = options.isAttackCardReroll();
            this.commanderDie = options.isCommanderDie();
            this.humanPlayers = source.getHumanPlayers();
            this.aiPlayers = source.getAiPlayers();
            this.randomSeed = source.getRandomSeed();
            this.builtinMap = source.getBuiltinMap();
        }

        public Params toParams() {
            Params params = new Params();
            params.setGameMode(gameMode);
            params.setFogOfWar(fogOfWar);
            params.setAttackWithAll(attackWithAll);
            params.setSkynetMode(skynetMode);
            RulesOptions options = new RulesOptions();
            options.setIncrementalCardSetValues(incrementalCardSetValues);
            options.setExpandedManeuver(expandedManeuver);
            options.setAttackCardReroll(attackCardReroll);
            options.setCommanderDie(commanderDie);
            params.setRulesOptions(options);
            params.setHumanPlayers(humanPlayers);
            params.setAiPlayers(aiPlayers);
            params.setRandomSeed(randomSeed);
            params.setBuiltinMap(builtinMap);
            return params;
        }
    }

    public static class PlayerData {
        public int colorRgb;
        public boolean neutral;
        public int reinforcements;
        public boolean conqueredTerritoryThisTurn;
        public int headquartersIndex = -1;
        public String interfaceClass;
        public List<CardData> cards = new ArrayList<>();
        public MissionSpec mission;

        public PlayerData() {
        }

        public PlayerData(Player player, List<Field> fields) {
            this.colorRgb = player.getColor().getRGB();
            this.neutral = player.isNeutral();
            this.reinforcements = player.getCurrentReinforcements();
            this.conqueredTerritoryThisTurn = player.hasConqueredTerritoryThisTurn();
            this.headquartersIndex = player.getHeadquarters() == null
                    ? -1 : fields.indexOf(player.getHeadquarters());
            this.interfaceClass = player.getPlayerInterface().getClass().getName();
            for (RiskCard card : player.getRiskCards()) {
                cards.add(new CardData(card));
            }
            Mission playerMission = player.getMission();
            this.mission = playerMission == null ? null : playerMission.getSpec();
        }
    }

    public static class CardData {
        public int fieldIndex = -1;
        public CardSymbol symbol;

        public CardData() {
        }

        public CardData(RiskCard card) {
            this.fieldIndex = card.getFieldIndex();
            this.symbol = card.getSymbol();
        }
    }
}
