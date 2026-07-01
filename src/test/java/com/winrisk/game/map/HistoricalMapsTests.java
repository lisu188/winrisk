package com.winrisk.game.map;

import com.winrisk.game.Play;
import com.winrisk.game.data.Params;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.serialization.MapSketch;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.*;

public class HistoricalMapsTests {

    private java.util.Map<String, Map> allMaps() {
        java.util.Map<String, Map> maps = new LinkedHashMap<>();
        maps.put("pangaea", HistoricalMaps.pangaea());
        maps.put("laurasia", HistoricalMaps.laurasia());
        maps.put("gondwana", HistoricalMaps.gondwana());
        maps.put("rodinia", HistoricalMaps.rodinia());
        return maps;
    }

    @Test
    public void historicalMapsHaveExpectedSize() {
        assertEquals(32, HistoricalMaps.pangaea().getFields().size());
        assertEquals(6, HistoricalMaps.pangaea().getContinents().size());
        assertEquals(24, HistoricalMaps.laurasia().getFields().size());
        assertEquals(5, HistoricalMaps.laurasia().getContinents().size());
        assertEquals(26, HistoricalMaps.gondwana().getFields().size());
        assertEquals(5, HistoricalMaps.gondwana().getContinents().size());
        assertEquals(20, HistoricalMaps.rodinia().getFields().size());
        assertEquals(4, HistoricalMaps.rodinia().getContinents().size());
    }

    @Test
    public void historicalMapsAreStructurallySound() {
        allMaps().forEach((name, map) -> {
            Set<String> names = new HashSet<>();
            for (int i = 0; i < map.getFields().size(); i++) {
                Field field = map.getFields().get(i);
                String label = name + "/" + field.getDisplayName();
                assertEquals(label + " index", i, field.getFieldIndex());
                assertTrue(label + " duplicate name", names.add(field.getDisplayName()));
                assertFalse(label + " generic name",
                        field.getDisplayName().matches("Territory \\d+"));
                assertTrue(label + " symbol", field.hasCardSymbol());
                assertNotNull(label + " region", field.getContinent());
                assertFalse(label + " isolated", field.getNext().isEmpty());
                for (Field next : field.getNext()) {
                    assertNotSame(label + " self link", field, next);
                    assertTrue(label + " asymmetric link to " + next.getDisplayName(),
                            next.getNext().contains(field));
                }
            }
            int regionTotal = 0;
            for (Continent continent : map.getContinents()) {
                assertTrue(name + " region bonus", continent.getBonus() > 0);
                assertFalse(name + " empty region", continent.getFields().isEmpty());
                regionTotal += continent.getFields().size();
            }
            assertEquals(name + " regions partition the board",
                    map.getFields().size(), regionTotal);
            assertTrue(name + " connected", isConnected(map));
        });
    }

    @Test
    public void everyHistoricalMapSupportsHeadlessPlay() {
        for (String name : List.of("pangaea", "laurasia", "gondwana", "rodinia")) {
            Params params = new Params();
            params.setHumanPlayers(0);
            params.setAiPlayers(3);
            params.setBuiltinMap(name);
            params.setRandomSeed(42L);
            Play.Result result = new Play(params, 400).playResult();
            assertNotNull(name, result);
            assertTrue(name + " must end in a win or a draw",
                    result.getWinner() != null || result.isDraw());
        }
    }

    @Test
    public void authoredNamesSurviveSketchRoundTrip() {
        Map original = HistoricalMaps.gondwana();
        MapSketch sketch = new MapSketch(original);
        Map restored = new Map();
        restored.fromSketch(sketch);

        assertEquals(original.getFields().size(), restored.getFields().size());
        for (int i = 0; i < original.getFields().size(); i++) {
            assertEquals(original.getFields().get(i).getDisplayName(),
                    restored.getFields().get(i).getDisplayName());
            assertEquals(original.getFields().get(i).getCardSymbol(),
                    restored.getFields().get(i).getCardSymbol());
        }
    }

    private boolean isConnected(Map map) {
        Set<Field> visited = new HashSet<>();
        Queue<Field> queue = new LinkedList<>();
        queue.add(map.getFields().get(0));
        while (!queue.isEmpty()) {
            Field current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            queue.addAll(current.getNext());
        }
        return visited.size() == map.getFields().size();
    }
}
