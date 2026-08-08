package com.winrisk.game.object;

import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.cluster.ContinentList;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PatchList;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.util.GameColor;
import com.winrisk.game.view.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Player {

    private final PlayerInterface playerInterface;
    private final List<RiskCard> riskCards = new ArrayList<>();
    private GameColor color;
    private boolean conqueredTerritoryThisTurn;
    private int rein;
    private Mission mission;
    private boolean neutral;
    private Field headquarters;

    public Player(GameColor color, PlayerInterface ifc) {
        this.playerInterface = ifc;
        this.color = color;
        conqueredTerritoryThisTurn = false;
        rein = 0;
    }

    @Deprecated
    public void addCard() {
        if (conqueredTerritoryThisTurn) {
            return;
        }
        conqueredTerritoryThisTurn = true;
        riskCards.add(RiskCard.wild());
    }

    public void addCard(RiskCard card) {
        if (card != null) {
            riskCards.add(card);
        }
    }

    public void addCards(Collection<RiskCard> cards) {
        riskCards.addAll(cards);
    }

    @Deprecated
    public void applyCardBonus() {
        conqueredTerritoryThisTurn = false;
    }

    public void applyContinentBonus(ContinentList continents) {
        rein(getContinentBonus(continents));
    }

    public void applyTerritoryBonus(FieldList fields, Game game) {
        fields.forEach(Field::setMin);
        rein(getTerritoryBonus(game));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Player)) {
            return false;
        }
        Player other = (Player) obj;
        return color == null ? other.color == null : color.equals(other.color);
    }

    @Override
    public int hashCode() {
        return 31 + (color == null ? 0 : color.hashCode());
    }

    public FieldList getBorders(Game game) {
        return getFields(game).stream().filter(Field::isBorder)
                .collect(Collectors.toCollection(FieldList::new));
    }

    @Deprecated
    public int[] getCards() {
        int[] counts = new int[3];
        for (RiskCard card : riskCards) {
            if (card.getSymbol() == CardSymbol.INFANTRY) {
                counts[0]++;
            } else if (card.getSymbol() == CardSymbol.CAVALRY) {
                counts[1]++;
            } else if (card.getSymbol() == CardSymbol.ARTILLERY) {
                counts[2]++;
            }
        }
        return counts;
    }

    public List<RiskCard> getRiskCards() {
        return riskCards;
    }

    public GameColor getColor() {
        return color;
    }

    int getContinentBonus(ContinentList continents) {
        int r = 0;
        for (Continent continent : continents) {
            Player player = continent.getPlayer();
            if (player == this) {
                r += continent.getBonus();
            }
        }
        return r;
    }

    public FieldList getFields(Game game) {
        return game.getFields().stream()
                .filter(field -> Player.this.equals(field.getPlayer()))
                .collect(Collectors.toCollection(FieldList::new));
    }

    public int getFieldState(Game game) {
        return getFields(game).size();
    }

    public PlayerInterface getPlayerInterface() {
        return playerInterface;
    }

    public int getCurrentReinforcements() {
        return rein;
    }

    int getTerritoryBonus(Game game) {
        int r = getFieldState(game) / 3;
        if (r < 3) {
            r = 3;
        }
        return r;
    }

    public FieldList getVis(Game game) {
        FieldList vis = new FieldList();
        for (Field x : getFields(game)) {
            if (!vis.contains(x)) {
                vis.add(x);
            }
            x.getNext().stream().filter(y -> !vis.contains(y)).forEach(vis::add);
        }
        return vis;
    }

    public boolean isDead(Game game) {
        return !neutral && getFieldState(game) <= 0;
    }

    public PatchList getPatchList(Game game) {
        PatchList pl = new PatchList();
        for (Field field : getFields(game)) {
            if (!pl.has(field)) {
                pl.add(field.getPatch());
            }
        }
        return pl;
    }

    public void obtainField(Field field) {
        field.setPlayer(this);
    }

    public void rein(int i) {
        rein += i;
    }

    public void takeCards(Player player, Game game) {
        boolean tmp = conqueredTerritoryThisTurn;
        if (!player.isDead(game)) {
            throw new RuntimeException("Not dead player.");
        }
        riskCards.addAll(player.getRiskCards());
        player.getRiskCards().clear();
        game.getCardService().tradeAfterElimination(game, this);
        conqueredTerritoryThisTurn = tmp;
    }

    public void setRein(int rein) {
        this.rein = rein;
    }

    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }

    public boolean hasConqueredTerritoryThisTurn() {
        return conqueredTerritoryThisTurn;
    }

    public void setConqueredTerritoryThisTurn(boolean conqueredTerritoryThisTurn) {
        this.conqueredTerritoryThisTurn = conqueredTerritoryThisTurn;
    }

    public boolean isNeutral() {
        return neutral;
    }

    public void setNeutral(boolean neutral) {
        this.neutral = neutral;
    }

    public Field getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(Field headquarters) {
        this.headquarters = headquarters;
    }
}
