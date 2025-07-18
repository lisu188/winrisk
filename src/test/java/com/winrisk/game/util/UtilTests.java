package com.winrisk.game.util;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class UtilTests {
    @Test
    public void arraysAvgFloat() {
        assertEquals(2.0f, Arrays.avg(new float[]{1f,2f,3f}), 0.0001f);
    }

    @Test
    public void arraysAvgInteger() {
        assertEquals(2.0f, Arrays.avg(new Integer[]{1,2,3}), 0.0001f);
    }

    @Test
    public void arraysIndices() {
        float[] arr = {1f,5f,3f};
        assertEquals(1, Arrays.indMax(arr));
        assertEquals(0, Arrays.indMin(arr));
    }

    @Test
    public void arraysMinMax() {
        int[] arr = {2,5,1};
        assertEquals(5, Arrays.max(arr));
        assertEquals(1, Arrays.min(arr));
    }

    @Test
    public void colorsGetRandom() {
        assertEquals(Color.RED, Colors.get(0));
        assertNotNull(Colors.getRandomColor());
    }

    @Test
    public void pointsDistAndSearch() {
        PointF a = new PointF(0,0);
        PointF b = new PointF(3,4);
        assertEquals(5.0f, PointF.dist(a,b), 0.001f);

        Field f1 = new Field(new PointF(0,0));
        Field f2 = new Field(new PointF(30,30));
        FieldList list = new FieldList();
        list.add(f1);
        list.add(f2);
        assertEquals(f1, Points.search(list, new PointF(2,2)));
        assertNull(Points.search(list, new PointF(100,100)));
    }
}
