package com.winrisk.game.ai;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

public class ContinentAITest {
    @Test
    public void testMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        ContinentAI ai = new ContinentAI();
        ai.reinforce(game);
        ai.move(game);
        ai.attack(game);
    }
}
