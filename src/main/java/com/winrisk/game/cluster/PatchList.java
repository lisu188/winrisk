package com.winrisk.game.cluster;

import com.winrisk.game.object.Field;

import java.util.ArrayList;

public class PatchList extends ArrayList<FieldList> {

    private static final long serialVersionUID = 8677291006652433063L;

    public boolean has(Field field) {
        for (int i = 0; i < size(); i++) {
            if (get(i).contains(field)) {
                return true;
            }
        }
        return false;
    }
}