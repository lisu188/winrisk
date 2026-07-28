package com.winrisk.gui;

import com.winrisk.game.view.GameSurface;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GraphicsSurface implements GameSurface {
    private BufferedImage decodedBackground;
    private Graphics2D graphics;
    private BoardViewport viewport;
    private int panelWidth;
    private int panelHeight;

    @Override
    public void drawBackground(byte[] background) {
        if (decodedBackground == null) {
            if (background == null) {
                return;
            }
            try {
                decodedBackground = ImageIO.read(new ByteArrayInputStream(background));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        if (decodedBackground == null) {
            return;
        }
        // With a viewport the graphics is already in board space. The image is
        // stretched into the fixed default board rectangle - not the per-map
        // bounding box - so the art lands identically in the editor and the
        // game regardless of where territories sit. Without a viewport
        // (legacy callers) it fills the panel directly.
        if (viewport != null) {
            graphics.drawImage(decodedBackground, 0, 0,
                    BoardViewport.DEFAULT_WIDTH, BoardViewport.DEFAULT_HEIGHT, null);
        } else {
            graphics.drawImage(decodedBackground, 0, 0, panelWidth, panelHeight, null);
        }
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
    public void drawStringSmall(String string, int x, int y) {
        withFont(graphics.getFont().deriveFont(10f), () -> drawString(string, x, y));
    }

    @Override
    public void drawStringBold(String string, int x, int y) {
        withFont(graphics.getFont().deriveFont(Font.BOLD, 13f),
                () -> drawString(string, x, y));
    }

    private void withFont(Font font, Runnable draw) {
        Font previous = graphics.getFont();
        graphics.setFont(font);
        try {
            draw.run();
        } finally {
            graphics.setFont(previous);
        }
    }

    @Override
    public void setColor(Color white) {
        graphics.setColor(white);
    }

    public void setGraphics(JPanel panel, Graphics graphics) {
        setGraphics(panel, graphics, null);
    }

    /**
     * Prepares the surface for one paint pass. When a viewport is supplied the
     * graphics is translated and uniformly scaled so that every draw call made
     * in board coordinates lands letterboxed within the panel.
     */
    public void setGraphics(JPanel panel, Graphics graphics, BoardViewport viewport) {
        this.graphics = (Graphics2D) graphics;
        this.viewport = viewport;
        this.panelWidth = panel.getWidth();
        this.panelHeight = panel.getHeight();
        this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        this.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        this.graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        this.graphics.setStroke(new BasicStroke(2f));
        if (viewport != null) {
            this.graphics.translate(viewport.getOffsetX(), viewport.getOffsetY());
            this.graphics.scale(viewport.getScale(), viewport.getScale());
        }
    }
}
