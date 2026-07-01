package com.winrisk.game.ai;

import com.winrisk.game.object.Field;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HumanChoiceTests {

    private static class RecordingProvider implements HumanChoiceProvider {
        int countToReturn;
        int cardSetToReturn;
        int lastMin;
        int lastMax;
        int lastSuggested;
        boolean lastForced;

        @Override
        public int chooseCount(String prompt, int min, int max, int suggested) {
            lastMin = min;
            lastMax = max;
            lastSuggested = suggested;
            return countToReturn;
        }

        @Override
        public int chooseCardSet(List<List<RiskCard>> sets, boolean forced) {
            lastForced = forced;
            return cardSetToReturn;
        }
    }

    private Field namedField() {
        Field field = new Field(new PointF(0, 0));
        field.setDisplayName("Brazil");
        return field;
    }

    private List<List<RiskCard>> sampleSets() {
        List<List<RiskCard>> sets = new ArrayList<>();
        List<RiskCard> set = new ArrayList<>();
        set.add(RiskCard.wild());
        set.add(RiskCard.wild());
        set.add(RiskCard.wild());
        sets.add(set);
        return sets;
    }

    @Test
    public void autoProviderClampsCountToRange() {
        AutoHumanChoiceProvider provider = new AutoHumanChoiceProvider();
        assertEquals(2, provider.chooseCount("x", 1, 3, 2));
        assertEquals(1, provider.chooseCount("x", 1, 3, 0));
        assertEquals(3, provider.chooseCount("x", 1, 3, 5));
    }

    @Test
    public void autoProviderOnlyTradesWhenForced() {
        AutoHumanChoiceProvider provider = new AutoHumanChoiceProvider();
        assertEquals(0, provider.chooseCardSet(sampleSets(), true));
        assertEquals(-1, provider.chooseCardSet(sampleSets(), false));
        assertEquals(-1, provider.chooseCardSet(new ArrayList<>(), true));
    }

    @Test
    public void interactiveHumanIsInteractiveAndDefaultsToAuto() {
        InteractiveHuman human = new InteractiveHuman();
        assertTrue(human.isInteractive());
        assertTrue(human.getChoiceProvider() instanceof AutoHumanChoiceProvider);
        // A null provider falls back to the automatic one.
        human.setChoiceProvider(null);
        assertTrue(human.getChoiceProvider() instanceof AutoHumanChoiceProvider);
    }

    @Test
    public void interactiveHumanDelegatesDiceAndOccupationChoices() {
        InteractiveHuman human = new InteractiveHuman();
        RecordingProvider provider = new RecordingProvider();
        human.setChoiceProvider(provider);

        provider.countToReturn = 2;
        assertEquals(2, human.chooseAttackDice(null, null, null, 3));
        assertEquals(1, provider.lastMin);
        assertEquals(3, provider.lastMax);
        assertEquals(3, provider.lastSuggested);

        provider.countToReturn = 4;
        assertEquals(4, human.chooseOccupationTroops(null, null, namedField(), 2, 7));
        assertEquals(2, provider.lastMin);
        assertEquals(7, provider.lastMax);
        assertEquals(7, provider.lastSuggested);
    }

    @Test
    public void interactiveHumanTradesChosenSet() {
        InteractiveHuman human = new InteractiveHuman();
        RecordingProvider provider = new RecordingProvider();
        human.setChoiceProvider(provider);
        List<List<RiskCard>> sets = sampleSets();

        provider.cardSetToReturn = 0;
        assertEquals(sets.get(0), human.chooseCardTrade(null, null, sets, false));
        assertFalse(provider.lastForced);
    }

    @Test
    public void interactiveHumanCanDeclineOptionalTrade() {
        InteractiveHuman human = new InteractiveHuman();
        RecordingProvider provider = new RecordingProvider();
        provider.cardSetToReturn = -1;
        human.setChoiceProvider(provider);

        assertTrue(human.chooseCardTrade(null, null, sampleSets(), false).isEmpty());
    }

    @Test
    public void interactiveHumanMustTradeWhenForcedEvenIfDeclined() {
        InteractiveHuman human = new InteractiveHuman();
        RecordingProvider provider = new RecordingProvider();
        provider.cardSetToReturn = -1;
        human.setChoiceProvider(provider);
        List<List<RiskCard>> sets = sampleSets();

        assertEquals(sets.get(0), human.chooseCardTrade(null, null, sets, true));
    }

    @Test
    public void interactiveHumanReturnsEmptyWhenNoSets() {
        InteractiveHuman human = new InteractiveHuman();
        assertTrue(human.chooseCardTrade(null, null, new ArrayList<>(), true).isEmpty());
    }
}
