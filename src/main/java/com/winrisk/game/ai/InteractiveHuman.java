package com.winrisk.game.ai;

import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.view.Game;

import java.util.List;

/**
 * Player interface for a human. Territory selection, attacks and fortification
 * are driven by mouse input on the board, but the per-decision prompts (how
 * many dice to roll, how many armies to move into a conquered territory, and
 * whether to trade risk cards) are delegated to a {@link HumanChoiceProvider}.
 * The Swing UI installs an interactive provider; headless play keeps the
 * automatic default.
 */
public class InteractiveHuman implements PlayerInterface {

    private HumanChoiceProvider choiceProvider = new AutoHumanChoiceProvider();

    public HumanChoiceProvider getChoiceProvider() {
        return choiceProvider;
    }

    public void setChoiceProvider(HumanChoiceProvider choiceProvider) {
        this.choiceProvider = choiceProvider == null
                ? new AutoHumanChoiceProvider() : choiceProvider;
    }

    @Override
    public void attack(Game game) {
        // Driven by board interaction.
    }

    @Override
    public void move(Game game) {
        // Driven by board interaction.
    }

    @Override
    public void reinforce(Game game) {
        // Driven by board interaction.
    }

    @Override
    public int chooseAttackDice(Game game, Field source, Field target, int maxDice) {
        return choiceProvider.chooseCount("Attack dice", 1, maxDice, maxDice);
    }

    @Override
    public int chooseDefenseDice(Game game, Field source, Field target, int maxDice) {
        return choiceProvider.chooseCount("Defense dice", 1, maxDice, maxDice);
    }

    @Override
    public int chooseOccupationTroops(Game game, Field source, Field target, int minimum, int maximum) {
        return choiceProvider.chooseCount("Armies to move into " + target.getDisplayName(),
                minimum, maximum, maximum);
    }

    @Override
    public List<RiskCard> chooseCardTrade(Game game, Player player,
                                          List<List<RiskCard>> legalSets, boolean forced) {
        if (legalSets.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        int choice = choiceProvider.chooseCardSet(legalSets, forced);
        if (choice >= 0 && choice < legalSets.size()) {
            return legalSets.get(choice);
        }
        return forced ? legalSets.get(0) : java.util.Collections.emptyList();
    }
}
