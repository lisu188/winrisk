package com.winrisk.game.serialization;

import java.io.InputStream;

public interface Serializer {
    void load(Saveable saveable, String path);

    /**
     * Loads serialized data from a stream. Unlike the path overload this works
     * for classpath resources inside a jar. {@code sourceName} is used only in
     * error messages.
     */
    void load(Saveable saveable, InputStream input, String sourceName);

    void save(Saveable saveable, String path);

}
