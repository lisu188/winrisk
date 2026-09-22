package com.winrisk.game.util;

import java.awt.*;
import java.util.Random;

public class Colors {
    private final static Random rand = new Random();

    private final static Color[] colors = {Color.RED, Color.BLUE, Color.CYAN,
            Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY,
            Color.MAGENTA, Color.YELLOW};

    /**
     * Saturated, mutually distinguishable seat colours. Distinct from
     * {@link #NEUTRAL}, which matters because players compare equal by colour.
     */
    private final static Color[] playerColors = {
            new Color(0xC0392B),   // crimson
            new Color(0x2E5FA3),   // royal blue
            new Color(0x1E8449),   // emerald
            new Color(0xD68910),   // amber
            new Color(0x7D3C98)};  // violet

    /**
     * Muted tints for continents and their connection lines, deliberately
     * separate from the player palette so regions never read as ownership.
     * Same length as the legacy palette so every stored colorNo stays valid.
     */
    private final static Color[] continentColors = {
            new Color(0x8E6E53),   // umber
            new Color(0x5B7C99),   // steel blue
            new Color(0x6B8F71),   // sage
            new Color(0xA98A45),   // ochre
            new Color(0x9A7B9E),   // heather
            new Color(0x4F8A8B),   // teal grey
            new Color(0xB07A5E),   // terracotta
            new Color(0x7C8A5A),   // olive
            new Color(0x8A7CA8)};  // lavender grey

    public static final Color NEUTRAL = new Color(0x5D6D7E);

    public static Color get(int i) {
        return i < colors.length ? colors[i] : getRandomColor();
    }

    public static Color getPlayer(int i) {
        return i >= 0 && i < playerColors.length ? playerColors[i] : getRandomColor();
    }

    public static Color getContinent(int i) {
        return i >= 0 && i < continentColors.length ? continentColors[i] : getRandomColor();
    }

    /**
     * @return an ink colour that stays readable on top of the given fill.
     */
    public static Color textColorFor(Color background) {
        if (background == null) {
            return Color.WHITE;
        }
        double luminance = ((0.299 * background.getRed())
                + (0.587 * background.getGreen())
                + (0.114 * background.getBlue())) / 255.0;
        // 0.55 keeps white ink off the amber player colour (luminance 0.57),
        // the one palette entry where white text fails contrast.
        return luminance > 0.55 ? new Color(0x1B2631) : Color.WHITE;
    }

    public static Color getRandomColor() {
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        return new Color(r, g, b);
    }
}
