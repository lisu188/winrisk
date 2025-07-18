package com.winrisk.game.ai;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

public class PlayerAITest {
    @Test
    public void testNoOps() throws Exception {
        Game game = TestUtil.createNewGame();
        PlayerAI ai = new PlayerAI();
        ai.attack(game);
        ai.move(game);
        ai.reinforce(game);
    }
}
