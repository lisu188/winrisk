package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

/** Ensure exception thrown when taking cards from a living player. */
public class PlayerTakeCardsTest {
    @Test(expected = RuntimeException.class)
    public void testTakeCardsFromLivingPlayerFails() throws Exception {
        Game game = TestUtil.createNewGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        p1.takeCards(p2, game);
    }
}
