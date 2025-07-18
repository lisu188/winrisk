package com.winrisk.game.data;

import com.winrisk.game.map.Map;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class DataTests {
    @Test
    public void gamePhaseCycle() {
        GamePhase phase = GamePhase.ATTACK;
        assertEquals(GamePhase.MOVE, phase.getNextState());
        phase = phase.getNextState();
        assertEquals(GamePhase.REINFORCE, phase.getNextState());
        assertEquals(GamePhase.REINFORCE, GamePhase.UNDEFINED.getNextState());
    }

    @Test
    public void motionEventData() {
        long t = System.currentTimeMillis();
        MotionEvent e = new MotionEvent(MotionEvent.ACTION_DOWN, t, t+1, 2, 3);
        assertEquals(MotionEvent.ACTION_DOWN, e.getAction());
        assertEquals(t, e.getDownTime());
        assertEquals(t+1, e.getEventTime());
        assertEquals(2, e.getX());
        assertEquals(3, e.getY());
    }

    @Test
    public void paramsRoundTrip() throws Exception {
        Params p = new Params();
        p.setAiPlayers(2);
        p.setHumanPlayers(1);
        p.setAttackWithAll(true);
        p.setFogOfWar(true);
        p.setSkynetMode(true);
        String path = new File(Map.class.getResource("world.map").toURI()).getAbsolutePath();
        p.setMap(path);
        assertEquals(2, p.getAiPlayers());
        assertEquals(1, p.getHumanPlayers());
        assertTrue(p.isAttackWithAll());
        assertTrue(p.isFogOfWar());
        assertTrue(p.isSkynetMode());
        Map map = p.loadMap();
        assertNotNull(map);
        assertFalse(map.getFields().isEmpty());
    }
}
