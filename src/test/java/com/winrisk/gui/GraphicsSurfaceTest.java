package com.winrisk.gui;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class GraphicsSurfaceTest {
    @Test
    public void testDrawing() throws Exception {
        System.setProperty("java.awt.headless", "true");
        GraphicsSurface surface = new GraphicsSurface();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);

        Graphics2D g = img.createGraphics();
        JPanel panel = new JPanel();
        panel.setSize(10, 10);
        surface.setGraphics(panel, g);
        surface.drawBackground(bos.toByteArray());
        surface.setColor(Color.RED);
        surface.drawLine(0, 0, 1, 1);
        surface.drawOval(5, 5, 2, 2);
        surface.drawString("A", 1, 1);
        g.dispose();
    }
}
