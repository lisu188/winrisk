package com.winrisk.game.ai;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

public class EasyAITest {
    @Test
    public void testMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        EasyAI ai = new EasyAI();
        ai.reinforce(game);
        ai.move(game);
        ai.attack(game);
    }
}
