package com.winrisk.game.map;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
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
        try {
            URL resource = Map.class.getResource("world.map");
            if (resource == null) {
                throw new IllegalStateException("Default world map resource is unavailable");
            }
            return new Map(new File(resource.toURI()).getAbsolutePath());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Default world map resource is unavailable", e);
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
