package com.winrisk.game;

import com.winrisk.game.serialization.MapSketch;
import org.junit.Test;

public class GameTests {
    @Test
    public void serialization() throws Exception {
        MapSketch game = TestUtil.createNewGame().getMapSketch();
        String serializedGame = TestUtil.serialize(game);
        MapSketch newGame = TestUtil.deserialize(serializedGame, MapSketch.class);
        assert(game.equals(newGame));
    }
}
