package com.winrisk.game.cluster;

import com.winrisk.game.object.Field;
import com.winrisk.game.util.Arrays;
import com.winrisk.game.view.Game;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class FieldList extends ArrayList<Field> {

    private static final long serialVersionUID = -3868842508580334811L;

    public FieldList(FieldList next) {
        super(next);
    }

    public FieldList() {

    }

    public void delete(Field field) {

        remove(field);

        FieldList nxt = new FieldList(field.getNext());
        if (field.getContinent() != null) {
            field.getContinent().removeField(field);
        }
        for (Field f : nxt) {
            f.removeNext(field);
        }

    }

    public FieldList getHuman() {
        return this.stream().filter(field -> field.getPlayer().getPlayerInterface().isInteractive()).collect(Collectors.toCollection(FieldList::new));
    }

    public Field getStrong(Game game) {
        float[] scale = new float[size()];
        for (int i = 0; i < scale.length; i++) {
            scale[i] = get(i).getScale(game);
        }
        return get(Arrays.indMin(scale));
    }

    public Field getWeak(Game game) {
        float[] scale = new float[size()];
        for (int i = 0; i < scale.length; i++) {
            scale[i] = get(i).getScale(game);
        }
        return get(Arrays.indMax(scale));
    }

    public void sort(Game game) {
        FieldList sorted = new FieldList();
        float[] scale = new float[size()];
        for (int i = 0; i < scale.length; i++) {
            scale[i] = get(i).getScale(game);
        }
        Field strong;
        for (int i = 0; i < size(); i++) {
            strong = get(Arrays.indMax(scale));
            scale[Arrays.indMax(scale)] = -1;
            sorted.add(strong);
        }

        this.clear();
        for (int i = 0; i < sorted.size(); i++) {
            add(sorted.get(sorted.size() - 1 - i));
        }
    }
}
