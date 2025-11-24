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
    private final Random random = new Random();

    public MissionDeck(Map map, PlayerList players) {
        missions = new ArrayList<>();
        missions.add(createTerritoryMission(24));
        missions.add(createFortifiedTerritoryMission(18, 2));
        missions.add(createContinentMission(2));
        missions.add(createContinentMission(3));
        missions.add(createFortifiedTerritoryMission(15, 3));
        for (Player target : players) {
            missions.add(createEliminationMission(target));
        }
        Collections.shuffle(missions, random);
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

    private Mission createContinentMission(int continentCount) {
        String description = "Conquer " + continentCount + " continents.";
        return new Mission(description, (game, player) -> game.getMap().getContinents()
                .stream()
                .filter(continent -> player.equals(continent.getPlayer()))
                .count() >= continentCount);
    }

    private Mission createEliminationMission(Player target) {
        String description = "Eliminate the player with color " + target.getColor();
        return new Mission(description, (game, player) -> {
            if (player.equals(target)) {
                return false;
            }
            if (target.isDead(game)) {
                return player.getFieldState(game) >= 24;
            }
            return target.isDead(game);
        }, target);
    }

    private Mission createFortifiedTerritoryMission(int territories, int minimumArmies) {
        String description = "Conquer " + territories + " territories with at least "
                + minimumArmies + " armies on each.";
        return new Mission(description, (game, player) -> game.getFields().stream()
                .filter(field -> player.equals(field.getPlayer()))
                .filter(field -> field.getArmy() >= minimumArmies)
                .count() >= territories);
    }

    private Mission createTerritoryMission(int territories) {
        String description = "Conquer " + territories + " territories.";
        return new Mission(description, (game, player) -> player.getFieldState(game) >= territories);
    }
}
