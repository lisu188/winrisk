package com.winrisk.game.map;

import com.winrisk.game.object.Field;
import com.winrisk.game.rules.CardSymbol;

final class WorldTerritoryMetadata {
    private static final String[] WORLD_TERRITORIES = {
            "Alaska",
            "Northwest Territory",
            "Greenland",
            "Alberta",
            "Ontario",
            "Quebec",
            "Western United States",
            "Eastern United States",
            "Central America",
            "Venezuela",
            "Peru",
            "Brazil",
            "Argentina",
            "Iceland",
            "Scandinavia",
            "Ukraine",
            "Great Britain",
            "Northern Europe",
            "Western Europe",
            "Southern Europe",
            "North Africa",
            "Egypt",
            "East Africa",
            "Congo",
            "South Africa",
            "Madagascar",
            "Ural",
            "Siberia",
            "Yakutsk",
            "Kamchatka",
            "Irkutsk",
            "Mongolia",
            "Japan",
            "Afghanistan",
            "China",
            "Middle East",
            "India",
            "Siam",
            "Indonesia",
            "New Guinea",
            "Western Australia",
            "Eastern Australia"
    };

    private WorldTerritoryMetadata() {
    }

    static void apply(Map map) {
        for (int i = 0; i < map.getFields().size(); i++) {
            Field field = map.getFields().get(i);
            field.setFieldIndex(i);
            field.setDisplayName(displayName(i, map.getFields().size()));
            field.setCardSymbol(symbol(i));
        }
    }

    private static String displayName(int index, int fieldCount) {
        if (fieldCount == WORLD_TERRITORIES.length) {
            return WORLD_TERRITORIES[index];
        }
        return "Territory " + (index + 1);
    }

    private static CardSymbol symbol(int index) {
        CardSymbol[] symbols = {
                CardSymbol.INFANTRY,
                CardSymbol.CAVALRY,
                CardSymbol.ARTILLERY
        };
        return symbols[index % symbols.length];
    }
}
