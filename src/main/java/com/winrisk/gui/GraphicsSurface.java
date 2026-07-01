package com.winrisk.gui;

import com.winrisk.game.view.GameSurface;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GraphicsSurface implements GameSurface {
    private BufferedImage cache = null;
    private Graphics graphics;
    private int x;
    private int y;

    @Override
    public void drawBackground(byte[] background) {
        if (cache == null) {
            if (background == null) {
                return;
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(background);
            try {
                cache = ImageIO.read(bis);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            BufferedImage dimg = new BufferedImage(x, y, cache.getType());
            Graphics2D g = dimg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(cache, 0, 0, x, y, 0, 0, cache.getWidth(),
                    cache.getHeight(), null);
            g.dispose();
            cache = dimg;
        }
        graphics.drawImage(cache, 0, 0, null);

    }

    @Override
    public void drawLine(int x, int y, int x2, int y2) {
        graphics.drawLine(x, y, x2, y2);
    }

    @Override
    public void drawOval(int x, int y, int i, int j) {
        graphics.fillOval((int) (x - (i / 2F)), (int) (y - (j / 2F)), i, j);
    }

    @Override
    public void drawString(String string, int x, int y) {
        graphics.drawString(string, (int) (x - (graphics.getFontMetrics()
                .stringWidth(string) / 2F)), (int) (y + (graphics
                .getFontMetrics().getHeight() / 4F)));

    }

    @Override
    public void drawStringLeft(String string, int x, int y) {
        graphics.drawString(string, x, (int) (y + (graphics
                .getFontMetrics().getHeight() / 4F)));
    }

    @Override
    public void setColor(Color white) {
        graphics.setColor(white);
    }

    public void setGraphics(JPanel panel, Graphics graphics) {
        this.graphics = graphics;
        x = panel.getWidth();
        y = panel.getHeight();
    }
}
