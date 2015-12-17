package com.winrisk.game.data;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.map.Map;

import java.io.Serializable;

public class Params implements Serializable {
    private int aiPlayers;

    private boolean attackWithAll;

    private boolean fogOfWar;

    private int humanPlayers = 1;

    private transient String map;

    private boolean skynetMode;

    public AiFactory getAiFactory() {
        return aiFactory;
    }

    public void setAiFactory(AiFactory aiFactory) {
        this.aiFactory = aiFactory;
    }

    private AiFactory aiFactory= i -> PlayerFactory.getRandomAI();

    public Params() {
    }

    public int getAiPlayers() {
        return aiPlayers;
    }

    public int getHumanPlayers() {
        return humanPlayers;
    }

    public Map loadMap() {
        return new Map(this.map);
    }

    public boolean isAttackWithAll() {
        return attackWithAll;
    }

    public boolean isFogOfWar() {
        return fogOfWar;
    }

    public boolean isSkynetMode() {
        return skynetMode;
    }

    public void setAiPlayers(int aiPlayers) {
        this.aiPlayers = aiPlayers;
    }

    public void setHumanPlayers(int humanPlayers) {
        this.humanPlayers = humanPlayers;
    }

    public void setAttackWithAll(boolean attackWithAll) {
        this.attackWithAll = attackWithAll;
    }

    public void setFogOfWar(boolean fogOfWar) {
        this.fogOfWar = fogOfWar;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public void setSkynetMode(boolean skynetMode) {
        this.skynetMode = skynetMode;
    }
}
