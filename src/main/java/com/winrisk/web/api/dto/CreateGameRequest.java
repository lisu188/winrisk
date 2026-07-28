package com.winrisk.web.api.dto;

public class CreateGameRequest {

    public String mode;
    public int aiPlayers;
    public MapChoice map;
    public Rules rules;
    public Long seed;

    public static class MapChoice {
        public String builtin;
        public String file;
        public RandomMap random;
    }

    public static class RandomMap {
        public int fields;
        public int continents;
    }

    public static class Rules {
        public boolean attackWithAll;
        public boolean fogOfWar;
        public boolean skynet;
        public boolean incrementalCardSetValues;
        public boolean expandedManeuver;
        public boolean attackCardReroll;
        public boolean commanderDie;
    }
}
