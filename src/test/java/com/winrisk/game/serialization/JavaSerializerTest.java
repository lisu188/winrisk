package com.winrisk.game.serialization;

import com.winrisk.game.TestUtil;
import com.winrisk.game.map.Map;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class JavaSerializerTest {
    @Test
    public void testRoundTrip() throws Exception {
        Map map = TestUtil.createNewGame().getMap();
        JavaSerializer ser = new JavaSerializer();
        File tmp = File.createTempFile("map",".dat");
        ser.save(map, tmp.getAbsolutePath());
        Map loaded = new Map();
        ser.load(loaded, tmp.getAbsolutePath());
        assertEquals(map.getFields().size(), loaded.getFields().size());
        tmp.delete();
    }
}
