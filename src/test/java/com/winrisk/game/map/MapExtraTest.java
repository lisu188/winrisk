package com.winrisk.game.map;

import com.winrisk.game.serialization.MapSketch;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

public class MapExtraTest {
    @Test
    public void testSketchEquality() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addField(new PointF(1,1));
        map.addContinent(1);
        MapSketch sketch = new MapSketch(map);
        Map map2 = new Map();
        sketch.toMap(map2);
        MapSketch sketch2 = new MapSketch(map2);
        assertEquals(sketch, sketch2);
        assertEquals(sketch.hashCode(), sketch2.hashCode());
    }
}
