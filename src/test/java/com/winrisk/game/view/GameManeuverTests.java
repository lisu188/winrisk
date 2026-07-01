package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameManeuverTests {

    private Field[] preparePlayerRoute(Game game) {
        Player player = game.getPlayer();
        Field from = game.getFields().get(0);
        Field to = from.getNext().get(0);
        from.setPlayer(player);
        to.setPlayer(player);
        from.setArmy(10);
        to.setArmy(1);
        return new Field[]{from, to};
    }

    @Test
    public void fortificationMovesMultipleArmiesAlongOneRoute() throws Exception {
        Game game = TestUtil.createNewGame();
        game.setPhase(GamePhase.MOVE);
        Field[] route = preparePlayerRoute(game);
        Field from = route[0];
        Field to = route[1];

        // Move armies one at a time, as the UI does when dragging.
        assertTrue(game.maneuver(from, to, 1));
        assertTrue(game.isManeuverUsed());
        assertTrue(game.maneuver(from, to, 1));
        assertTrue(game.maneuver(from, to, 1));

        assertEquals(7, from.getArmy());
        assertEquals(4, to.getArmy());
    }

    @Test
    public void fortificationRejectsASecondDifferentRoute() throws Exception {
        Game game = TestUtil.createNewGame();
        game.setPhase(GamePhase.MOVE);
        Field[] route = preparePlayerRoute(game);
        Field from = route[0];
        Field to = route[1];

        assertTrue(game.maneuver(from, to, 1));

        // Reversing (or otherwise changing) the route is a second fortification
        // and must be rejected; only one fortification move is allowed per turn.
        assertFalse(game.maneuver(to, from, 1));
        assertEquals(9, from.getArmy());
        assertEquals(2, to.getArmy());
    }

    @Test
    public void fortificationLeavesAtLeastOneArmyBehind() throws Exception {
        Game game = TestUtil.createNewGame();
        game.setPhase(GamePhase.MOVE);
        Field[] route = preparePlayerRoute(game);
        Field from = route[0];
        Field to = route[1];
        from.setArmy(3);

        // Moving all 3 is capped so one army stays behind.
        assertTrue(game.maneuver(from, to, 3));
        assertEquals(1, from.getArmy());
        assertEquals(3, to.getArmy());
    }
}
