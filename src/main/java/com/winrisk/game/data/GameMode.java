package com.winrisk.game.data;

public enum GameMode {
    SECRET_MISSION,
    CLASSIC,
    CAPITAL;

    public static GameMode fromCli(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Game mode is required");
        }
        switch (value.trim().toLowerCase()) {
            case "secret":
            case "secret-mission":
            case "secret_mission":
                return SECRET_MISSION;
            case "classic":
                return CLASSIC;
            case "capital":
                return CAPITAL;
            default:
                throw new IllegalArgumentException("Unknown game mode: " + value);
        }
    }

    public String toCliValue() {
        switch (this) {
            case SECRET_MISSION:
                return "secret";
            case CLASSIC:
                return "classic";
            case CAPITAL:
                return "capital";
            default:
                return name().toLowerCase();
        }
    }
}
