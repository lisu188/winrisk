package com.winrisk.game.map;

import com.winrisk.game.TestUtil;
import com.winrisk.game.data.Params;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import com.winrisk.game.serialization.MapSketch;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.*;

public class MapTests {
    @Test
    public void mapSketchEquality() {
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

    @Test
    public void mapProximityCaching() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addField(new PointF(1,0));
        map.addField(new PointF(2,0));
        Field a = map.getFields().get(0);
        Field b = map.getFields().get(1);
        Field c = map.getFields().get(2);
        a.addNext(b);
        b.addNext(c);
        assertEquals(2, map.proximity(a, c));
        assertEquals(2, map.proximity(a, c));
        assertEquals(2, map.proximity(0,2));
    }

    @Test
    public void mapSaveLoad() throws Exception {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addContinent(1);
        File tmp = File.createTempFile("map",".dat");
        map.save(tmp.getAbsolutePath());
        Map loaded = new Map(tmp.getAbsolutePath());
        loaded.save(tmp.getAbsolutePath());
        tmp.delete();
    }

    @Test
    public void randomMapGenerationCreatesConnectedGraph() {
        Params params = new Params();
        params.setRandomMap(true);
        params.setRandomFields(12);
        params.setRandomContinents(3);
        params.setRandomSeed(42L);

        Map map = params.loadMap();

        assertEquals(12, map.getFields().size());
        assertEquals(3, map.getContinents().size());
        assertTrue(isConnected(map));
        map.getFields().forEach(field -> assertNotNull(field.getContinent()));
    }

    private boolean isConnected(Map map) {
        if (map.getFields().isEmpty()) {
            return true;
        }
        Set<Field> visited = new HashSet<>();
        Queue<Field> queue = new LinkedList<>();
        queue.add(map.getFields().get(0));
        while (!queue.isEmpty()) {
            Field current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            queue.addAll(current.getNext());
        }
        return visited.size() == map.getFields().size();
    }

    private static final String CACHE = "" +
"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"+
"H5+Pyfn/8ALHTRrQ==";

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
