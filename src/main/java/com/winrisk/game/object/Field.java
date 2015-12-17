package com.winrisk.game.object;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Game;
import com.winrisk.game.view.GameSurface;

import java.awt.*;
import java.io.Serializable;
import java.util.Random;
import java.util.stream.Collectors;

public class Field implements Serializable {
    private static final long serialVersionUID = -6700279861283808361L;
    private final FieldList next;
    private final PointF point;
    private int army;
    private Continent continent;
    private transient int min = 0;
    private Player player;

    public Field(PointF point) {
        this.point = point;
        player = null;
        continent = null;
        next = new FieldList();
    }

    public void addNext(Field field) {
        if (field == this) {
            return;
        }
        if (!next.contains(field)) {
            next.add(field);
        }
        if (!field.getNext().contains(this)) {
            field.addNext(this);
        }
    }

    int borders() {
        int bor = 0;
        for (Field aNext : next) {
            if (aNext.getPlayer() != player) {
                bor++;
            }
        }
        return bor;
    }

    private int dice() {
        return new Random().nextInt(6) + 1;
    }

    public void drawFields(GameSurface graphics) {
        if (player == null) {
            if (continent == null) {
                graphics.setColor(Color.WHITE);
            } else {
                graphics.setColor(continent.getColor());
            }

            graphics.drawOval(point.x, point.y, 25, 25);
            graphics.setColor(Color.BLACK);
            if (continent != null) {
                graphics.drawString(Integer.toString(continent.getBonus()),
                        point.x, point.y);
            }
            return;
        }
        graphics.setColor(player.getColor());
        graphics.drawOval(point.x, point.y, 25, 25);

        graphics.setColor(Color.BLACK);
        graphics.drawString(Integer.toString(army), point.x, point.y);
    }

    public void drawLines(GameSurface graphics) {
        for (Field aNext : next) {
            graphics.setColor(Color.WHITE);

            if (continent != null) {
                if (this.continent == aNext.getContinent()) {
                    graphics.setColor(continent.getColor());
                }
            }
            graphics.drawLine(point.x, point.y, aNext.getPoint().x,
                    aNext.getPoint().y);
        }
    }

    public void fight(Field def, Game game) {
        if (!this.getNext().contains(def)) {
            return;
        }

        if (this.player == def.getPlayer()) {
            return;
        }

        if (this.army == 1) {
            return;
        }

        int atts = this.army - 1;
        if (atts > 3) {
            atts = 3;
        }

        int defs = def.army;
        if (defs > 2) {
            defs = 2;
        }

        int[] attTab = new int[atts];
        int[] defTab = new int[defs];

        for (int i = 0; i < atts; i++) {
            attTab[i] = dice();
        }
        for (int i = 0; i < defs; i++) {
            defTab[i] = dice();
        }

        java.util.Arrays.sort(attTab);
        java.util.Arrays.sort(defTab);

        int count;
        if (atts < defs) {
            count = atts;
        } else {
            count = defs;
        }
        for (int i = 0; i < count; i++) {
            if (attTab[atts - 1 - i] > defTab[defs - 1 - i]) {
                def.army--;
            } else {
                this.army--;
            }
        }

        if (def.army <= 0) {
            player.obtainField(def);
            player.addCard();
            def.army = this.army - 1;
            this.army = 1;
            if (def.getPlayer().isDead(game)) {
                player.takeCards(def.getPlayer(), game);
            }
        } else if (game.getParams().isAttackWithAll()) {
            fight(def, game);
        }
    }

    public int getArmy() {
        return army;
    }

    public void setArmy(int army) {
        this.army = army;
    }

    public Continent getContinent() {
        return continent;
    }

    public void setContinent(Continent con) {
        if (continent == con) {
            return;
        }
        if (continent != null) {
            continent.removeField(this);
        }
        this.continent = con;
        if (continent != null) {
            continent.addField(this);
        }
    }

    public FieldList getEnemy() {
        return next.stream().filter(x -> x.getPlayer() != player)
                .collect(Collectors.toCollection(FieldList::new));
    }

    public FieldList getNext() {
        return next;
    }

    public FieldList getPatch() {
        return getPatch(null);
    }

    private FieldList getPatch(FieldList nxt) {

        if (nxt == null) {
            nxt = new FieldList();
        }
        if (!nxt.contains(this)) {
            nxt.add(this);
        } else {
            return nxt;
        }
        for (Field aNext : next) {
            if (aNext.getPlayer() == player) {
                nxt = aNext.getPatch(nxt);
            }
        }

        return nxt;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public PointF getPoint() {
        return point;
    }

    public float getScale(Game game) {
        float scale;
        FieldList fields = game.getFields();
        float fri = 0, foe = 0;
        float wage;
        for (Field field : (game.getParams().isFogOfWar() ? getPlayer()
                .getVis(game) : fields)) {
            wage = (field.getArmy() / (float) (Math.pow(2,
                    game.getMap().proximity(this, field))));
            if (game.getParams().isSkynetMode()
                    && field.getPlayer().getPlayerInterface().isInteractive()) {
                wage *= 2;
            }
            if (this.player.equals(field.getPlayer())) {
                fri += wage;
            } else {
                foe += wage;
            }
        }
        scale = foe / fri;
        return scale;
    }

    public boolean isBorder() {
        return borders() > 0;
    }

    public boolean move(Field to, int n) {
        if (n == 0) {
            return false;
        }
        if ((this.army - n) < 1) {
            return move(to, n - 1);
        } else {
            this.army -= n;
            to.army += n;
            return true;
        }
    }

    public void rein(int n) {
        if ((army + n) < min) {
            return;
        }
        if (player.getCurrentReinforcements() < n) {
            n = player.getCurrentReinforcements();
        }
        army += n;
        player.rein(-n);
    }

    public void removeNext(Field field) {
        if (field == this) {
            return;
        }
        if (next.contains(field)) {
            next.remove(field);
            field.removeNext(this);
        }
    }

    public void setMin() {
        min = army;
    }

}
