package com.winrisk.game.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class GamePhaseTest {
    @Test
    public void testCycle() {
        GamePhase phase = GamePhase.ATTACK;
        assertEquals(GamePhase.MOVE, phase.getNextState());
        phase = phase.getNextState();
        assertEquals(GamePhase.REINFORCE, phase.getNextState());
        assertEquals(GamePhase.REINFORCE, GamePhase.UNDEFINED.getNextState());
    }
}
