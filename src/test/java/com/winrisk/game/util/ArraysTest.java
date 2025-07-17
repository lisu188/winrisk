package com.winrisk.game.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArraysTest {
    @Test
    public void testAvgFloat() {
        assertEquals(2.0f, Arrays.avg(new float[]{1f, 2f, 3f}), 0.0001f);
    }

    @Test
    public void testAvgInteger() {
        assertEquals(2.0f, Arrays.avg(new Integer[]{1, 2, 3}), 0.0001f);
    }

    @Test
    public void testIndices() {
        float[] arr = {1f, 5f, 3f};
        assertEquals(1, Arrays.indMax(arr));
        assertEquals(0, Arrays.indMin(arr));
    }

    @Test
    public void testMinMax() {
        int[] arr = {2, 5, 1};
        assertEquals(5, Arrays.max(arr));
        assertEquals(1, Arrays.min(arr));
    }
}
