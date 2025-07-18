package com.winrisk.game.cluster;

import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class FieldListTest {
    @Test
    public void testDeleteAndGetHuman() {
        FieldList list = new FieldList();
        Field a = new Field(new PointF(0, 0));
        Field b = new Field(new PointF(1, 1));
        a.addNext(b);
        b.addNext(a);
        Continent continent = new Continent(0, 1);
        continent.addField(a);
        continent.addField(b);
        list.add(a);
        list.add(b);

        // set players
        Player human = new Player(Color.RED, PlayerFactory.getHuman());
        Player ai = new Player(Color.BLUE, PlayerFactory.getRandomAI());
        a.setPlayer(human);
        b.setPlayer(ai);

        FieldList humanFields = list.getHuman();
        assertEquals(1, humanFields.size());
        assertTrue(humanFields.contains(a));

        list.delete(a);
        assertFalse(list.contains(a));
        assertFalse(b.getNext().contains(a));
        assertFalse(continent.getFields().contains(a));
    }
}
