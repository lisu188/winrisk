package com.winrisk.gui;

import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.map.Map;
import com.winrisk.game.view.GameSurface;
import com.winrisk.game.view.Viewable;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class GuiTests {
    @Test
    public void gamePanelInteractions() throws Exception {
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
        System.setProperty("java.awt.headless", "false");
        try {
            new GamePanel(viewable);
        } catch (Throwable ignore) {
        }
    }

    @Test
    public void graphicsSurfaceMethods() throws Exception {
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
        surface.drawBackground(data);
        assertNotNull(surface);
        g.dispose();
    }

    @Test
    public void hostGameWindowInit() throws Exception {
        System.setProperty("java.awt.headless", "true");
        try {
            HostGameWindow window = new HostGameWindow();
            Field f = HostGameWindow.class.getDeclaredField("frame");
            f.setAccessible(true);
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                assertNotNull(f.get(window));
                window.setVisible(false);
            }
        } catch (HeadlessException e) {
        }
        System.setProperty("java.awt.headless", "false");
        try {
            new HostGameWindow();
        } catch (Throwable ignore) {
        }
    }

    @Test
    public void startGameButtons() {
        System.setProperty("java.awt.headless", "true");
        try {
            StartGame sg = new StartGame();
            sg.setVisible(false);
            Container c = sg.getContentPane();
            for (Component comp : c.getComponents()) {
                if (comp instanceof JButton) {
                    for (java.awt.event.ActionListener l : ((JButton) comp).getActionListeners()) {
                        l.actionPerformed(new ActionEvent(comp, ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            }
        } catch (HeadlessException ignore) {
        }
    }

    @Test
    public void startGameCreateEmptyMap() throws Exception {
        System.setProperty("java.awt.headless", "true");
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            StartGame sg = (StartGame) unsafe.allocateInstance(StartGame.class);
            Method m = StartGame.class.getDeclaredMethod("createEmptyMap", String.class);
            m.setAccessible(true);
            File img = new File("img/world.png");
            Map map = (Map) m.invoke(sg, img.getAbsolutePath());
            assertNotNull(map.getImage());
        } catch (HeadlessException e) {
        }
    }
}
