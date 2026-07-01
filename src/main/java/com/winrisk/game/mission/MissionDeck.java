package com.winrisk.game.mission;

import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MissionDeck {

    private final List<Mission> missions;
    private final Random random;

    public MissionDeck(Map map, PlayerList players) {
        this(map, players, new Random());
    }

    public MissionDeck(Map map, PlayerList players, Random random) {
        this.random = random;
        missions = new ArrayList<>();
        missions.add(createTerritoryMission(24));
        missions.add(createFortifiedTerritoryMission(18, 2));
        missions.add(createContinentMission(2));
        missions.add(createContinentMission(3));
        missions.add(createFortifiedTerritoryMission(15, 3));
        for (int i = 0; i < players.size(); i++) {
            missions.add(createEliminationMission(players.get(i), i));
        }
        Collections.shuffle(missions, random);
    }

    /**
     * Rebuilds a mission from its serializable {@link MissionSpec}, resolving
     * any player reference against the supplied roster. Used when loading a
     * saved game.
     */
    public static Mission fromSpec(MissionSpec spec, PlayerList players) {
        switch (spec.getKind()) {
            case TERRITORY:
                return createTerritoryMission(spec.getTerritories());
            case FORTIFIED_TERRITORY:
                return createFortifiedTerritoryMission(spec.getTerritories(), spec.getMinimumArmies());
            case CONTINENTS:
                return createContinentMission(spec.getContinentCount());
            case ELIMINATION:
                int index = spec.getEliminationTargetIndex();
                return createEliminationMission(players.get(index), index);
            default:
                throw new IllegalArgumentException("Unknown mission kind: " + spec.getKind());
        }
    }

    public Mission draw(Player player) {
        List<Mission> availableMissions = missions.stream()
                .filter(mission -> !mission.targets(player))
                .collect(Collectors.toCollection(ArrayList::new));
        if (availableMissions.isEmpty()) {
            availableMissions = new ArrayList<>(missions);
        }
        if (availableMissions.isEmpty()) {
            return createTerritoryMission(24);
        }
        Mission mission = availableMissions.get(random.nextInt(availableMissions.size()));
        missions.remove(mission);
        return mission;
    }

    private static Mission createContinentMission(int continentCount) {
        String description = "Conquer " + continentCount + " continents.";
        return new Mission(description, (game, player) -> game.getMap().getContinents()
                .stream()
                .filter(continent -> player.equals(continent.getPlayer()))
                .count() >= continentCount, null, MissionSpec.continents(continentCount));
    }

    static Mission createEliminationMission(Player target, int targetIndex) {
        String description = "Eliminate the player with color " + target.getColor();
        return new Mission(description, (game, player) -> {
            // A player can never eliminate themselves, so the official fallback
            // objective for a self-targeting mission is to capture 24 territories.
            if (player.equals(target)) {
                return player.getFieldState(game) >= 24;
            }
            return target.isDead(game);
        }, target, MissionSpec.elimination(targetIndex));
    }

    private static Mission createFortifiedTerritoryMission(int territories, int minimumArmies) {
        String description = "Conquer " + territories + " territories with at least "
                + minimumArmies + " armies on each.";
        return new Mission(description, (game, player) -> game.getFields().stream()
                .filter(field -> player.equals(field.getPlayer()))
                .filter(field -> field.getArmy() >= minimumArmies)
                .count() >= territories, null, MissionSpec.fortifiedTerritory(territories, minimumArmies));
    }

    private static Mission createTerritoryMission(int territories) {
        String description = "Conquer " + territories + " territories.";
        return new Mission(description, (game, player) -> player.getFieldState(game) >= territories,
                null, MissionSpec.territory(territories));
    }
}
