package com.winrisk.game.data;

import com.winrisk.game.map.Map;
import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;

public class ParamsTest {
    @Test
    public void testParams() throws Exception {
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
