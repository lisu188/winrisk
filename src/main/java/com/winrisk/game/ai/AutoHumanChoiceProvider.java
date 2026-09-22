package com.winrisk.game.ai;

import com.winrisk.game.rules.RiskCard;

import java.util.List;

/**
 * Non-interactive {@link HumanChoiceProvider} used for headless play and tests.
 * It always takes the suggested count and only trades cards when required,
 * matching the engine's historical default behaviour for a human seat.
 */
public class AutoHumanChoiceProvider implements HumanChoiceProvider {

    @Override
    public int chooseCount(String prompt, int min, int max, int suggested) {
        if (suggested < min) {
            return min;
        }
        if (suggested > max) {
            return max;
        }
        return suggested;
    }

    @Override
    public int chooseCardSet(List<List<RiskCard>> sets, boolean forced) {
        return forced && !sets.isEmpty() ? 0 : -1;
    }
}
