package com.winrisk.game.util;

import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.*;

public class ColorsTest {
    @Test
    public void testGet() {
        assertEquals(Color.RED, Colors.get(0));
    }

    @Test
    public void testRandomColor() {
        assertNotNull(Colors.getRandomColor());
    }
}
