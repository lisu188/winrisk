package com.winrisk.game.rules;

import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class OfficialSetup {
    private final Game game;
    private final Random random;

    public OfficialSetup(Game game, Random random) {
        this.game = game;
        this.random = random;
    }

    public int setup() {
        GameMode mode = game.getParams().getGameMode();
        switch (mode) {
            case SECRET_MISSION:
                return setupSecretMission();
            case CAPITAL:
                return setupCapital();
            case CLASSIC:
            default:
                return setupClassic();
        }
    }

    public static int startingTroops(int activePlayers) {
        if (activePlayers == 2) {
            return 40;
        }
        if (activePlayers == 3) {
            return 35;
        }
        if (activePlayers == 4) {
            return 30;
        }
        if (activePlayers == 5) {
            return 25;
        }
        throw new IllegalArgumentException("Official RISK supports 2-5 players");
    }

    private int setupSecretMission() {
        game.assignMissions();
        dealTerritoriesToPlayers(activePlayers());
        placeRemainingTroopsOneAtATime(activePlayers(), startingTroops(activePlayers().size()));
        game.getCardService().resetDrawDeck(game);
        return rollHighest(activePlayers());
    }

    private int setupClassic() {
        List<Player> activePlayers = activePlayers();
        if (activePlayers.size() == 2) {
            return setupTwoPlayerClassic(activePlayers);
        }
        int first = rollHighest(activePlayers);
        claimTerritories(first, activePlayers);
        placeRemainingTroopsOneAtATime(activePlayers, startingTroops(activePlayers.size()));
        game.getCardService().resetDrawDeck(game);
        return first;
    }

    private int setupCapital() {
        List<Player> activePlayers = activePlayers();
        int first = rollHighest(activePlayers);
        claimTerritories(first, activePlayers);
        placeRemainingTroopsOneAtATime(activePlayers, startingTroops(activePlayers.size()));
        chooseHeadquarters(activePlayers);
        game.getCardService().resetDrawDeck(game, headquarters(activePlayers));
        return first;
    }

    private int setupTwoPlayerClassic(List<Player> activePlayers) {
        Player neutral = game.getNeutralPlayer();
        List<Player> piles = new ArrayList<>(activePlayers);
        piles.add(neutral);
        List<RiskCard> cards = game.getCardService().shuffledTerritoryCards(game);
        for (int i = 0; i < cards.size(); i++) {
            Player owner = piles.get(i % piles.size());
            Field field = cards.get(i).getField();
            field.setPlayer(owner);
            field.setArmy(1);
        }
        placeTwoPlayerRemainingTroops(activePlayers, neutral);
        game.getCardService().resetDrawDeck(game);
        return rollHighest(activePlayers);
    }

    private void dealTerritoriesToPlayers(List<Player> players) {
        List<RiskCard> cards = game.getCardService().shuffledTerritoryCards(game);
        for (int i = 0; i < cards.size(); i++) {
            Player owner = players.get(i % players.size());
            Field field = cards.get(i).getField();
            field.setPlayer(owner);
            field.setArmy(1);
        }
    }

    private void claimTerritories(int firstPlayerIndex, List<Player> players) {
        List<Field> unclaimed = new ArrayList<>(game.getFields());
        int current = firstPlayerIndex;
        while (!unclaimed.isEmpty()) {
            Player player = game.getPlayers().get(current);
            if (!player.isNeutral()) {
                Field choice = player.getPlayerInterface()
                        .chooseSetupTerritory(game, new ArrayList<>(unclaimed));
                if (choice == null || !unclaimed.contains(choice)) {
                    choice = unclaimed.get(0);
                }
                choice.setPlayer(player);
                choice.setArmy(1);
                unclaimed.remove(choice);
            }
            current = nextActiveIndex(current);
        }
    }

    private void placeRemainingTroopsOneAtATime(List<Player> players, int startingTroops) {
        int[] remaining = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            remaining[i] = startingTroops - players.get(i).getFieldState(game);
        }
        boolean placed;
        do {
            placed = false;
            for (int i = 0; i < players.size(); i++) {
                if (remaining[i] <= 0) {
                    continue;
                }
                Player player = players.get(i);
                reinforceSetup(player, 1);
                remaining[i]--;
                placed = true;
            }
        } while (placed);
    }

    private void placeTwoPlayerRemainingTroops(List<Player> activePlayers, Player neutral) {
        int startingTroops = startingTroops(2);
        int[] remaining = new int[activePlayers.size()];
        for (int i = 0; i < activePlayers.size(); i++) {
            remaining[i] = startingTroops - activePlayers.get(i).getFieldState(game);
        }
        int neutralRemaining = startingTroops - neutral.getFieldState(game);
        while (remaining[0] > 0 || remaining[1] > 0 || neutralRemaining > 0) {
            for (int i = 0; i < activePlayers.size(); i++) {
                int troops = Math.min(2, remaining[i]);
                if (troops > 0) {
                    reinforceSetup(activePlayers.get(i), troops);
                    remaining[i] -= troops;
                }
                if (neutralRemaining > 0) {
                    reinforceSetup(neutral, 1);
                    neutralRemaining--;
                }
            }
        }
    }

    private void reinforceSetup(Player player, int troops) {
        List<Field> owned = player.getFields(game);
        if (owned.isEmpty()) {
            return;
        }
        Field target = player.getPlayerInterface()
                .chooseSetupReinforcement(game, player, owned, troops);
        if (target == null || !owned.contains(target)) {
            target = owned.get(0);
        }
        target.setArmy(target.getArmy() + troops);
    }

    private void chooseHeadquarters(List<Player> activePlayers) {
        for (Player player : activePlayers) {
            List<Field> owned = player.getFields(game);
            Field headquarters = player.getPlayerInterface()
                    .chooseHeadquarters(game, player, owned);
            if (headquarters == null || !owned.contains(headquarters)) {
                headquarters = owned.get(0);
            }
            player.setHeadquarters(headquarters);
        }
    }

    private Collection<Field> headquarters(List<Player> players) {
        return players.stream()
                .map(Player::getHeadquarters)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private int rollHighest(List<Player> players) {
        int first = 0;
        int high = -1;
        boolean tied;
        do {
            tied = false;
            high = -1;
            first = 0;
            for (Player player : players) {
                int roll = random.nextInt(6) + 1;
                int index = game.getPlayers().indexOf(player);
                if (roll > high) {
                    high = roll;
                    first = index;
                    tied = false;
                } else if (roll == high) {
                    tied = true;
                }
            }
        } while (tied);
        return first;
    }

    private int nextActiveIndex(int index) {
        PlayerList players = game.getPlayers();
        int current = index;
        do {
            current = (current + 1) % players.size();
        } while (players.get(current).isNeutral());
        return current;
    }

    private List<Player> activePlayers() {
        return game.getPlayers().stream()
                .filter(player -> !player.isNeutral())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
