package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.cluster.PatchList;
import com.winrisk.game.view.Game;
import com.winrisk.game.ai.PlayerFactory;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class PlayerExtraTest {
    @Test
    public void testPlayerMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        Player p1 = game.getPlayers().get(0);
        assertFalse(p1.isDead(game));
        assertNotNull(p1.getBorders(game));
        PatchList patches = p1.getPatchList(game);
        assertTrue(patches.size() > 0);

        Player dead = new Player(Color.PINK, PlayerFactory.getHuman());
        p1.takeCards(dead, game); // should not throw
    }
}
