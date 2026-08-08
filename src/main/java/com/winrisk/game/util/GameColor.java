package com.winrisk.game.util;

import java.io.Serializable;

public final class GameColor implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final GameColor RED = new GameColor(0xFF0000);
    public static final GameColor BLUE = new GameColor(0x0000FF);
    public static final GameColor CYAN = new GameColor(0x00FFFF);
    public static final GameColor DARK_GRAY = new GameColor(0x404040);
    public static final GameColor GRAY = new GameColor(0x808080);
    public static final GameColor GREEN = new GameColor(0x00FF00);
    public static final GameColor LIGHT_GRAY = new GameColor(0xC0C0C0);
    public static final GameColor MAGENTA = new GameColor(0xFF00FF);
    public static final GameColor YELLOW = new GameColor(0xFFFF00);
    public static final GameColor WHITE = new GameColor(0xFFFFFF);

    private final int argb;

    public GameColor(int rgb) {
        this(rgb, false);
    }

    public GameColor(int value, boolean hasAlpha) {
        this.argb = hasAlpha ? value : (0xFF000000 | (value & 0x00FFFFFF));
    }

    public GameColor(float red, float green, float blue) {
        this(toByte(red), toByte(green), toByte(blue));
    }

    public GameColor(int red, int green, int blue) {
        this.argb = 0xFF000000
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    private static int toByte(float value) {
        if (Float.isNaN(value)) {
            return 0;
        }
        return Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    public int getRGB() {
        return argb;
    }

    public int getAlpha() {
        return (argb >>> 24) & 0xFF;
    }

    public int getRed() {
        return (argb >>> 16) & 0xFF;
    }

    public int getGreen() {
        return (argb >>> 8) & 0xFF;
    }

    public int getBlue() {
        return argb & 0xFF;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof GameColor && ((GameColor) other).argb == argb);
    }

    @Override
    public int hashCode() {
        return argb;
    }

    @Override
    public String toString() {
        return String.format("#%08X", argb);
    }
}
