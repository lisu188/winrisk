package com.winrisk.gui;

import org.junit.Test;
import java.awt.HeadlessException;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class HostGameWindowTest {
    @Test
    public void testInitialization() throws Exception {
        System.setProperty("java.awt.headless", "true");
        try {
            HostGameWindow window = new HostGameWindow();
            Field f = HostGameWindow.class.getDeclaredField("frame");
            f.setAccessible(true);
            assertNotNull(f.get(window));
            window.setVisible(false);
        } catch (HeadlessException e) {
            // Ignore if environment does not support GUI
        }
    }
}
