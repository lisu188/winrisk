package com.winrisk.game.object;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class FieldOperationsTest {
    @Test
    public void testMoveAndRein() {
        Player p = new Player(Color.RED, PlayerFactory.getHuman());
        Field from = new Field(new PointF(0,0));
        Field to = new Field(new PointF(1,1));
        from.setPlayer(p);
        to.setPlayer(p);
        from.addNext(to);
        from.setArmy(1);
        p.setRein(3);
        from.rein(3);
        assertEquals(4, from.getArmy());
        assertEquals(0, p.getCurrentReinforcements());
        assertTrue(from.move(to,2));
        assertEquals(2, from.getArmy());
        assertEquals(2, to.getArmy());
        assertTrue(from.move(to,5));
        assertEquals(1, from.getArmy());
        assertEquals(3, to.getArmy());
    }
}
