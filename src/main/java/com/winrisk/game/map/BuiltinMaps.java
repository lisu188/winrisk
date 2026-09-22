package com.winrisk.game.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Registry of the boards that ship with the game: the classic world map plus
 * the historical supercontinent boards from {@link HistoricalMaps}. Maps are
 * addressed by a case-insensitive name, usable from the host window and the
 * headless {@code --map=} option.
 */
public final class BuiltinMaps {

    public static final String WORLD = "world";
    public static final String PANGAEA = "pangaea";
    public static final String LAURASIA = "laurasia";
    public static final String GONDWANA = "gondwana";
    public static final String RODINIA = "rodinia";

    private BuiltinMaps() {
    }

    public static List<String> names() {
        return List.of(WORLD, PANGAEA, LAURASIA, GONDWANA, RODINIA);
    }

    public static boolean isBuiltin(String name) {
        return names().contains(normalize(name));
    }

    public static Map byName(String name) {
        switch (normalize(name)) {
            case WORLD:
                return world();
            case PANGAEA:
                return HistoricalMaps.pangaea();
            case LAURASIA:
                return HistoricalMaps.laurasia();
            case GONDWANA:
                return HistoricalMaps.gondwana();
            case RODINIA:
                return HistoricalMaps.rodinia();
            default:
                throw new IllegalArgumentException("Unknown built-in map: " + name);
        }
    }

    private static Map world() {
        // Stream-based so the resource loads from inside a jar as well as from
        // an exploded class directory.
        try (InputStream input = Map.class.getResourceAsStream("world.map")) {
            if (input == null) {
                throw new IllegalStateException("Default world map resource is unavailable");
            }
            Map map = new Map();
            map.load(input, "classpath:com/winrisk/game/map/world.map");
            return map;
        } catch (IOException e) {
            throw new IllegalStateException("Default world map resource is unavailable", e);
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
