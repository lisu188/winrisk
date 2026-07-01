package com.winrisk.game.map;

import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.util.PointF;

import java.util.LinkedHashMap;

/**
 * Built-in boards based on Earth's historical supercontinents. Each board is
 * authored in code: territories are named after real cratons, shields and
 * terranes, adjacencies follow the reconstructed geography, and bonus regions
 * play the role of Risk continents.
 */
public final class HistoricalMaps {

    private HistoricalMaps() {
    }

    /**
     * Pangaea (~250 Ma): the single C-shaped supercontinent wrapped around the
     * Tethys Sea, assembled from Laurussia in the north and Gondwana in the
     * south with the Cimmerian terranes rifting across the Tethys.
     */
    public static Map pangaea() {
        return new MapBuilder()
                // Laurentia
                .territory("Laurentia", 120, 90)
                .territory("Greenland Shield", 230, 60)
                .territory("Superior Craton", 200, 140)
                .territory("Wyoming Craton", 90, 160)
                .territory("Ouachita", 150, 230)
                .territory("Appalachia", 270, 160)
                // Baltica & Variscides
                .territory("Fennoscandia", 340, 60)
                .territory("Baltica", 420, 90)
                .territory("Avalonia", 350, 140)
                .territory("Armorica", 420, 170)
                .territory("Iberia", 360, 230)
                // Siberia & East Asia
                .territory("Siberia", 560, 60)
                .territory("Kazakhstania", 620, 120)
                .territory("Tarim", 680, 170)
                .territory("North China", 740, 110)
                .territory("South China", 740, 210)
                // Cimmeria
                .territory("Anatolia", 520, 260)
                .territory("Iran", 590, 280)
                .territory("Tibet", 660, 300)
                .territory("Indochina", 730, 300)
                // West Gondwana
                .territory("Amazonia", 170, 330)
                .territory("Rio de la Plata", 120, 420)
                .territory("Patagonia", 180, 500)
                .territory("West Africa", 280, 310)
                .territory("Congo Craton", 330, 390)
                .territory("Kalahari", 300, 480)
                // East Gondwana
                .territory("Arabia", 450, 330)
                .territory("India", 540, 380)
                .territory("Madagascar", 430, 460)
                .territory("Antarctica", 520, 520)
                .territory("Australia", 630, 470)
                .territory("Zealandia", 720, 520)

                .connect("Laurentia", "Greenland Shield")
                .connect("Laurentia", "Superior Craton")
                .connect("Laurentia", "Wyoming Craton")
                .connect("Wyoming Craton", "Ouachita")
                .connect("Superior Craton", "Appalachia")
                .connect("Ouachita", "Appalachia")
                .connect("Greenland Shield", "Superior Craton")
                .connect("Fennoscandia", "Baltica")
                .connect("Baltica", "Avalonia")
                .connect("Avalonia", "Armorica")
                .connect("Armorica", "Iberia")
                .connect("Avalonia", "Iberia")
                .connect("Siberia", "Kazakhstania")
                .connect("Kazakhstania", "Tarim")
                .connect("Tarim", "North China")
                .connect("North China", "South China")
                .connect("Tarim", "South China")
                .connect("Anatolia", "Iran")
                .connect("Iran", "Tibet")
                .connect("Tibet", "Indochina")
                .connect("Amazonia", "Rio de la Plata")
                .connect("Rio de la Plata", "Patagonia")
                .connect("Amazonia", "West Africa")
                .connect("West Africa", "Congo Craton")
                .connect("Congo Craton", "Kalahari")
                .connect("Patagonia", "Kalahari")
                .connect("Arabia", "India")
                .connect("India", "Madagascar")
                .connect("Madagascar", "Antarctica")
                .connect("Antarctica", "Australia")
                .connect("Australia", "Zealandia")
                .connect("India", "Antarctica")
                // sutures and margins between the regions
                .connect("Greenland Shield", "Fennoscandia")
                .connect("Appalachia", "Avalonia")
                .connect("Appalachia", "West Africa")
                .connect("Ouachita", "Amazonia")
                .connect("Iberia", "West Africa")
                .connect("Baltica", "Kazakhstania")
                .connect("Siberia", "Baltica")
                .connect("Kazakhstania", "Iran")
                .connect("Tarim", "Tibet")
                .connect("South China", "Indochina")
                .connect("Anatolia", "Arabia")
                .connect("Iran", "Arabia")
                .connect("Tibet", "India")
                .connect("Congo Craton", "Arabia")
                .connect("Kalahari", "Madagascar")
                .connect("Kalahari", "Antarctica")

                .region(4, "Laurentia", "Greenland Shield", "Superior Craton",
                        "Wyoming Craton", "Ouachita", "Appalachia")
                .region(3, "Fennoscandia", "Baltica", "Avalonia", "Armorica", "Iberia")
                .region(3, "Siberia", "Kazakhstania", "Tarim", "North China", "South China")
                .region(2, "Anatolia", "Iran", "Tibet", "Indochina")
                .region(4, "Amazonia", "Rio de la Plata", "Patagonia",
                        "West Africa", "Congo Craton", "Kalahari")
                .region(4, "Arabia", "India", "Madagascar", "Antarctica",
                        "Australia", "Zealandia")
                .build();
    }

