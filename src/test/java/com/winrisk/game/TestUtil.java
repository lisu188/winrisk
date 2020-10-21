package com.winrisk.game;


import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.serialization.MapSketch;
import com.winrisk.game.view.Game;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class TestUtil {

    public static Game createNewGame() throws Exception {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(8);

        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());

        return new Game(params);
    }

    public static Game finishGame(Params params) {
        return finishGame(new Game(params));
    }

    public static Game finishGame(Game game) {
        while (!game.end()) {
            game.next();
        }
        return game;
    }

    public static <T> T getField(Object object, String field, Class<T> fieldClass) throws NoSuchFieldException, IllegalAccessException {
        Field proxCache = object.getClass().getDeclaredField("proxCache");
        proxCache.setAccessible(true);
        return (T) proxCache.get(object);
    }

    public static String serialize(Object object) throws Exception {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(byteOutputStream);
        stream.writeObject(object);
        return Base64.getEncoder().encodeToString(compress(byteOutputStream.toByteArray()));
    }

    public static <T> T deserialize(String data, Class<T> clas) throws Exception {
        byte[] decompress = decompress(Base64.getDecoder().decode(data));
        ByteArrayInputStream byteOutputStream = new ByteArrayInputStream(decompress, 0, decompress.length);
        ObjectInputStream stream = new ObjectInputStream(byteOutputStream);
        return (T) stream.readObject();
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        deflater.end();
        return output;
    }

    private static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        inflater.end();

        return output;
    }

    @Test
    public void serialization() throws Exception {
        MapSketch game = createNewGame().getMapSketch();
        String serializedGame = serialize(game);
        MapSketch newGame = deserialize(serializedGame, MapSketch.class);
        assert (game.equals(newGame));
    }

}

