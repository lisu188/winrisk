package com.winrisk.web.api.dto;

public class AttackResponse {

    public GameView view;
    public CombatView combat;

    public static class CombatView {
        public boolean captured;
        public int[] attackerDice;
        public int[] defenderDice;
        public int attackerLosses;
        public int defenderLosses;
    }
}
