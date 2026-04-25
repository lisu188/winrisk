package com.winrisk.game.view;

import java.awt.*;

public interface GameSurface {
    void drawBackground(byte[] background);

    void drawLine(int x, int y, int x2, int y2);

    void drawOval(int x, int y, int i, int j);

    void drawString(String string, int x, int y);

    default void drawStringLeft(String string, int x, int y) {
        drawString(string, x, y);
    }

    void setColor(Color white);

}
