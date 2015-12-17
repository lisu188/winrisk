package com.winrisk.game.serialization;

public interface Serializer {
    void load(Saveable saveable, String path);

    void save(Saveable saveable, String path);

}
