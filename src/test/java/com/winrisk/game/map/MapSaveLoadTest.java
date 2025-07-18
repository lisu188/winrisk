package com.winrisk.game.map;

import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.io.File;

public class MapSaveLoadTest {
    @Test
    public void testSaveLoad() throws Exception {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addContinent(1);
        File tmp = File.createTempFile("map",".dat");
        map.save(tmp.getAbsolutePath());
        Map loaded = new Map(tmp.getAbsolutePath());
        loaded.save(tmp.getAbsolutePath());
        tmp.delete();
    }
}
