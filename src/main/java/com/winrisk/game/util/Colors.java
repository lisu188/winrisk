package com.winrisk.game.util;

import java.awt.*;
import java.util.Random;

public class Colors {
    private final static Random rand = new Random();

    private final static Color[] colors = {Color.RED, Color.BLUE, Color.CYAN,
            Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY,
            Color.MAGENTA, Color.YELLOW};

    public static Color get(int i) {
        return i < colors.length ? colors[i] : getRandomColor();
    }

    public static Color getRandomColor() {
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        return new Color(r, g, b);
    }
}
