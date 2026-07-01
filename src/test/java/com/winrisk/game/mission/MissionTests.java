package com.winrisk.game.mission;

import com.winrisk.game.TestUtil;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void eliminationMissionCompletesWhenTargetIsEliminated() throws Exception {
        Game game = TestUtil.createNewGame();
        Player hunter = game.getPlayers().get(0);
        Player prey = game.getPlayers().get(1);
        Mission mission = MissionDeck.createEliminationMission(prey);

        // While the target still holds territory the mission is not complete.
        assertFalse(mission.isCompleted(game, hunter));

        // Eliminate the target by transferring all of its territories to the hunter.
        for (Field field : new FieldList(prey.getFields(game))) {
            field.setPlayer(hunter);
        }
        assertTrue(prey.isDead(game));
        assertTrue(mission.isCompleted(game, hunter));
    }

    @Test
    public void eliminationMissionFallsBackToTerritoriesWhenTargetingSelf() throws Exception {
        Game game = TestUtil.createNewGame();
        Player player = game.getPlayers().get(0);
        Mission mission = MissionDeck.createEliminationMission(player);

        // A self-targeting mission cannot be won by elimination; it requires 24 territories.
        assertFalse(mission.isCompleted(game, player));

        FieldList fields = game.getFields();
        for (int i = 0; i < 24; i++) {
            fields.get(i).setPlayer(player);
        }
        assertTrue(mission.isCompleted(game, player));
    }
}
