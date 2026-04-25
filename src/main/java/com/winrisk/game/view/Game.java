package com.winrisk.game.view;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.data.Params;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.mission.MissionDeck;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.CombatResolver;
import com.winrisk.game.rules.CombatResult;
import com.winrisk.game.rules.OfficialSetup;
import com.winrisk.game.rules.RiskCardService;
import com.winrisk.game.rules.WinConditionEvaluator;
import com.winrisk.game.serialization.MapSketch;
import com.winrisk.game.util.Colors;
import com.winrisk.game.util.PointF;

import java.awt.Color;
import java.util.Random;

public class Game implements FieldListener, Viewable {
    private final FieldDetector fieldDetector = new FieldDetector(this);
    private int curPlayer;
    private GamePhase curState;
    private Map map = new Map();

    private Params params = new Params();

    private PlayerList players;
    private MissionDeck missionDeck;
    private Player missionWinner;
    private Player winner;
    private String winReason;
    private Player neutralPlayer;
    private Random random;
    private RiskCardService cardService;
    private CombatResolver combatResolver;
    private WinConditionEvaluator winConditionEvaluator;
    private boolean maneuverUsed;
    private boolean commanderDieUsed;

    public Game(Params params) {
        this.params = params;
        this.random = params.createRandom();
        this.cardService = new RiskCardService(random, params.getRulesOptions());
        this.combatResolver = new CombatResolver(random);
        this.winConditionEvaluator = new WinConditionEvaluator();
        this.map = params.loadMap();
        startNewGame();
    }

    public boolean end() {
        winner = winConditionEvaluator.evaluate(this);
        winReason = winConditionEvaluator.getReason();
        if (params.getGameMode() == GameMode.SECRET_MISSION
                && "secret mission completed".equals(winReason)) {
            missionWinner = winner;
        }
        return winner != null;
    }

    public int getCurPlayer() {
        return curPlayer;
    }

    public void setCurPlayer(int curPlayer) {
        this.curPlayer = curPlayer;
    }

    @Override
    public FieldList getFieldList() {
        return map.getFields();
    }

    public FieldList getFields() {
        return map.getFields();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;

    }

    public MapSketch getMapSketch() {
        return new MapSketch(this);
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params2) {
        this.params = params2;
    }

    public GamePhase getPhase() {
        return curState;
    }

    public void setPhase(GamePhase curState) {
        this.curState = curState;
    }

    public Player getPlayer() {
        return players.get(curPlayer);
    }

    public PlayerList getPlayers() {
        return players;
    }

    public void setPlayers(PlayerList players) {
        this.players = players;
        if (players != null && !players.isEmpty() && curPlayer >= players.size()) {
            curPlayer = 0;
        }
    }

    public Player getMissionWinner() {
        if (missionWinner == null) {
            end();
        }
        return missionWinner;
    }

    public Player getWinner() {
        if (winner == null) {
            end();
        }
        return winner;
    }

    public String getWinReason() {
        if (winReason == null) {
            end();
        }
        return winReason;
    }

    public Random getRandom() {
        return random;
    }

    public RiskCardService getCardService() {
        return cardService;
    }

    public Player getNeutralPlayer() {
        return neutralPlayer;
    }

    public Player getDefenseController(Player defender) {
        if (defender == neutralPlayer && players != null) {
            return players.stream()
                    .filter(player -> !player.isNeutral())
                    .filter(player -> player != getPlayer())
                    .findFirst()
                    .orElse(getPlayer());
        }
        return defender;
    }

    public boolean isCommanderDieUsed() {
        return commanderDieUsed;
    }

    public void setCommanderDieUsed(boolean commanderDieUsed) {
        this.commanderDieUsed = commanderDieUsed;
    }

    private void incState() {
        curState = curState.getNextState();
        if (curState == GamePhase.REINFORCE) {
            advanceToNextActivePlayer();
            maneuverUsed = false;
            commanderDieUsed = false;
            Player player = getPlayer();
            player.setConqueredTerritoryThisTurn(false);
            cardService.beginTurn(this, player);
            player.applyContinentBonus(map.getContinents());
            player.applyTerritoryBonus(map.getFields(), this);
        } else if (curState == GamePhase.MOVE) {
            cardService.awardConquestCard(getPlayer());
        }
    }

    private void advanceToNextActivePlayer() {
        do {
            curPlayer = (curPlayer + 1) % players.size();
        } while (getPlayer().isNeutral() || getPlayer().isDead(this));
    }

    private void initPlayers() {
        players = new PlayerList();
        int playerCount = params.getAiPlayers() + params.getHumanPlayers();
        validatePlayerCount(playerCount);
        for (int i = 0; i < playerCount; i++) {
            PlayerInterface ifc;
            if (i < params.getHumanPlayers()) {
                ifc = PlayerFactory.getHuman();
            } else {
                ifc = params.getAiFactory().getAi(i);
            }
            players.add(new Player(Colors.get(i), ifc));
        }
        if (params.getGameMode() == GameMode.CLASSIC && playerCount == 2) {
            neutralPlayer = new Player(Color.GRAY, new com.winrisk.game.ai.PlayerAI());
            neutralPlayer.setNeutral(true);
            players.add(neutralPlayer);
        }
    }

