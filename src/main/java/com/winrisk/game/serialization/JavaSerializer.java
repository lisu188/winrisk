package com.winrisk.game.serialization;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JavaSerializer implements Serializer {
    @Override
    public void load(Saveable map, String path) {
        try {
            MapSketch mapSketch;
            File mapPath = new File(path);
            FileInputStream mapFile = new FileInputStream(mapPath);
            GZIPInputStream gzip = new GZIPInputStream(mapFile);
            ObjectInputStream mapObj = new ObjectInputStream(gzip);
            mapSketch = (MapSketch) mapObj.readObject();
            map.fromSketch(mapSketch);
            mapObj.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Saveable map, String path) {
        try {
            FileOutputStream mapFile;
            File mapPath = new File(path);
            mapFile = new FileOutputStream(mapPath);
            GZIPOutputStream gzip = new GZIPOutputStream(mapFile);
            ObjectOutputStream mapObj = new ObjectOutputStream(gzip);
            mapObj.writeObject(map.toSketch());
            mapObj.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
