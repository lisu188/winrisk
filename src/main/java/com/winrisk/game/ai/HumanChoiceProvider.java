package com.winrisk.game.ai;

import com.winrisk.game.rules.RiskCard;

import java.util.List;

/**
 * Supplies the interactive decisions a human player has to make during a turn:
 * how many dice to roll, how many armies to move after a conquest, and whether
 * to trade in a set of risk cards. Keeping these behind an interface lets the
 * Swing UI prompt the user while headless play (and tests) can use an automatic
 * implementation.
 */
public interface HumanChoiceProvider {

    /**
     * Chooses a count within {@code [min, max]}.
     *
     * @param prompt    human-readable description of the choice
     * @param min       smallest allowed value
     * @param max       largest allowed value
     * @param suggested a sensible default within the range
     * @return the chosen value; implementations must return a value in range
     */
    int chooseCount(String prompt, int min, int max, int suggested);

    /**
     * Chooses which set of risk cards to trade in.
     *
     * @param sets   the legal sets currently available
     * @param forced whether the player is required to trade this turn
     * @return the index of the chosen set within {@code sets}, or {@code -1} to
     * decline trading (ignored when {@code forced})
     */
    int chooseCardSet(List<List<RiskCard>> sets, boolean forced);
}
