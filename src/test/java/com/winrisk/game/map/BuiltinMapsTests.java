package com.winrisk.game.map;

import com.winrisk.game.data.Params;
import org.junit.Test;

import static org.junit.Assert.*;

public class BuiltinMapsTests {

    @Test
    public void registryListsAllBuiltinBoards() {
        assertEquals(5, BuiltinMaps.names().size());
        assertTrue(BuiltinMaps.names().containsAll(java.util.List.of(
                "world", "pangaea", "laurasia", "gondwana", "rodinia")));
    }

    @Test
    public void byNameIsCaseAndWhitespaceInsensitive() {
        assertEquals(32, BuiltinMaps.byName("PANGAEA").getFields().size());
        assertEquals(24, BuiltinMaps.byName(" Laurasia ").getFields().size());
        assertEquals(42, BuiltinMaps.byName("world").getFields().size());
    }

    @Test
    public void isBuiltinRecognisesNamesOnly() {
        assertTrue(BuiltinMaps.isBuiltin("gondwana"));
        assertTrue(BuiltinMaps.isBuiltin("Rodinia"));
        assertFalse(BuiltinMaps.isBuiltin("atlantis"));
        assertFalse(BuiltinMaps.isBuiltin("maps/custom.map"));
        assertFalse(BuiltinMaps.isBuiltin(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void byNameRejectsUnknownBoards() {
        BuiltinMaps.byName("atlantis");
    }

    @Test
    public void paramsLoadBuiltinMapByName() {
        Params params = new Params();
        params.setBuiltinMap("rodinia");
        assertEquals(20, params.loadMap().getFields().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void paramsRejectUnknownBuiltinMap() {
        Params params = new Params();
        params.setBuiltinMap("atlantis");
        params.loadMap();
    }
}
