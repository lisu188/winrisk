package com.winrisk.game.mission;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MissionTests {

    @Test
    public void missionsAreAssignedToAllPlayers() throws Exception {
        Game game = TestUtil.createNewGame();

        game.getPlayers().forEach(player -> {
            assertNotNull(player.getMission());
            assertTrue(!player.getMission().targets(player));
        });
    }

    @Test
    public void completingMissionEndsGameEarly() throws Exception {
        Game game = TestUtil.createNewGame();

        game.getPlayers().get(0).setMission(new Mission("Auto win", (g, p) -> true));
        for (int i = 1; i < game.getPlayers().size(); i++) {
            game.getPlayers().get(i).setMission(new Mission("Never", (g, p) -> false));
        }

        assertTrue(game.end());
        assertEquals(game.getPlayers().get(0), game.getWinner());
    }
}
