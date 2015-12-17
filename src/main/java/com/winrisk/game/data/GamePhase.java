package com.winrisk.game.data;

public enum GamePhase {
    ATTACK, MOVE, REINFORCE, UNDEFINED;

    public GamePhase getNextState() {
        switch (this) {
            case ATTACK:
                return MOVE;
            case MOVE:
                return REINFORCE;
            case REINFORCE:
                return ATTACK;
            case UNDEFINED:
                return REINFORCE;
        }
        return null;
    }

}
