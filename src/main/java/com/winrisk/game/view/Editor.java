package com.winrisk.game.view;

import com.winrisk.game.cluster.ContinentList;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;

public class Editor implements FieldListener, Viewable {
    private final FieldDetector fieldDetector;
    private final Map map;

    public Editor(Map map) {
        this.map = map;
        fieldDetector = new FieldDetector(this);
    }

    public Editor(String path) {
        map = new Map();
        if (path != null) {
            map.load(path);
        }
        fieldDetector = new FieldDetector(this);
    }

    @Override
    public FieldList getFieldList() {
        return map.getFields();
    }

    @Override
    public void onAction() {
        map.addContinent(0);
    }

    @Override
    public void onClose() {

    }

    @Override
    public void onDraw(GameSurface graphics) {
        map.draw(graphics, null);

    }

    @Override
    public void onEvent(MotionEvent event) {
        fieldDetector.feed(event);

    }

    @Override
    public boolean onFieldDrag(Field from, Field to) {
        if (from.getNext().contains(to)) {
            from.removeNext(to);
        } else {
            from.addNext(to);
        }
        return true;
    }

    @Override
    public boolean onFromField(Field field) {
        if (field.getContinent() == null) {
            return false;
        }
        field.getContinent().decBonus();
        return true;
    }

    @Override
    public boolean onLongClick(Field field) {
        map.getFields().delete(field);
        return true;
    }

    @Override
    public void onSave(String path) {
        map.save(path);
    }

    @Override
    public boolean onShortClick(Field field) {
        ContinentList continents = map.getContinents();
        if (continents.size() > 0) {
            int i = continents.indexOf(field.getContinent());
            i = (i + 1) % continents.size();
            field.setContinent(continents.get(i));
            return true;
        }
        return false;
    }

    @Override
    public boolean onToField(Field field) {
        if (field.getContinent() == null) {
            return false;
        }
        field.getContinent().incBonus();
        return true;
    }

    @Override
    public boolean onVoidClick(PointF point) {
        map.addField(point);
        return true;
    }

    @Override
    public boolean onVoidDrag(PointF from, PointF to) {
        return true;
    }

}
