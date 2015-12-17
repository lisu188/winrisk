package com.winrisk.game.view;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.serialization.MapSketch;
import com.winrisk.game.util.Colors;
import com.winrisk.game.util.PointF;

import java.util.Random;

public class Game implements FieldListener, Viewable {
    private int curPlayer;

    private GamePhase curState;
    private final FieldDetector fieldDetector = new FieldDetector(this);

    private Map map = new Map();

    private Params params = new Params();

    private PlayerList players;

    public Game(Params params) {
        this.params = params;
        this.map = params.loadMap();
        startNewGame();
    }

    public boolean end() {
        int alive = 0;
        for (Player player : players) {
            if (!player.isDead(this)) {
                alive++;
            }
        }
        return (alive == 1);
    }

    public int getCurPlayer() {
        return curPlayer;
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

    public MapSketch getMapSketch() {
        return new MapSketch(this);
    }

    public Params getParams() {
        return params;
    }

    public GamePhase getPhase() {
        return curState;
    }

    public Player getPlayer() {
        return players.get(curPlayer);
    }

    public PlayerList getPlayers() {
        return players;
    }

    private void incState() {
        curState = curState.getNextState();
        if (curState == GamePhase.REINFORCE) {
            curPlayer = (curPlayer + 1) % players.size();
            getPlayer().applyCardBonus();
            getPlayer().applyContinentBonus(map.getContinents());
            getPlayer().applyTerritoryBonus(map.getFields(), this);
        }
    }

    private void initFields() {
        Random gen = new Random();
        int fields = map.getFields().size();
        for (int i = 0; i < fields; i++) {
            map.getFields().get(i).setArmy(gen.nextInt(9) + 1);
        }
        for (int i = 0; i < fields; i++) {
            int player = gen.nextInt(players.size());
            players.get(player).obtainField(map.getFields().get(i));
        }
    }

    private void initPlayers() {
        players = new PlayerList();
        int playerCount = params.getAiPlayers() + params.getHumanPlayers();
        for (int i = 0; i < (map.getFields().size() < playerCount ? map
                .getFields().size() : playerCount); i++) {
            PlayerInterface ifc;
            if (i < params.getHumanPlayers()) {
                ifc = PlayerFactory.getHuman();
            } else {
                ifc = params.getAiFactory().getAi(i);
            }
            players.add(new Player(Colors.get(i), ifc));
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
    }

    @Override
    public void onEvent(MotionEvent event) {
        fieldDetector.feed(event);
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
                from.fight(to, this);
                return true;

            case MOVE:
                if (from.getPlayer() != getPlayer()) {
                    return false;
                }
                if (!from.getPatch().contains(to)) {
                    return false;
                }
                from.move(to, 1);
                return true;
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

    public void setCurPlayer(int curPlayer) {
        this.curPlayer = curPlayer;
    }

    public void setMap(Map map) {
        this.map = map;

    }

    public void setParams(Params params2) {
        this.params = params2;
    }

    public void setPhase(GamePhase curState) {
        this.curState = curState;
    }

    public void setPlayers(PlayerList players) {
        this.players = players;
    }

    private void startNewGame() {
        initPlayers();
        initFields();
        curPlayer = -1;
        curState = GamePhase.UNDEFINED;
        incState();
    }
}
