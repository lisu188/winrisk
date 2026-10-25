package com.winrisk.game.util;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class UtilTests {
    @Test
    public void arraysAvgFloat() {
        assertEquals(2.0f, Arrays.avg(new float[]{1f,2f,3f}), 0.0001f);
    }

    @Test
    public void arraysAvgFloatDecimals() {
        assertEquals(1.5f, Arrays.avg(new float[]{1.2f, 1.8f}), 0.0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void arraysAvgFloatEmpty() {
        Arrays.avg(new float[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void arraysAvgFloatNull() {
        Arrays.avg((float[]) null);
    }

    @Test
    public void arraysAvgInteger() {
        assertEquals(2.0f, Arrays.avg(new Integer[]{1,2,3}), 0.0001f);
    }

    @Test
    public void arraysAvgIntegerKeepsLargeSumsAndCancellation() {
        assertEquals((float) Integer.MAX_VALUE,
                Arrays.avg(new Integer[]{Integer.MAX_VALUE, Integer.MAX_VALUE}), 0f);
        assertEquals((float) Integer.MIN_VALUE,
                Arrays.avg(new Integer[]{Integer.MIN_VALUE, Integer.MIN_VALUE}), 0f);
        assertEquals(-0.5f, Arrays.avg(new Integer[]{Integer.MAX_VALUE, Integer.MIN_VALUE}), 0f);
        assertEquals(16777217f, Arrays.avg(new Integer[]{16777217, 16777217}), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void arraysAvgIntegerEmpty() {
        Arrays.avg(new Integer[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void arraysAvgIntegerNull() {
        Arrays.avg((Integer[]) null);
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

    }

    @Test
    public void pointsDistanceWidensBeforeSubtractingExtremeCoordinates() {
        PointF low = new PointF(Integer.MIN_VALUE, Integer.MIN_VALUE);
        PointF high = new PointF(Integer.MAX_VALUE, Integer.MAX_VALUE);
        float expected = (float) Math.hypot(4294967295d, 4294967295d);
        assertEquals(expected, PointF.dist(low, high), 0f);
        assertEquals(expected, PointF.dist(high, low), 0f);
        assertEquals(0f, PointF.dist(low, low), 0f);
    }
}
