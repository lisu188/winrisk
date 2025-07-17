package com.winrisk.game.serialization;

import com.winrisk.game.TestUtil;
import com.winrisk.game.map.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class MapSketchTest {
    @Test
    public void testRoundTrip() throws Exception {
        Map original = TestUtil.createNewGame().getMap();
        MapSketch sketch = new MapSketch(original);
        Map recreated = new Map();
        sketch.toMap(recreated);
        assertEquals(original.getFields().size(), recreated.getFields().size());
        assertEquals(original.getContinents().size(), recreated.getContinents().size());
    }
}