    private void validatePlayerCount(int playerCount) {
        int min = params.getGameMode() == GameMode.CLASSIC ? 2 : 3;
        if (playerCount < min || playerCount > 5) {
            throw new IllegalArgumentException(params.getGameMode()
                    + " supports " + min + "-5 active players");
        }
    }

    public void assignMissions() {
        missionDeck = new MissionDeck(map, players, random);
        for (Player player : players) {
            if (!player.isNeutral()) {
                Mission mission = missionDeck.draw(player);
                player.setMission(mission);
            }
        }
    }

    public boolean next() {
        if (end()) {
            return false;
        }
        incState();
        Player player = getPlayer();
        if (player.isDead(this)) {
            curState = GamePhase.UNDEFINED;
            return true;
        }
        player.getPlayerInterface().control(this);
        return !player.getPlayerInterface().isInteractive();
    }

    @Override
    public void onAction() {
        while (next()) ;
    }

    @Override
    public void onClose() {

    }

    @Override
    public void onDraw(GameSurface graphics) {
        map.draw(graphics, this);
        drawHud(graphics);
    }

    @Override
    public void onEvent(MotionEvent event) {
        fieldDetector.feed(event);
    }

    public CombatResult attack(Field from, Field to) {
        return combatResolver.attack(this, from, to);
    }

    public boolean maneuver(Field from, Field to, int troops) {
        if (from == null || to == null) {
            return false;
        }
        if (params.getRulesOptions().isExpandedManeuver()) {
            return from.getPlayer() == getPlayer()
                    && from.getPlayer() == to.getPlayer()
                    && from.getPatch().contains(to)
                    && from.move(to, troops);
        }
        if (maneuverUsed) {
            return false;
        }
        if (from.getPlayer() != getPlayer()) {
            return false;
        }
        if (from.getPlayer() != to.getPlayer()) {
            return false;
        }
        if (!from.getPatch().contains(to)) {
            return false;
        }
        boolean moved = from.move(to, troops);
        maneuverUsed = moved;
        return moved;
    }

    private void drawHud(GameSurface graphics) {
        if (players == null || players.isEmpty() || curPlayer < 0) {
            return;
        }
        Player player = getPlayer();
        graphics.setColor(Color.BLACK);
        int y = 18;
        graphics.drawString("Mode: " + params.getGameMode().toCliValue()
                + " | Phase: " + curState
                + " | Player: " + (curPlayer + 1)
                + (player.isNeutral() ? " neutral" : ""), 10, y);
        y += 16;
        graphics.drawString("Reinforcements: " + player.getCurrentReinforcements()
                + " | Cards: " + player.getRiskCards().size(), 10, y);
        y += 16;
        if (params.getGameMode() == GameMode.SECRET_MISSION && player.getMission() != null) {
            graphics.drawString("Mission: " + player.getMission().getDescription(), 10, y);
        } else if (params.getGameMode() == GameMode.CAPITAL && player.getHeadquarters() != null) {
            graphics.drawString("Headquarters: " + player.getHeadquarters().getDisplayName(), 10, y);
        }
    }

    @Override
    public boolean onFieldDrag(Field from, Field to) {
        switch (getPhase()) {
            case REINFORCE:
                onFromField(from);
                break;

            case ATTACK:
                if (from.getPlayer() != getPlayer()) {
                    return false;
                }
                if (from.getPlayer() == to.getPlayer()) {
                    return false;
                }
                if (!from.getNext().contains(to)) {
                    return false;
                }
                return attack(from, to).isLegal();

            case MOVE:
                return maneuver(from, to, 1);
            case UNDEFINED:
                throw new RuntimeException("Undefined game state");
        }
        return false;
    }

    @Override
    public boolean onFromField(Field field) {
        if (getPhase() != GamePhase.REINFORCE) {
            return false;
        }
        if (!field.getPlayer().equals(getPlayer())) {
            return false;
        }
        field.rein(-1);
        return true;
    }

    @Override
    public boolean onLongClick(Field field) {
        if (getPhase() != GamePhase.REINFORCE) {
            return false;
        }
        if (!field.getPlayer().equals(getPlayer())) {
            return false;
        }
        field.rein(field.getPlayer().getCurrentReinforcements());
        return true;
    }

    @Override
    public void onSave(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onShortClick(Field field) {
        if (getPhase() != GamePhase.REINFORCE) {
            return false;
        }
        if (!field.getPlayer().equals(getPlayer())) {
            return false;
        }
        field.rein(1);
        return true;
    }

    @Override
    public boolean onToField(Field field) {
        return false;

    }

    @Override
    public boolean onVoidClick(PointF point) {
        return false;
    }

    @Override
    public boolean onVoidDrag(PointF from, PointF to) {
        return true;
    }

    private void startNewGame() {
        initPlayers();
        int firstPlayer = new OfficialSetup(this, random).setup();
        curPlayer = firstPlayer - 1;
        curState = GamePhase.UNDEFINED;
        incState();
    }
}
