package com.winrisk.game.rules;

import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RiskDeck {
    private final Random random;
    private final Deque<RiskCard> drawPile = new ArrayDeque<>();
    private final List<RiskCard> discardPile = new ArrayList<>();

    public RiskDeck(Random random) {
        this.random = random;
    }

    public static List<RiskCard> territoryCards(Map map) {
        return map.getFields().stream()
                .map(RiskCard::territory)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void reset(Map map, Collection<Field> excludedTerritories) {
        drawPile.clear();
        discardPile.clear();
        List<RiskCard> cards = territoryCards(map).stream()
                .filter(card -> excludedTerritories == null
                        || !excludedTerritories.contains(card.getField()))
                .collect(Collectors.toCollection(ArrayList::new));
        cards.add(RiskCard.wild());
        cards.add(RiskCard.wild());
        Collections.shuffle(cards, random);
        drawPile.addAll(cards);
    }

    public List<RiskCard> shuffledTerritoryCards(Map map) {
        List<RiskCard> cards = territoryCards(map);
        Collections.shuffle(cards, random);
        return cards;
    }

    public RiskCard draw() {
        if (drawPile.isEmpty() && !discardPile.isEmpty()) {
            Collections.shuffle(discardPile, random);
            drawPile.addAll(discardPile);
            discardPile.clear();
        }
        return drawPile.pollFirst();
    }

    public void discard(Collection<RiskCard> cards) {
        discardPile.addAll(cards);
    }

    public int size() {
        return drawPile.size();
    }

    /**
     * @return the draw pile in draw order (next card first). Used for saving.
     */
    public List<RiskCard> getDrawPile() {
        return new ArrayList<>(drawPile);
    }

    /**
     * @return the discard pile. Used for saving.
     */
    public List<RiskCard> getDiscardPile() {
        return new ArrayList<>(discardPile);
    }

    /**
     * Replaces the draw and discard piles wholesale. Used when loading a saved
     * game. The draw pile keeps the supplied order (next card first).
     */
    public void restore(Collection<RiskCard> draw, Collection<RiskCard> discard) {
        drawPile.clear();
        discardPile.clear();
        drawPile.addAll(draw);
        discardPile.addAll(discard);
    }
}
