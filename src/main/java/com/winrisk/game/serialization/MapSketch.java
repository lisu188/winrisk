package com.winrisk.game.serialization;

import com.winrisk.game.map.Map;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MapSketch implements Serializable, Sketch {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapSketch mapSketch = (MapSketch) o;

        if (continents != null ? !continents.equals(mapSketch.continents) : mapSketch.continents != null) return false;
        if (fields != null ? !fields.equals(mapSketch.fields) : mapSketch.fields != null) return false;
        if (!Arrays.equals(image, mapSketch.image)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = continents != null ? continents.hashCode() : 0;
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (image != null ? Arrays.hashCode(image) : 0);
        return result;
    }

    static private class ContinentSketch implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int bonus;

        private final int color;
        public final ArrayList<Integer> fields;

        public ContinentSketch(Continent continent, Map map) {
            fields = new ArrayList<>();
            fields.addAll(continent.getFields().stream().map(field -> map.getFields().indexOf(field)).collect(Collectors.toList()));
            color = continent.getColorNo();
            bonus = continent.getBonus();
        }

        public Continent recoverContinent() {
            return new Continent(color, bonus);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ContinentSketch that = (ContinentSketch) o;

            if (bonus != that.bonus) return false;
            if (color != that.color) return false;
            if (fields != null ? !fields.equals(that.fields) : that.fields != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = bonus;
            result = 31 * result + color;
            result = 31 * result + (fields != null ? fields.hashCode() : 0);
            return result;
        }
    }

    static private class FieldSketch implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ArrayList<Integer> next;

        private final int x, y;

        public FieldSketch(Field field, Map map) {
            x = field.getPoint().x;
            y = field.getPoint().y;
            next = new ArrayList<>();
            next.addAll(field.getNext().stream().map(nxt -> map.getFields().indexOf(nxt)).collect(Collectors.toList()));
        }

        public ArrayList<Integer> getNext() {
            return next;
        }

        public Field recoverField() {
            return new Field(new PointF(x, y));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldSketch that = (FieldSketch) o;

            if (x != that.x) return false;
            if (y != that.y) return false;
            if (next != null ? !next.equals(that.next) : that.next != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = next != null ? next.hashCode() : 0;
            result = 31 * result + x;
            result = 31 * result + y;
            return result;
        }
    }

    private static final long serialVersionUID = 1763722350848012546L;

    private ArrayList<ContinentSketch> continents;

    private ArrayList<FieldSketch> fields;

    private byte[] image;

    public MapSketch(Game game) {
        this(game.getMap());
    }

    public MapSketch(Map map) {
        fields = new ArrayList<>();
        this.image = map.getImage();
        fields.addAll(map.getFields().stream().map(field -> new FieldSketch(field, map)).collect(Collectors.toList()));
        continents = new ArrayList<>();
        continents.addAll(map.getContinents().stream().map(continent -> new ContinentSketch(continent, map)).collect(Collectors.toList()));
    }

    public void toMap(Map map) {
        for (FieldSketch fieSketch : fields) {
            map.getFields().add(fieSketch.recoverField());
        }
        for (ContinentSketch conSketch : continents) {
            map.getContinents().add(conSketch.recoverContinent());
        }

        for (int i = 0; i < fields.size(); i++) {
            Field field = map.getFields().get(i);
            for (int j : fields.get(i).getNext()) {
                field.addNext(map.getFields().get(j));
            }
        }

        for (int i = 0; i < continents.size(); i++) {
            Continent continent = map.getContinents().get(i);
            for (int j : continents.get(i).fields) {
                if ((j >= map.getFields().size()) || (j < 0)) {
                    throw new RuntimeException("Incorrect map file!!!");
                }
                continent.addField(map.getFields().get(j));
            }
        }
        map.setImage(image);
    }
}
