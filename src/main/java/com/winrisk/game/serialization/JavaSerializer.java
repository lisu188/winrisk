package com.winrisk.game.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class JavaSerializer implements Serializer {
    private final Gson gson = new Gson();

    @Override
    public void load(Saveable map, String path) {
        requireSaveable(map);
        File input = requirePath(path);
        try (InputStream fileStream = new FileInputStream(input)) {
            byte[] raw = readAll(fileStream);
            byte[] data = isGzip(raw) ? decompress(raw) : raw;

            Sketch sketch = tryJsonDeserialize(map, data);
            if (sketch == null) {
                sketch = tryJavaDeserialize(data);
            }

            if (sketch == null) {
                throw new IllegalArgumentException(
                        "Unsupported serialized data: " + input.getAbsolutePath());
            }
            applySketch(map, sketch, input);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load serialized data from " + input.getAbsolutePath(), e);
        }
    }

    @Override
    public void save(Saveable map, String path) {
        requireSaveable(map);
        File output = requirePath(path);
        try (FileOutputStream mapFile = new FileOutputStream(output)) {
            String json = gson.toJson(map.toSketch(), map.getSketchClass());
            mapFile.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not save serialized data to " + output.getAbsolutePath(), e);
        }
    }

    private void requireSaveable(Saveable saveable) {
        if (saveable == null) {
            throw new IllegalArgumentException("Saveable cannot be null");
        }
    }

    private File requirePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or blank");
        }
        return new File(path);
    }

    private void applySketch(Saveable saveable, Sketch sketch, File source) {
        try {
            saveable.fromSketch(sketch);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Invalid serialized data: " + source.getAbsolutePath(), e);
        }
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }

    private boolean isGzip(byte[] data) {
        return data.length > 2 && (data[0] == (byte) 0x1f) && (data[1] == (byte) 0x8b);
    }

    private byte[] decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return readAll(gzip);
        }
    }

    private Sketch tryJsonDeserialize(Saveable map, byte[] data) {
        try {
            return gson.fromJson(new String(data, StandardCharsets.UTF_8), map.getSketchClass());
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private Sketch tryJavaDeserialize(byte[] data) {
        try (ObjectInputStream mapObj = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object sketch = mapObj.readObject();
            if (sketch instanceof Sketch) {
                return (Sketch) sketch;
            }
        } catch (IOException | ClassNotFoundException e) {
            // ignored, will return null to signal failure
        }
        return null;
    }
}
