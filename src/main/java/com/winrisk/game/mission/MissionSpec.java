package com.winrisk.game.mission;

import java.io.Serializable;

/**
 * Serializable description of a {@link Mission} so that a mission can be
 * persisted and rebuilt when a game is saved and reloaded. Missions are
 * otherwise backed by lambda conditions that cannot be serialized directly.
 */
public class MissionSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        TERRITORY,
        FORTIFIED_TERRITORY,
        CONTINENTS,
        ELIMINATION
    }

    private Kind kind;
    private int territories;
    private int minimumArmies;
    private int continentCount;
    private int eliminationTargetIndex = -1;

    public MissionSpec() {
    }

    private MissionSpec(Kind kind) {
        this.kind = kind;
    }

    public static MissionSpec territory(int territories) {
        MissionSpec spec = new MissionSpec(Kind.TERRITORY);
        spec.territories = territories;
        return spec;
    }

    public static MissionSpec fortifiedTerritory(int territories, int minimumArmies) {
        MissionSpec spec = new MissionSpec(Kind.FORTIFIED_TERRITORY);
        spec.territories = territories;
        spec.minimumArmies = minimumArmies;
        return spec;
    }

    public static MissionSpec continents(int continentCount) {
        MissionSpec spec = new MissionSpec(Kind.CONTINENTS);
        spec.continentCount = continentCount;
        return spec;
    }

    public static MissionSpec elimination(int eliminationTargetIndex) {
        MissionSpec spec = new MissionSpec(Kind.ELIMINATION);
        spec.eliminationTargetIndex = eliminationTargetIndex;
        return spec;
    }

    public Kind getKind() {
        return kind;
    }

    public int getTerritories() {
        return territories;
    }

    public int getMinimumArmies() {
        return minimumArmies;
    }

    public int getContinentCount() {
        return continentCount;
    }

    public int getEliminationTargetIndex() {
        return eliminationTargetIndex;
    }
}
