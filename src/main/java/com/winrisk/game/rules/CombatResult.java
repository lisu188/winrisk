package com.winrisk.game.rules;

import com.winrisk.game.object.Player;

import java.util.Arrays;

public class CombatResult {
    private final boolean legal;
    private final boolean captured;
    private final Player attacker;
    private final Player defender;
    private final int[] attackerDice;
    private final int[] defenderDice;
    private final int attackerLosses;
    private final int defenderLosses;

    public CombatResult(boolean legal,
                        boolean captured,
                        Player attacker,
                        Player defender,
                        int[] attackerDice,
                        int[] defenderDice,
                        int attackerLosses,
                        int defenderLosses) {
        this.legal = legal;
        this.captured = captured;
        this.attacker = attacker;
        this.defender = defender;
        this.attackerDice = attackerDice;
        this.defenderDice = defenderDice;
        this.attackerLosses = attackerLosses;
        this.defenderLosses = defenderLosses;
    }

    public static CombatResult illegal() {
        return new CombatResult(false, false, null, null, new int[0], new int[0], 0, 0);
    }

    public boolean isLegal() {
        return legal;
    }

    public boolean isCaptured() {
        return captured;
    }

    public Player getAttacker() {
        return attacker;
    }

    public Player getDefender() {
        return defender;
    }

    public int[] getAttackerDice() {
        return Arrays.copyOf(attackerDice, attackerDice.length);
    }

    public int[] getDefenderDice() {
        return Arrays.copyOf(defenderDice, defenderDice.length);
    }

    public int getAttackerLosses() {
        return attackerLosses;
    }

    public int getDefenderLosses() {
        return defenderLosses;
    }
}
