package com.winrisk.game.util;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import org.junit.Test;

import static org.junit.Assert.*;

public class PointsTest {
    @Test
    public void testDistAndSearch() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(3, 4);
        assertEquals(5.0f, PointF.dist(a, b), 0.001f);

        Field f1 = new Field(new PointF(0, 0));
        Field f2 = new Field(new PointF(30, 30));
        FieldList list = new FieldList();
        list.add(f1);
        list.add(f2);

        assertEquals(f1, Points.search(list, new PointF(2, 2)));
        assertNull(Points.search(list, new PointF(100, 100)));
    }
}
