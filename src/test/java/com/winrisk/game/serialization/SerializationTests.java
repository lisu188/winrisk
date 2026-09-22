package com.winrisk.game.serialization;

import com.winrisk.game.TestUtil;
import com.winrisk.game.map.Map;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SerializationTests {
    @Test
    public void mapSketchRoundTrip() throws Exception {
        Map original = TestUtil.createNewGame().getMap();
        MapSketch sketch = new MapSketch(original);
        Map recreated = new Map();
        sketch.toMap(recreated);
        assertEquals(original.getFields().size(), recreated.getFields().size());
        assertEquals(original.getContinents().size(), recreated.getContinents().size());
    }

    @Test
    public void javaSerializerRoundTrip() throws Exception {
        Map map = TestUtil.createNewGame().getMap();
        JavaSerializer ser = new JavaSerializer();
        File tmp = File.createTempFile("map",".dat");
        ser.save(map, tmp.getAbsolutePath());
        Map loaded = new Map();
        ser.load(loaded, tmp.getAbsolutePath());
        assertEquals(map.getFields().size(), loaded.getFields().size());
        assertTrue(readJson(tmp).trim().startsWith("{"));
        tmp.delete();
    }

    @Test
    public void javaSerializerLoadsFromStream() throws Exception {
        Map map = TestUtil.createNewGame().getMap();
        JavaSerializer serializer = new JavaSerializer();
        File tmp = File.createTempFile("map", ".dat");
        serializer.save(map, tmp.getAbsolutePath());

        Map loaded = new Map();
        try (java.io.InputStream input = new java.io.FileInputStream(tmp)) {
            serializer.load(loaded, input, "stream-test");
        }
        assertEquals(map.getFields().size(), loaded.getFields().size());
        assertEquals(map.getContinents().size(), loaded.getContinents().size());
        tmp.delete();
    }

    @Test
    public void worldMapLoadsFromClasspathStream() throws Exception {
        // The path every packaged (jar) deployment takes.
        Map map = com.winrisk.game.map.BuiltinMaps.byName("world");
        assertEquals(42, map.getFields().size());
    }

    @Test
    public void javaSerializerRejectsBlankPaths() throws Exception {
        JavaSerializer serializer = new JavaSerializer();
        Map map = new Map();

        assertThrowsException(IllegalArgumentException.class,
                () -> serializer.load(map, null));
        assertThrowsException(IllegalArgumentException.class,
                () -> serializer.save(map, " "));
    }

    @Test
    public void javaSerializerRejectsUnsupportedData() throws Exception {
        JavaSerializer serializer = new JavaSerializer();
        File tmp = File.createTempFile("invalid-map", ".dat");
        Files.writeString(tmp.toPath(), "not a serialized map", StandardCharsets.UTF_8);

        assertThrowsException(IllegalArgumentException.class,
                () -> serializer.load(new Map(), tmp.getAbsolutePath()));
        tmp.delete();
    }

    @Test
    public void javaSerializerRejectsIncompleteJsonMap() throws Exception {
        JavaSerializer serializer = new JavaSerializer();
        File tmp = File.createTempFile("incomplete-map", ".dat");
        Files.writeString(tmp.toPath(), "{}", StandardCharsets.UTF_8);

        assertThrowsException(IllegalArgumentException.class,
                () -> serializer.load(new Map(), tmp.getAbsolutePath()));
        tmp.delete();
    }

    private String readJson(File file) throws Exception {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private <T extends Exception> T assertThrowsException(
            Class<T> expected,
            ThrowingRunnable runnable) throws Exception {
        try {
            runnable.run();
        } catch (Exception e) {
            if (expected.isInstance(e)) {
                return expected.cast(e);
            }
            throw e;
        }
        fail("Expected " + expected.getSimpleName());
        return null;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
