package com.winrisk.game.object;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ContinentTest {
    @Test
    public void testBasics() {
        Continent continent = new Continent(0, 2);
        Field a = new Field(new PointF(0,0));
        Field b = new Field(new PointF(1,1));
        Field c = new Field(new PointF(2,2));
        a.addNext(b);
        b.addNext(c);
        continent.addField(a);
        continent.addField(b);
        continent.addField(c);
        assertTrue(continent.getFields().contains(a));
        assertEquals(continent, a.getContinent());

        continent.incBonus();
        continent.decBonus();
        assertEquals(2, continent.getBonus());

        // border detection: connect b to external field
        Continent other = new Continent(1,1);
        Field outside = new Field(new PointF(3,3));
        outside.addNext(b);
        other.addField(outside);
        assertEquals(1, continent.getBorderCount());

        // player share and getPlayer
        Player p1 = new Player(Color.RED, PlayerFactory.getHuman());
        Player p2 = new Player(Color.BLUE, PlayerFactory.getRandomAI());
        a.setPlayer(p1);
        b.setPlayer(p1);
        c.setPlayer(p2);
        assertNull(continent.getPlayer());
        c.setPlayer(p1);
        assertEquals(p1, continent.getPlayer());
        c.setPlayer(p2);
        assertNull(continent.getPlayer());
        assertEquals(2f/3f, continent.getPlayerShare(p1), 0.001f);

        continent.removeField(a);
        assertFalse(continent.getFields().contains(a));
        assertNull(a.getContinent());
    }
}
