package com.winrisk.game.ai;

import com.winrisk.game.view.Game;

public interface PlayerInterface {

    void move(Game game);

    void reinforce(Game game);

    void attack(Game game);

    default void control(Game game) {
        switch (game.getPhase()) {
            case ATTACK:
                attack(game);
                break;
            case MOVE:
                move(game);
                break;
            case REINFORCE:
                reinforce(game);
                break;
            case UNDEFINED:
                throw new RuntimeException("Undefined game state");
        }
    }

    default boolean isInteractive() {
        return !getClass().isAnnotationPresent(ArtificialIntelligence.class);
    }
}
