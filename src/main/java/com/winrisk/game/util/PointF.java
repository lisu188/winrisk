package com.winrisk.game.util;

import java.io.Serializable;

public class PointF implements Serializable {

    private static final long serialVersionUID = -8103492893081767774L;

    public static float dist(PointF x, PointF y) {
        float dist = 0;
        dist += Math.pow(x.x - y.x, 2);
        dist += Math.pow(x.y - y.y, 2);
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    public int x;

    public int y;

    public PointF(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
