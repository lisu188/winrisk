package com.winrisk.game.ai;

import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.view.Game;

import java.util.Collections;
import java.util.List;

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

    default Field chooseSetupTerritory(Game game, List<Field> availableTerritories) {
        return availableTerritories.isEmpty() ? null : availableTerritories.get(0);
    }

    default Field chooseSetupReinforcement(Game game, Player player, List<Field> ownedTerritories, int troops) {
        return ownedTerritories.isEmpty() ? null : ownedTerritories.get(0);
    }

    default List<RiskCard> chooseCardTrade(Game game, Player player, List<List<RiskCard>> legalSets, boolean forced) {
        if (forced && !legalSets.isEmpty()) {
            return legalSets.get(0);
        }
        return Collections.emptyList();
    }

    default int chooseAttackDice(Game game, Field source, Field target, int maxDice) {
        return maxDice;
    }

    default int chooseDefenseDice(Game game, Field source, Field target, int maxDice) {
        return maxDice;
    }

    default int chooseOccupationTroops(Game game, Field source, Field target, int minimum, int maximum) {
        return maximum;
    }

    default Field chooseHeadquarters(Game game, Player player, List<Field> ownedTerritories) {
        return ownedTerritories.isEmpty() ? null : ownedTerritories.get(0);
    }
}