    /**
     * Laurasia (~150 Ma): the northern supercontinent left after Pangaea broke
     * apart, stretching from Laurentia over Baltica and Siberia to East Asia.
     */
    public static Map laurasia() {
        return new MapBuilder()
                // Western Laurentia
                .territory("Alaska", 80, 120)
                .territory("Cordillera", 140, 190)
                .territory("Laurentia", 230, 160)
                .territory("Sonora", 150, 280)
                .territory("Wyoming Craton", 230, 250)
                // Eastern Laurentia
                .territory("Superior Craton", 310, 200)
                .territory("Labrador", 370, 130)
                .territory("Greenland", 440, 70)
                .territory("Appalachia", 350, 290)
                // Baltica
                .territory("Fennoscandia", 520, 110)
                .territory("Baltica", 560, 180)
                .territory("Avalonia", 480, 240)
                .territory("Bohemia", 560, 280)
                .territory("Iberia", 480, 330)
                // Siberia
                .territory("Taimyr", 660, 80)
                .territory("West Siberia", 630, 150)
                .territory("Angara", 700, 160)
                .territory("Kolyma", 750, 90)
                // East Asia
                .territory("Kazakhstania", 630, 250)
                .territory("Tarim", 690, 300)
                .territory("Mongolia", 740, 230)
                .territory("North China", 740, 330)
                .territory("South China", 700, 400)
                .territory("Indochina", 620, 430)

                .connect("Alaska", "Cordillera")
                .connect("Cordillera", "Laurentia")
                .connect("Cordillera", "Sonora")
                .connect("Sonora", "Wyoming Craton")
                .connect("Laurentia", "Wyoming Craton")
                .connect("Superior Craton", "Labrador")
                .connect("Labrador", "Greenland")
                .connect("Superior Craton", "Appalachia")
                .connect("Labrador", "Appalachia")
                .connect("Fennoscandia", "Baltica")
                .connect("Baltica", "Avalonia")
                .connect("Baltica", "Bohemia")
                .connect("Avalonia", "Iberia")
                .connect("Bohemia", "Iberia")
                .connect("Taimyr", "West Siberia")
                .connect("West Siberia", "Angara")
                .connect("Angara", "Kolyma")
                .connect("Taimyr", "Angara")
                .connect("Kazakhstania", "Tarim")
                .connect("Tarim", "Mongolia")
                .connect("Mongolia", "North China")
                .connect("Tarim", "North China")
                .connect("North China", "South China")
                .connect("South China", "Indochina")
                // sutures and margins between the regions
                .connect("Laurentia", "Superior Craton")
                .connect("Wyoming Craton", "Superior Craton")
                .connect("Alaska", "Kolyma")
                .connect("Greenland", "Fennoscandia")
                .connect("Appalachia", "Avalonia")
                .connect("Baltica", "West Siberia")
                .connect("Bohemia", "Kazakhstania")
                .connect("Kazakhstania", "West Siberia")
                .connect("Mongolia", "Angara")

                .region(3, "Alaska", "Cordillera", "Laurentia", "Sonora", "Wyoming Craton")
                .region(2, "Superior Craton", "Labrador", "Greenland", "Appalachia")
                .region(3, "Fennoscandia", "Baltica", "Avalonia", "Bohemia", "Iberia")
                .region(2, "Taimyr", "West Siberia", "Angara", "Kolyma")
                .region(4, "Kazakhstania", "Tarim", "Mongolia", "North China",
                        "South China", "Indochina")
                .build();
    }

