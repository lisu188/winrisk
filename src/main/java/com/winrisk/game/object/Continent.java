package com.winrisk.game.object;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.util.Colors;

import java.awt.*;
import java.io.Serializable;

public class Continent implements Serializable {

    private static final long serialVersionUID = -755509907464278869L;
    private final int colorNo;
    private final Color color;
    private final FieldList fields;
    private int bonus;

    public Continent(int color, int bonus) {
        this.bonus = bonus;
        this.colorNo = color;
        this.color = Colors.getContinent(color);
        fields = new FieldList();
    }

    public void addField(Field field) {
        if (fields.contains(field)) {
            return;
        }
        fields.add(field);
        field.setContinent(this);

    }

    public void decBonus() {
        bonus -= 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Continent)) {
            return false;
        }
        Continent other = (Continent) obj;
        if (bonus != other.bonus) {
            return false;
        }
        if (color == null) {
            if (other.color != null) {
                return false;
            }
        } else if (!color.equals(other.color)) {
            return false;
        }
        return fields.equals(other.fields);
    }

    public int getBonus() {
        return bonus;
    }

    public int getBorderCount() {
        return getBorders().size();
    }

    FieldList getBorders() {
        FieldList borders = new FieldList();
        for (Field field : fields) {
            boolean isBorder = false;
            for (Field innerField : field.getNext()) {
                if (!field.getContinent().equals(innerField.getContinent())) {
                    isBorder = true;
                    break;
                }
            }
            if (isBorder) {
                borders.add(field);
            }
        }
        return borders;
    }

    public Color getColor() {
        return color;
    }

    public FieldList getFields() {
        return fields;
    }

    public Player getPlayer() {
        for (int i = 1; i < fields.size(); i++) {
            if (fields.get(i).getPlayer() != fields.get(i - 1).getPlayer()) {
                return null;
            }
        }
        return fields.get(0).getPlayer();
    }

    public float getPlayerShare(Player player) {
        float count = 0;
        for (Field field : fields) {
            if (field.getPlayer() == player) {
                count++;
            }
        }
        return count / this.fields.size();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + bonus;
        result = (prime * result) + ((color == null) ? 0 : color.hashCode());
        result = (prime * result) + (fields.hashCode());
        return result;
    }

    public void incBonus() {
        bonus += 1;
    }

    public void removeField(Field field) {
        if (!fields.contains(field)) {
            return;
        }
        fields.remove(field);
        field.setContinent(null);
    }

    public int getColorNo() {
        return colorNo;
    }
}
