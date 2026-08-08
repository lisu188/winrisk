package com.winrisk.game.util;

import java.util.Random;

public class Colors {
    private static final Random rand = new Random();

    private static final GameColor[] colors = {
            GameColor.RED, GameColor.BLUE, GameColor.CYAN,
            GameColor.DARK_GRAY, GameColor.GRAY, GameColor.GREEN,
            GameColor.LIGHT_GRAY, GameColor.MAGENTA, GameColor.YELLOW
    };

    private static final GameColor[] playerColors = {
            new GameColor(0xC0392B),
            new GameColor(0x2E5FA3),
            new GameColor(0x1E8449),
            new GameColor(0xD68910),
            new GameColor(0x7D3C98)
    };

    private static final GameColor[] continentColors = {
            new GameColor(0x8E6E53),
            new GameColor(0x5B7C99),
            new GameColor(0x6B8F71),
            new GameColor(0xA98A45),
            new GameColor(0x9A7B9E),
            new GameColor(0x4F8A8B),
            new GameColor(0xB07A5E),
            new GameColor(0x7C8A5A),
            new GameColor(0x8A7CA8)
    };

    public static final GameColor NEUTRAL = new GameColor(0x5D6D7E);

    private Colors() {
    }

    public static GameColor get(int i) {
        return i < colors.length ? colors[i] : getRandomColor();
    }

    public static GameColor getPlayer(int i) {
        return i >= 0 && i < playerColors.length ? playerColors[i] : getRandomColor();
    }

    public static GameColor getContinent(int i) {
        return i >= 0 && i < continentColors.length ? continentColors[i] : getRandomColor();
    }

    public static GameColor textColorFor(GameColor background) {
        if (background == null) {
            return GameColor.WHITE;
        }
        double luminance = ((0.299 * background.getRed())
                + (0.587 * background.getGreen())
                + (0.114 * background.getBlue())) / 255.0;
        return luminance > 0.55 ? new GameColor(0x1B2631) : GameColor.WHITE;
    }

    public static GameColor getRandomColor() {
        return new GameColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }
}