    /**
     * Gondwana (~420 Ma): the southern supercontinent of South America, Africa,
     * Arabia, India, Antarctica and Australia.
     */
    public static Map gondwana() {
        return new MapBuilder()
                // South America
                .territory("Amazonia", 120, 140)
                .territory("Sao Francisco", 210, 190)
                .territory("Pampia", 110, 240)
                .territory("Rio de la Plata", 180, 290)
                .territory("Patagonia", 140, 380)
                // Africa
                .territory("West Africa", 330, 120)
                .territory("Sahara Metacraton", 430, 110)
                .territory("Congo Craton", 380, 210)
                .territory("Tanzania Craton", 470, 250)
                .territory("Kalahari", 400, 320)
                .territory("Kaapvaal", 330, 350)
                // Arabia and Nubia
                .territory("Levant", 530, 80)
                .territory("Nubian Shield", 520, 160)
                .territory("Arabia", 600, 130)
                .territory("Oman", 660, 190)
                // India and Antarctica
                .territory("Madagascar", 500, 330)
                .territory("India", 610, 280)
                .territory("Enderby Land", 580, 380)
                .territory("East Antarctica", 520, 450)
                .territory("Queen Maud Land", 430, 430)
                .territory("West Antarctica", 330, 470)
                // Australia
                .territory("Pilbara", 700, 330)
                .territory("Yilgarn", 680, 420)
                .territory("North Australia", 750, 260)
                .territory("Gawler", 740, 440)
                .territory("Tasmanides", 740, 520)

                .connect("Amazonia", "Sao Francisco")
                .connect("Amazonia", "Pampia")
                .connect("Pampia", "Rio de la Plata")
                .connect("Sao Francisco", "Rio de la Plata")
                .connect("Rio de la Plata", "Patagonia")
                .connect("West Africa", "Sahara Metacraton")
                .connect("West Africa", "Congo Craton")
                .connect("Sahara Metacraton", "Congo Craton")
                .connect("Congo Craton", "Tanzania Craton")
                .connect("Congo Craton", "Kalahari")
                .connect("Tanzania Craton", "Kalahari")
                .connect("Kalahari", "Kaapvaal")
                .connect("Levant", "Nubian Shield")
                .connect("Levant", "Arabia")
                .connect("Nubian Shield", "Arabia")
                .connect("Arabia", "Oman")
                .connect("Madagascar", "India")
                .connect("Madagascar", "Enderby Land")
                .connect("India", "Enderby Land")
                .connect("Enderby Land", "East Antarctica")
                .connect("East Antarctica", "Queen Maud Land")
                .connect("Queen Maud Land", "West Antarctica")
                .connect("North Australia", "Pilbara")
                .connect("Pilbara", "Yilgarn")
                .connect("Yilgarn", "Gawler")
                .connect("Gawler", "Tasmanides")
                .connect("North Australia", "Gawler")
                // sutures and margins between the regions
                .connect("Amazonia", "West Africa")
                .connect("Sao Francisco", "Congo Craton")
                .connect("Pampia", "Kaapvaal")
                .connect("Patagonia", "West Antarctica")
                .connect("Kalahari", "Queen Maud Land")
                .connect("Tanzania Craton", "Madagascar")
                .connect("Sahara Metacraton", "Nubian Shield")
                .connect("Oman", "India")
                .connect("East Antarctica", "Gawler")
                .connect("India", "Pilbara")

                .region(3, "Amazonia", "Sao Francisco", "Pampia", "Rio de la Plata", "Patagonia")
                .region(4, "West Africa", "Sahara Metacraton", "Congo Craton",
                        "Tanzania Craton", "Kalahari", "Kaapvaal")
                .region(2, "Levant", "Nubian Shield", "Arabia", "Oman")
                .region(4, "Madagascar", "India", "Enderby Land", "East Antarctica",
                        "Queen Maud Land", "West Antarctica")
                .region(3, "Pilbara", "Yilgarn", "North Australia", "Gawler", "Tasmanides")
                .build();
    }

