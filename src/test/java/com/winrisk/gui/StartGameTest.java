package com.winrisk.gui;

import com.winrisk.game.map.Map;
import org.junit.Test;

import java.awt.HeadlessException;
import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class StartGameTest {
    @Test
    public void testCreateEmptyMap() throws Exception {
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
            // Ignore if environment does not support GUI
        }
    }
}
