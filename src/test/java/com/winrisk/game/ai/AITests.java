package com.winrisk.game.ai;

import com.winrisk.game.TestUtil;
import com.winrisk.game.view.Game;
import org.junit.Test;

import static org.junit.Assert.*;

public class AITests {
    @Test
    public void continentAiMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        ContinentAI ai = new ContinentAI();
        ai.reinforce(game);
        ai.move(game);
        ai.attack(game);
    }

    @Test
    public void easyAiMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        EasyAI ai = new EasyAI();
        ai.reinforce(game);
        ai.move(game);
        ai.attack(game);
    }

    @Test
    public void playerAiNoOps() throws Exception {
        Game game = TestUtil.createNewGame();
        PlayerAI ai = new PlayerAI();
        ai.attack(game);
        ai.move(game);
        ai.reinforce(game);
    }

    @Test
    public void playerFactory() {
        PlayerInterface human = PlayerFactory.getHuman();
        assertNotNull(human);
        assertTrue(human.isInteractive());
        PlayerInterface ai = PlayerFactory.getRandomAI();
        assertNotNull(ai);
        assertFalse(ai.isInteractive());
    }
}