    /**
     * Rodinia (~1 Ga): the Neoproterozoic supercontinent with Laurentia at its
     * core, following the SWEAT-style reconstruction that places Australia and
     * East Antarctica against Laurentia's western margin.
     */
    public static Map rodinia() {
        return new MapBuilder()
                // Laurentia core
                .territory("Slave", 180, 120)
                .territory("Laurentia", 260, 200)
                .territory("Wyoming Craton", 170, 260)
                .territory("Superior Craton", 350, 150)
                .territory("Grenville Belt", 400, 250)
                .territory("Siberia", 150, 60)
                // Atlantica
                .territory("Baltica", 520, 130)
                .territory("Amazonia", 560, 230)
                .territory("West Africa", 640, 160)
                .territory("Sao Francisco", 650, 300)
                .territory("Congo Craton", 720, 230)
                // Ur
                .territory("Kalahari", 690, 400)
                .territory("Zimbabwe Craton", 740, 330)
                .territory("India", 600, 420)
                .territory("Tarim", 520, 470)
                // Panthalassan margin
                .territory("North China", 420, 60)
                .territory("South China", 330, 400)
                .territory("Australia", 250, 450)
                .territory("Mawson Craton", 140, 400)
                .territory("East Antarctica", 230, 520)

                .connect("Slave", "Laurentia")
                .connect("Laurentia", "Wyoming Craton")
                .connect("Laurentia", "Superior Craton")
                .connect("Laurentia", "Grenville Belt")
                .connect("Superior Craton", "Grenville Belt")
                .connect("Slave", "Superior Craton")
                .connect("Slave", "Siberia")
                .connect("Baltica", "Amazonia")
                .connect("Baltica", "West Africa")
                .connect("Amazonia", "Sao Francisco")
                .connect("West Africa", "Congo Craton")
                .connect("Sao Francisco", "Congo Craton")
                .connect("Kalahari", "Zimbabwe Craton")
                .connect("Kalahari", "India")
                .connect("Zimbabwe Craton", "India")
                .connect("India", "Tarim")
                .connect("North China", "South China")
                .connect("South China", "Australia")
                .connect("Australia", "Mawson Craton")
                .connect("Australia", "East Antarctica")
                .connect("Mawson Craton", "East Antarctica")
                // sutures and margins between the regions
                .connect("Siberia", "North China")
                .connect("Grenville Belt", "Baltica")
                .connect("Grenville Belt", "Amazonia")
                .connect("Congo Craton", "Zimbabwe Craton")
                .connect("India", "East Antarctica")
                .connect("Tarim", "South China")
                .connect("Wyoming Craton", "Mawson Craton")

                .region(4, "Slave", "Laurentia", "Wyoming Craton", "Superior Craton",
                        "Grenville Belt", "Siberia")
                .region(3, "Baltica", "Amazonia", "West Africa", "Sao Francisco", "Congo Craton")
                .region(2, "Kalahari", "Zimbabwe Craton", "India", "Tarim")
                .region(3, "North China", "South China", "Australia", "Mawson Craton",
                        "East Antarctica")
                .build();
    }

    private static final class MapBuilder {
        private final Map map = new Map();
        private final java.util.Map<String, Field> fieldsByName = new LinkedHashMap<>();

        MapBuilder territory(String name, int x, int y) {
            if (fieldsByName.containsKey(name)) {
                throw new IllegalStateException("Duplicate territory: " + name);
            }
            Field field = new Field(new PointF(x, y));
            int index = map.getFields().size();
            field.setFieldIndex(index);
            field.setDisplayName(name);
            field.setCardSymbol(CardSymbol.values()[index % 3]);
            map.getFields().add(field);
            fieldsByName.put(name, field);
            return this;
        }

        MapBuilder connect(String a, String b) {
            get(a).addNext(get(b));
            return this;
        }

        MapBuilder region(int bonus, String... names) {
            Continent continent = new Continent(map.getContinents().size(), bonus);
            map.getContinents().add(continent);
            for (String name : names) {
                continent.addField(get(name));
            }
            return this;
        }

        private Field get(String name) {
            Field field = fieldsByName.get(name);
            if (field == null) {
                throw new IllegalStateException("Unknown territory: " + name);
            }
            return field;
        }

        Map build() {
            for (Field field : map.getFields()) {
                if (field.getContinent() == null) {
                    throw new IllegalStateException(
                            "Territory without a region: " + field.getDisplayName());
                }
            }
            return map;
        }
    }
}
