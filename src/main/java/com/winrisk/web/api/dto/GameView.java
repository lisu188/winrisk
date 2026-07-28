package com.winrisk.web.api.dto;

import java.util.List;

/**
 * The complete state a client needs to render and play a game. Purpose-built
 * for the API: no background image bytes, no deck contents, no other players'
 * hands or missions. Field order matches the frontend contract exactly.
 */
public class GameView {

    public String id;
    public long version;
    public String mode;
    public String phase;
    public int currentPlayerIndex;
    public int humanPlayerIndex;
    public int reinforcements;
    public boolean maneuverUsed;
    public boolean commanderDieUsed;
    public int winnerIndex = -1;
    public String winReason;
    public boolean draw;
    public boolean hasBackgroundImage;
    public String missionText;
    public String hqName;
    public int hqIndex = -1;
    public RulesView rules;
    public List<FieldView> fields;
    public List<ContinentView> continents;
    public List<int[]> links;
    public List<PlayerView> players;
    public List<CardView> hand;
    public boolean tradeForced;

    public static class RulesView {
        public boolean attackWithAll;
        public boolean fogOfWar;
        public boolean skynet;
        public boolean incrementalCardSetValues;
        public boolean expandedManeuver;
        public boolean attackCardReroll;
        public boolean commanderDie;
    }

    public static class FieldView {
        public int index;
        public String name;
        public int x;
        public int y;
        public int ownerIndex = -1;
        public int army;
        public int continentIndex = -1;
    }

    public static class ContinentView {
        public int index;
        public int bonus;
        public int colorRgb;
    }

    public static class PlayerView {
        public int colorRgb;
        public int textColorRgb;
        public boolean neutral;
        public boolean ai;
        public boolean dead;
        public int territories;
        public int cardCount;
        public int reinforcements;
    }

    public static class CardView {
        public int fieldIndex;
        public String symbol;
        public String territoryName;
    }
}
