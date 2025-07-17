package com.winrisk.game.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class MotionEventTest {
    @Test
    public void testEvent() {
        long t = System.currentTimeMillis();
        MotionEvent e = new MotionEvent(MotionEvent.ACTION_DOWN, t, t+1, 2, 3);
        assertEquals(MotionEvent.ACTION_DOWN, e.getAction());
        assertEquals(t, e.getDownTime());
        assertEquals(t+1, e.getEventTime());
        assertEquals(2, e.getX());
        assertEquals(3, e.getY());
    }
}
