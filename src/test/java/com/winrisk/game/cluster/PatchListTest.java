package com.winrisk.game.cluster;

import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

public class PatchListTest {
    @Test
    public void testHas() {
        PatchList patches = new PatchList();
        FieldList list = new FieldList();
        Field f = new Field(new PointF(0,0));
        list.add(f);
        patches.add(list);
        assertTrue(patches.has(f));
        assertFalse(patches.has(new Field(new PointF(1,1))));
    }
}
