package com.winrisk.game.rules;

import com.winrisk.game.data.RulesOptions;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.stream.Collectors;

public class RiskCardService {
    private final RiskDeck deck;
    private final RulesOptions options;
    private int tradeCount;

    public RiskCardService(Random random, RulesOptions options) {
        this.deck = new RiskDeck(random);
        this.options = options;
    }

    public RiskDeck getDeck() {
        return deck;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void resetDrawDeck(Game game) {
        resetDrawDeck(game, new HashSet<>());
    }

    public void resetDrawDeck(Game game, Collection<Field> excludedTerritories) {
        deck.reset(game.getMap(), excludedTerritories);
    }

    public List<RiskCard> shuffledTerritoryCards(Game game) {
        return deck.shuffledTerritoryCards(game.getMap());
    }

    public void awardConquestCard(Player player) {
        if (!player.hasConqueredTerritoryThisTurn()) {
            return;
        }
        RiskCard card = deck.draw();
        if (card != null) {
            player.addCard(card);
        }
        player.setConqueredTerritoryThisTurn(false);
    }

    public void beginTurn(Game game, Player player) {
        if (player.isNeutral()) {
            return;
        }
        boolean forced = isTradeForced(player);
        List<List<RiskCard>> sets = findTradeSets(player.getRiskCards());
        if (sets.isEmpty()) {
            return;
        }
        List<RiskCard> chosen = player.getPlayerInterface()
                .chooseCardTrade(game, player, sets, forced);
        if ((chosen == null || chosen.isEmpty()) && forced) {
            chosen = sets.get(0);
        }
        if (chosen != null && !chosen.isEmpty()) {
            trade(game, player, chosen);
        }
        while (isTradeForced(player) && hasSet(player)) {
            trade(game, player, findTradeSets(player.getRiskCards()).get(0));
        }
    }

    public void tradeAfterElimination(Game game, Player player) {
        if (player.getRiskCards().size() < 6) {
            return;
        }
        while (player.getRiskCards().size() > 4 && hasSet(player)) {
            trade(game, player, findTradeSets(player.getRiskCards()).get(0));
        }
    }

    public boolean isTradeForced(Player player) {
        return player.getRiskCards().size() >= 5;
    }

    public boolean hasSet(Player player) {
        return !findTradeSets(player.getRiskCards()).isEmpty();
    }

    public int nextTradeValue() {
        if (options != null && options.isIncrementalCardSetValues()) {
            return 4 + tradeCount;
        }
        int[] officialValues = {4, 6, 8, 10, 12, 15};
        if (tradeCount < officialValues.length) {
            return officialValues[tradeCount];
        }
        return 15 + ((tradeCount - officialValues.length + 1) * 5);
    }

    public void trade(Game game, Player player, List<RiskCard> cards) {
        if (!isLegalSet(cards)) {
            throw new IllegalArgumentException("Cards do not form a legal RISK set");
        }
        if (!hasCards(player.getRiskCards(), cards)) {
            throw new IllegalArgumentException("Player does not hold all cards in the set");
        }
        removeCards(player.getRiskCards(), cards);
        int value = nextTradeValue();
        tradeCount++;
        player.rein(value);
        applyOccupiedTerritoryBonus(player, cards);
        deck.discard(cards);
    }

    private boolean hasCards(List<RiskCard> hand, List<RiskCard> cards) {
        List<RiskCard> remaining = new ArrayList<>(hand);
        for (RiskCard card : cards) {
            if (!remaining.remove(card)) {
                return false;
            }
        }
        return true;
    }

    private void removeCards(List<RiskCard> hand, List<RiskCard> cards) {
        for (RiskCard card : cards) {
            hand.remove(card);
        }
    }

    private void applyOccupiedTerritoryBonus(Player player, List<RiskCard> cards) {
        for (RiskCard card : cards) {
            Field field = card.getField();
            if (field != null && player.equals(field.getPlayer())) {
                field.setArmy(field.getArmy() + 2);
                return;
            }
        }
    }

    public List<List<RiskCard>> findTradeSets(List<RiskCard> cards) {
        List<List<RiskCard>> sets = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                for (int k = j + 1; k < cards.size(); k++) {
                    List<RiskCard> candidate = new ArrayList<>();
                    candidate.add(cards.get(i));
                    candidate.add(cards.get(j));
                    candidate.add(cards.get(k));
                    if (isLegalSet(candidate)) {
                        sets.add(candidate);
                    }
                }
            }
        }
        return sets;
    }

    public boolean isLegalSet(List<RiskCard> cards) {
        if (cards == null || cards.size() != 3) {
            return false;
        }
        long wilds = cards.stream().filter(RiskCard::isWild).count();
        if (wilds > 2) {
            return false;
        }
        List<CardSymbol> symbols = cards.stream()
                .filter(card -> !card.isWild())
                .map(RiskCard::getSymbol)
                .collect(Collectors.toList());
        if (symbols.size() <= 1) {
            return true;
        }
        Set<CardSymbol> unique = new HashSet<>(symbols);
        if (wilds > 0) {
            return unique.size() <= 2;
        }
        return unique.size() == 1 || unique.size() == 3;
    }

    public EnumMap<CardSymbol, Long> composition(List<RiskCard> cards) {
        EnumMap<CardSymbol, Long> counts = new EnumMap<>(CardSymbol.class);
        for (CardSymbol symbol : CardSymbol.values()) {
            counts.put(symbol, 0L);
        }
        for (RiskCard card : cards) {
            counts.put(card.getSymbol(), counts.get(card.getSymbol()) + 1);
        }
        return counts;
    }
}
