package com.winrisk.gui;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertNotNull;

public class GraphicsSurfaceTest {
    @Test
    public void testDrawMethods() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GraphicsSurface surface = new GraphicsSurface();
        JPanel panel = new JPanel();
        panel.setSize(10, 10);
        surface.setGraphics(panel, g);
        surface.setColor(Color.WHITE);
        surface.drawLine(0, 0, 1, 1);
        surface.drawOval(5, 5, 4, 4);
        surface.drawString("hi", 3, 3);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        byte[] data = bos.toByteArray();
        surface.drawBackground(data);
        // call again to use cached background path
        surface.drawBackground(data);
        assertNotNull(surface);
        g.dispose();
    }
}
