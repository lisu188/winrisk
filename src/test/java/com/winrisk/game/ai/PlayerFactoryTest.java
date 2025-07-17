package com.winrisk.game.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerFactoryTest {
    @Test
    public void testFactory() {
        PlayerInterface human = PlayerFactory.getHuman();
        assertNotNull(human);
        assertTrue(human.isInteractive());
        PlayerInterface ai = PlayerFactory.getRandomAI();
        assertNotNull(ai);
        assertFalse(ai.isInteractive());
    }
}
