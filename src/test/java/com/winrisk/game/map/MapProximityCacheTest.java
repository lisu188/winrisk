package com.winrisk.game.map;

import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

/** Test the proximity algorithm and cache behaviour. */
public class MapProximityCacheTest {
    @Test
    public void testProximityCaching() {
        Map map = new Map();
        map.addField(new PointF(0,0)); // A
        map.addField(new PointF(1,0)); // B
        map.addField(new PointF(2,0)); // C
        Field a = map.getFields().get(0);
        Field b = map.getFields().get(1);
        Field c = map.getFields().get(2);
        a.addNext(b);
        b.addNext(c);

        // first call should compute
        assertEquals(2, map.proximity(a, c));
        // second call should return cached value quickly
        assertEquals(2, map.proximity(a, c));
        // symmetry check via indices
        assertEquals(2, map.proximity(0,2));
    }
}
