package com.winrisk.game.util;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;

class Points {
    private static final int distMin = 25;

    public static Field search(FieldList fields, PointF p) {
        Field tmp;
        float dist[] = new float[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            tmp = fields.get(i);
            dist[i] = 0;
            dist[i] += Math.pow(p.x - tmp.getPoint().x, 2);
            dist[i] += Math.pow(p.y - tmp.getPoint().y, 2);
            dist[i] = (float) Math.sqrt(dist[i]);
        }
        int ind = -1;
        float min = distMin;
        for (int i = 0; i < fields.size(); i++) {
            if (dist[i] < min) {
                ind = i;
                min = dist[i];
            }
        }
        if (ind == -1) {
            return null;
        }
        return fields.get(ind);
    }
}
