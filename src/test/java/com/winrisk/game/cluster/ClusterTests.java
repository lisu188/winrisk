package com.winrisk.game.cluster;

import com.winrisk.game.ai.PlayerFactory;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.util.GameColor;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClusterTests {
    @Test
    public void fieldListDeleteAndGetHuman() {
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

        Player human = new Player(GameColor.RED, PlayerFactory.getHuman());
        Player ai = new Player(GameColor.BLUE, PlayerFactory.getRandomAI());
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

    @Test
    public void patchListHas() {
        PatchList patches = new PatchList();
        FieldList list = new FieldList();
        Field f = new Field(new PointF(0,0));
        list.add(f);
        patches.add(list);
        assertTrue(patches.has(f));
        assertFalse(patches.has(new Field(new PointF(1,1))));
    }
}
