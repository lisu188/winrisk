package com.winrisk.game.object;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerBonusTest {
    @Test
    public void testBonuses() throws Exception {
        Game game = TestUtil.createNewGame();
        Player player = game.getPlayers().get(0);
        player.setRein(0);
        player.getCards()[0] = 3; // guarantee a bonus
        int bonus = player.getCardBonus();
        player.applyCardBonus();
        assertEquals(4, bonus);
        assertEquals(bonus, player.getCurrentReinforcements());
    }
}
