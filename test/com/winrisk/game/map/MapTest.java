package com.winrisk.game.map;

import com.winrisk.game.TestUtil;
import org.junit.Test;

import java.util.Arrays;

public class MapTest {
    private static final String CACHE = "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";

    @Test
    public void proximityTest() throws Exception {
        Map map = TestUtil.createNewGame().getMap();
        int size = map.getFields().size();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.proximity(i, j);
            }
        }
        assert (Arrays.deepEquals(TestUtil.deserialize(CACHE, int[][].class), TestUtil.getField(map, "proxCache", int[][].class)));
    }
}
