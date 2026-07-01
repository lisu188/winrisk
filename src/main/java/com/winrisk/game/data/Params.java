package com.winrisk.game.data;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.map.BuiltinMaps;
import com.winrisk.game.map.Map;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

public class Params implements Serializable {
    private static final long serialVersionUID = 1L;

    private int aiPlayers;

    private boolean attackWithAll;

    private boolean fogOfWar;

    private int humanPlayers = 1;

    private transient String map;

    private String builtinMap;

    private boolean randomMap;

    private int randomContinents = 6;

    private int randomFields = 30;

    private Long randomSeed;

    private GameMode gameMode = GameMode.CLASSIC;

    private RulesOptions rulesOptions = new RulesOptions();

    private boolean skynetMode;
    private AiFactory aiFactory = PlayerFactory::getDefaultAI;

    public Params() {
    }

    public AiFactory getAiFactory() {
        return aiFactory;
    }

    public void setAiFactory(AiFactory aiFactory) {
        this.aiFactory = aiFactory;
    }

    public int getAiPlayers() {
        return aiPlayers;
    }

    public void setAiPlayers(int aiPlayers) {
        this.aiPlayers = aiPlayers;
    }

    public int getHumanPlayers() {
        return humanPlayers;
    }

    public void setHumanPlayers(int humanPlayers) {
        this.humanPlayers = humanPlayers;
    }

    public Map loadMap() {
        if (randomMap) {
            return randomSeed == null
                    ? new com.winrisk.game.map.MapGenerator().generate(randomFields, randomContinents)
                    : new com.winrisk.game.map.MapGenerator(randomSeed).generate(randomFields, randomContinents);
        }
        if (builtinMap != null) {
            return BuiltinMaps.byName(builtinMap);
        }
        if (map == null) {
            try {
                map = new File(Map.class.getResource("world.map").toURI())
                        .getAbsolutePath();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "No map configured and default map is unavailable", e);
            }
        }
        return new Map(this.map);
    }

    public boolean isAttackWithAll() {
        return attackWithAll;
    }

    public void setAttackWithAll(boolean attackWithAll) {
        this.attackWithAll = attackWithAll;
    }

    public boolean isFogOfWar() {
        return fogOfWar;
    }

    public void setFogOfWar(boolean fogOfWar) {
        this.fogOfWar = fogOfWar;
    }

    public boolean isSkynetMode() {
        return skynetMode;
    }

    public void setSkynetMode(boolean skynetMode) {
        this.skynetMode = skynetMode;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getBuiltinMap() {
        return builtinMap;
    }

    /**
     * Selects one of the boards shipped with the game by name (see
     * {@link BuiltinMaps#names()}). Takes precedence over a file path set via
     * {@link #setMap(String)}.
     */
    public void setBuiltinMap(String builtinMap) {
        this.builtinMap = builtinMap;
    }

    public void setRandomMap(boolean randomMap) {
        this.randomMap = randomMap;
    }

    public boolean isRandomMap() {
        return randomMap;
    }

    public int getRandomContinents() {
        return randomContinents;
    }

    public void setRandomContinents(int randomContinents) {
        this.randomContinents = randomContinents;
    }

    public int getRandomFields() {
        return randomFields;
    }

    public void setRandomFields(int randomFields) {
        this.randomFields = randomFields;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public Random createRandom() {
        return randomSeed == null ? new Random() : new Random(randomSeed);
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        if (gameMode == null) {
            throw new IllegalArgumentException("Game mode cannot be null");
        }
        this.gameMode = gameMode;
    }

    public RulesOptions getRulesOptions() {
        if (rulesOptions == null) {
            rulesOptions = new RulesOptions();
        }
        return rulesOptions;
    }

    public void setRulesOptions(RulesOptions rulesOptions) {
        if (rulesOptions == null) {
            throw new IllegalArgumentException("Rules options cannot be null");
        }
        this.rulesOptions = rulesOptions;
    }

    public int getActivePlayerCount() {
        return aiPlayers + humanPlayers;
    }
}
