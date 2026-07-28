package com.winrisk.game.object;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.util.Colors;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Game;
import com.winrisk.game.view.GameSurface;

import java.awt.*;
import java.io.Serializable;
import java.util.stream.Collectors;

public class Field implements Serializable {
    private static final long serialVersionUID = -6700279861283808361L;
    private final FieldList next;
    private final PointF point;
    private int army;
    private int fieldIndex = -1;
    private String displayName;
    private CardSymbol cardSymbol;
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

    private static final int DISC_DIAMETER = 32;
    private static final int RING_WIDTH = 6;
    private static final Color INK = new Color(0x1B2631);
    private static final Color CROSS_REGION_LINK = new Color(120, 110, 95, 90);
    private static final int REGION_LINK_ALPHA = 150;

    public void drawFields(GameSurface graphics) {
        if (player == null) {
            Color fill = continent == null ? Color.WHITE : continent.getColor();
            drawDisc(graphics, fill);
            if (continent != null) {
                graphics.setColor(Colors.textColorFor(fill));
                graphics.drawStringBold(Integer.toString(continent.getBonus()),
                        point.x, point.y);
            }
            drawName(graphics);
            return;
        }
        drawDisc(graphics, player.getColor());
        graphics.setColor(Colors.textColorFor(player.getColor()));
        graphics.drawStringBold(Integer.toString(army), point.x, point.y);
        drawName(graphics);
    }

    private void drawDisc(GameSurface graphics, Color fill) {
        graphics.setColor(INK);
        graphics.drawOval(point.x, point.y,
                DISC_DIAMETER + RING_WIDTH, DISC_DIAMETER + RING_WIDTH);
        graphics.setColor(fill);
        graphics.drawOval(point.x, point.y, DISC_DIAMETER, DISC_DIAMETER);
    }

    private void drawName(GameSurface graphics) {
        graphics.setColor(INK);
        graphics.drawStringSmall(getDisplayName(), point.x,
                point.y + (DISC_DIAMETER / 2) + 12);
    }

    public void drawLines(GameSurface graphics) {
        for (Field aNext : next) {
            // Draw each link once (links are translucent, so drawing them from
            // both endpoints would stack the alpha). Fields without assigned
            // indices - hand-built fixtures - keep the draw-both behaviour.
            if (fieldIndex >= 0 && aNext.getFieldIndex() >= 0
                    && fieldIndex >= aNext.getFieldIndex()) {
                continue;
            }
            Color color = CROSS_REGION_LINK;
            if ((continent != null) && (this.continent == aNext.getContinent())) {
                Color tint = continent.getColor();
                color = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(),
                        REGION_LINK_ALPHA);
            }
            graphics.setColor(color);
            graphics.drawLine(point.x, point.y, aNext.getPoint().x,
                    aNext.getPoint().y);
        }
    }

    public void fight(Field def, Game game) {
        game.attack(this, def);
    }

    public int getArmy() {
        return army;
    }

    public void setArmy(int army) {
        this.army = army;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public String getDisplayName() {
        return displayName == null ? "Territory " + (fieldIndex + 1) : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public CardSymbol getCardSymbol() {
        return cardSymbol == null ? CardSymbol.INFANTRY : cardSymbol;
    }

    /**
     * @return whether a card symbol has been explicitly assigned, as opposed
     * to the {@link #getCardSymbol()} fallback.
     */
    public boolean hasCardSymbol() {
        return cardSymbol != null;
    }

    public void setCardSymbol(CardSymbol cardSymbol) {
        this.cardSymbol = cardSymbol;
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
