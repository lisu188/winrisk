package com.winrisk.game.object;

import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.cluster.ContinentList;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PatchList;
import com.winrisk.game.view.Game;

import java.awt.*;
import java.util.Random;
import java.util.stream.Collectors;

public class Player {

    private final PlayerInterface playerInterface;
    private int[] cards;
    private Color color;
    private boolean conq;
    private int rein;

    public Player(Color color2, PlayerInterface ifc) {
        this.playerInterface = ifc;
        this.color = color2;
        conq = false;
        cards = new int[3];
        rein = 0;
    }

    public void addCard() {
        if (conq) {
            return;
        }
        conq = true;
        Random gen = new Random();
        cards[gen.nextInt(3)]++;
    }

    public void applyCardBonus() {
        rein(getCardBonus());
        conq = false;
        for (int i = 0; i < 3; i++) {
            if (cards[i] >= 3) {
                cards[i] -= 3;
            }
        }
        if ((cards[0] > 0) && (cards[1] > 0) && (cards[2] > 0)) {
            cards[0]--;
            cards[1]--;
            cards[2]--;
        }
    }

    public void applyContinentBonus(ContinentList continents) {
        rein(getContinentBonus(continents));
    }

    public void applyTerritoryBonus(FieldList fields, Game game) {
        fields.forEach(com.winrisk.game.object.Field::setMin);
        rein(getTerritoryBonus(game));
    }

    private int cardSum() {
        return cards[0] + cards[1] + cards[2];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Player)) {
            return false;
        }
        Player other = (Player) obj;
        if (color == null) {
            if (other.color != null) {
                return false;
            }
        } else if (!color.equals(other.color)) {
            return false;
        }
        return true;
    }

    public FieldList getBorders(Game game) {
        return getFields(game).stream().filter(Field::isBorder)
                .collect(Collectors.toCollection(FieldList::new));
    }

    int getCardBonus() {
        conq = false;
        for (int i = 0; i < 3; i++) {
            if (cards[i] >= 3) {
                return (i + 2) * 2;
            }
        }
        if ((cards[0] > 0) && (cards[1] > 0) && (cards[2] > 0)) {
            return 10;
        }
        return 0;
    }

    public int[] getCards() {
        return cards;
    }

    public Color getColor() {
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
        return game.getFields().stream().filter(field -> Player.this.equals(field.getPlayer())).collect(Collectors.toCollection(FieldList::new));
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
            x.getNext().stream().filter(y -> !vis.contains(y))
                    .forEach(vis::add);
        }
        return vis;
    }

    public boolean isDead(Game game) {
        return getFieldState(game) <= 0;
    }

    public PatchList getPatchList(Game game) {
        PatchList pl = new PatchList();
        Field field;
        for (Field field1 : getFields(game)) {
            field = field1;
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
        boolean tmp = conq;
        if (!player.isDead(game)) {
            throw new RuntimeException("Not dead player.");
        }
        this.cards[0] += player.getCards()[0];
        this.cards[1] += player.getCards()[1];
        this.cards[2] += player.getCards()[2];
        while (cardSum() >= 5) {
            applyCardBonus();
        }
        conq = tmp;
    }

    public void setRein(int rein) {
        this.rein = rein;
    }
}
