package com.winrisk.game.util;

import java.io.Serializable;

public class PointF implements Serializable {

    private static final long serialVersionUID = -8103492893081767774L;
    public int x;
    public int y;

    public PointF(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static float dist(PointF x, PointF y) {
        double dx = (double) x.x - y.x;
        double dy = (double) x.y - y.y;
        return (float) Math.hypot(dx, dy);
    }
}
