package com.winrisk.gui;

import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.view.GameSurface;
import com.winrisk.game.view.Viewable;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

public class GamePanelTest {
    @Test
    public void testPanelInteractions() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Viewable viewable = new Viewable() {
            @Override public void onAction() {}
            @Override public void onClose() {}
            @Override public void onDraw(GameSurface surface) {}
            @Override public void onEvent(MotionEvent event) {}
            @Override public void onSave(String path) {}
        };
        try {
            GamePanel panel = new GamePanel(viewable);
            BufferedImage img = new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            panel.paintComponent(g);
            for (MouseListener l : panel.getMouseListeners()) {
                MouseEvent press = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, 1, 1, 1, false, MouseEvent.BUTTON1);
                l.mousePressed(press);
                MouseEvent release = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, 1, 1, 1, false, MouseEvent.BUTTON1);
                l.mouseReleased(release);
            }
            g.dispose();
        } catch (HeadlessException ignored) {
        }
    }
}
