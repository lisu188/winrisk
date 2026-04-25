package com.winrisk.game.rules;

import com.winrisk.game.Play;
import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import com.winrisk.gui.StartGame;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class OfficialRulesTests {

    @Test
    public void classicSetupClaimsAllTerritoriesWithOfficialTroopCounts() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 42L);

        assertEquals(3, game.getPlayers().size());
        assertEquals(42, game.getFields().stream().filter(field -> field.getPlayer() != null).count());
        for (Player player : game.getPlayers()) {
            assertFalse(player.isNeutral());
            assertEquals(35, totalArmies(game, player));
        }
    }

    @Test
    public void secretMissionSetupDealsMissionsAndTerritories() throws Exception {
        Game game = game(GameMode.SECRET_MISSION, 3, 42L);

        for (Player player : game.getPlayers()) {
            assertNotNull(player.getMission());
            assertTrue(player.getFieldState(game) >= 14);
            assertEquals(35, totalArmies(game, player));
        }
    }

    @Test
    public void classicTwoPlayerSetupCreatesNeutralArmyThatDoesNotTakeTurns() throws Exception {
        Game game = game(GameMode.CLASSIC, 2, 7L);
        Player neutral = game.getNeutralPlayer();

        assertNotNull(neutral);
        assertTrue(neutral.isNeutral());
        assertEquals(3, game.getPlayers().size());
        assertEquals(14, neutral.getFieldState(game));
        assertEquals(40, totalArmies(game, neutral));
        for (int i = 0; i < 12; i++) {
            game.next();
            assertFalse(game.getPlayer().isNeutral());
        }
        assertEquals(0, neutral.getCurrentReinforcements());
    }

    @Test
    public void capitalSetupSelectsHeadquartersAndRemovesTheirCardsFromDeck() throws Exception {
        Game game = game(GameMode.CAPITAL, 3, 11L);

        for (Player player : game.getPlayers()) {
            assertNotNull(player.getHeadquarters());
            assertEquals(player, player.getHeadquarters().getPlayer());
        }
        assertEquals(41, game.getCardService().getDeck().size());
    }

    @Test
    public void cardDeckUsesFortyTwoTerritoriesAndTwoWilds() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 4L);
        List<RiskCard> territoryCards = RiskDeck.territoryCards(game.getMap());

        assertEquals(42, territoryCards.size());
        assertEquals(14, territoryCards.stream().filter(card -> card.getSymbol() == CardSymbol.INFANTRY).count());
        assertEquals(14, territoryCards.stream().filter(card -> card.getSymbol() == CardSymbol.CAVALRY).count());
        assertEquals(14, territoryCards.stream().filter(card -> card.getSymbol() == CardSymbol.ARTILLERY).count());
        assertEquals(44, game.getCardService().getDeck().size());
    }

    @Test
    public void cardSetsTradeValuesMandatoryTradesAndOccupiedBonusesWork() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 4L);
        Player player = game.getPlayers().get(0);
        Field occupied = player.getFields(game).get(0);
        int before = occupied.getArmy();
        player.setRein(0);
        player.getRiskCards().clear();
        player.addCard(RiskCard.territory(occupied));
        player.addCard(sameSymbolCard(game, occupied, player.getRiskCards()));
        player.addCard(sameSymbolCard(game, occupied, player.getRiskCards()));
        player.addCard(RiskCard.wild());
        player.addCard(RiskCard.wild());

        assertTrue(game.getCardService().isTradeForced(player));
        game.getCardService().beginTurn(game, player);

        assertEquals(2, player.getRiskCards().size());
        assertEquals(4, player.getCurrentReinforcements());
        assertEquals(before + 2, occupied.getArmy());

        player.getRiskCards().clear();
        RiskCard firstWild = RiskCard.wild();
        RiskCard secondWild = RiskCard.wild();
        RiskCard first = RiskCard.territory(game.getFields().get(0));
        RiskCard second = sameSymbolCard(game, game.getFields().get(0), java.util.List.of(first));
        player.addCard(first);
        player.addCard(second);
        player.addCard(firstWild);
        player.addCard(secondWild);
        game.getCardService().trade(game, player, java.util.List.of(first, second, firstWild));
        assertEquals(1, player.getRiskCards().size());
        assertTrue(player.getRiskCards().get(0).isWild());

        Game incrementalGame = game(GameMode.CLASSIC, 3, 6L);
        incrementalGame.getParams().getRulesOptions().setIncrementalCardSetValues(true);
        Player incrementalPlayer = incrementalGame.getPlayers().get(0);
        incrementalPlayer.setRein(0);
        incrementalPlayer.getRiskCards().clear();
        List<RiskCard> cards = RiskDeck.territoryCards(incrementalGame.getMap()).stream()
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
        cards.forEach(incrementalPlayer::addCard);
        incrementalGame.getCardService().trade(incrementalGame, incrementalPlayer, cards);
        assertEquals(5, incrementalGame.getCardService().nextTradeValue());
    }

    @Test
    public void eliminatedPlayerCardsTransferAndImmediatelyTradeDownFromSix() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 5L);
        Player attacker = game.getPlayers().get(0);
        Player eliminated = game.getPlayers().get(1);
        attacker.getRiskCards().clear();
        eliminated.getRiskCards().clear();
        RiskDeck.territoryCards(game.getMap()).stream().limit(3).forEach(attacker::addCard);
        RiskDeck.territoryCards(game.getMap()).stream().skip(3).limit(3).forEach(eliminated::addCard);
        eliminated.getFields(game).forEach(field -> field.setPlayer(attacker));

        attacker.takeCards(eliminated, game);

        assertTrue(attacker.getRiskCards().size() <= 4);
        assertTrue(attacker.getCurrentReinforcements() > 0);
    }

    @Test
    public void combatUsesDiceLimitsTiesAndCaptureOccupationRules() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 12L);
        Player attacker = game.getPlayers().get(0);
        Player defender = game.getPlayers().get(1);
        Field source = game.getFields().get(0);
        Field target = source.getNext().get(0);
        source.setPlayer(attacker);
        target.setPlayer(defender);
        source.setArmy(2);
        target.setArmy(1);

        CombatResult tie = new CombatResolver(new FixedRandom(3, 3)).attack(game, source, target);

        assertTrue(tie.isLegal());
        assertFalse(tie.isCaptured());
        assertEquals(1, tie.getAttackerLosses());
        assertEquals(1, source.getArmy());
        assertEquals(1, target.getArmy());

        source.setArmy(4);
        target.setArmy(1);
        CombatResult capture = new CombatResolver(new FixedRandom(6, 5, 4, 1)).attack(game, source, target);

        assertEquals(3, capture.getAttackerDice().length);
        assertEquals(1, capture.getDefenderDice().length);
        assertTrue(capture.isCaptured());
        assertEquals(attacker, target.getPlayer());
        assertEquals(1, source.getArmy());
        assertEquals(3, target.getArmy());
    }

    @Test
    public void expertAttackOptionsApplyRerollAndCommanderDie() throws Exception {
        Game rerollGame = game(GameMode.CLASSIC, 3, 12L);
        rerollGame.getParams().getRulesOptions().setAttackCardReroll(true);
        Player rerollAttacker = rerollGame.getPlayers().get(0);
        Player rerollDefender = rerollGame.getPlayers().get(1);
        Field rerollSource = rerollGame.getFields().get(0);
        Field rerollTarget = rerollSource.getNext().get(0);
        rerollSource.setPlayer(rerollAttacker);
        rerollTarget.setPlayer(rerollDefender);
        rerollSource.setArmy(2);
        rerollTarget.setArmy(1);
        rerollAttacker.addCard(RiskCard.territory(rerollSource));

        CombatResult rerolled = new CombatResolver(new FixedRandom(1, 5, 6))
                .attack(rerollGame, rerollSource, rerollTarget);

        assertTrue(rerolled.isCaptured());

        Game commanderGame = game(GameMode.CLASSIC, 3, 13L);
        commanderGame.getParams().getRulesOptions().setCommanderDie(true);
        Player commanderAttacker = commanderGame.getPlayers().get(0);
        Player commanderDefender = commanderGame.getPlayers().get(1);
        Field commanderSource = commanderGame.getFields().get(0);
        Field commanderTarget = commanderSource.getNext().get(0);
        commanderSource.setPlayer(commanderAttacker);
        commanderTarget.setPlayer(commanderDefender);
        commanderSource.setArmy(2);
        commanderTarget.setArmy(1);

        CombatResult commanded = new CombatResolver(new FixedRandom(1, 5))
                .attack(commanderGame, commanderSource, commanderTarget);

        assertTrue(commanded.isCaptured());
        assertTrue(commanderGame.isCommanderDieUsed());
    }

    @Test
    public void conquestAwardsOnlyOneCardPerAttackPhase() throws Exception {
        Game game = game(GameMode.CLASSIC, 3, 9L);
        Player player = game.getPlayers().get(0);
        int before = player.getRiskCards().size();

        player.setConqueredTerritoryThisTurn(true);
        game.getCardService().awardConquestCard(player);
        game.getCardService().awardConquestCard(player);

        assertEquals(before + 1, player.getRiskCards().size());
    }

    @Test
    public void modeWinConditionsCoverSecretClassicNeutralAndCapital() throws Exception {
        Game secret = game(GameMode.SECRET_MISSION, 3, 1L);
        secret.getPlayers().get(0).setMission(new Mission("Auto", (g, p) -> true));
        assertTrue(secret.end());
        assertEquals("secret mission completed", secret.getWinReason());

        Game classic = game(GameMode.CLASSIC, 3, 2L);
        Player classicWinner = classic.getPlayers().get(0);
        classic.getFields().forEach(field -> field.setPlayer(classicWinner));
        assertTrue(classic.end());
        assertEquals(classicWinner, classic.getWinner());

        Game twoPlayer = game(GameMode.CLASSIC, 2, 3L);
        Player activeWinner = twoPlayer.getPlayers().get(0);
        Player defeated = twoPlayer.getPlayers().get(1);
        defeated.getFields(twoPlayer).forEach(field -> field.setPlayer(activeWinner));
        assertTrue(twoPlayer.end());
        assertEquals(activeWinner, twoPlayer.getWinner());
        assertTrue(twoPlayer.getNeutralPlayer().getFieldState(twoPlayer) > 0);

        Game capital = game(GameMode.CAPITAL, 3, 4L);
        Player capitalWinner = capital.getPlayers().get(0);
        for (Player player : capital.getPlayers()) {
            player.getHeadquarters().setPlayer(capitalWinner);
        }
        assertTrue(capital.end());
        assertEquals("captured all opposing headquarters", capital.getWinReason());
    }

    @Test
    public void cliFlagsAndSeededHeadlessPlayAreDeterministic() throws Exception {
        StartGame.HeadlessConfig config = StartGame.buildHeadlessConfig(new String[]{
                "--headless-play",
                "--mode=capital",
                "--seed=99",
                "--incremental-cards",
                "--expanded-maneuver",
                "--attack-card-reroll",
                "--commander-die"
        });

        assertEquals(GameMode.CAPITAL, config.getParams().getGameMode());
        assertEquals(Long.valueOf(99L), config.getParams().getRandomSeed());
        assertTrue(config.getParams().getRulesOptions().isIncrementalCardSetValues());
        assertTrue(config.getParams().getRulesOptions().isExpandedManeuver());
        assertTrue(config.getParams().getRulesOptions().isAttackCardReroll());
        assertTrue(config.getParams().getRulesOptions().isCommanderDie());

        String first = deterministicReport(123L);
        String second = deterministicReport(123L);
        assertEquals(first, second);
    }

    private String deterministicReport(long seed) throws Exception {
        Params params = params(GameMode.CLASSIC, 3, seed);
        params.setAiFactory(index -> new FinishAI());
        return new Play(params, 50).playResult().toReport();
    }

    private Game game(GameMode mode, int aiPlayers, long seed) throws Exception {
        return new Game(params(mode, aiPlayers, seed));
    }

    private Params params(GameMode mode, int aiPlayers, long seed) throws Exception {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(aiPlayers);
        params.setGameMode(mode);
        params.setRandomSeed(seed);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        return params;
    }

    private int totalArmies(Game game, Player player) {
        return player.getFields(game).stream().mapToInt(Field::getArmy).sum();
    }

    private RiskCard sameSymbolCard(Game game, Field field, List<RiskCard> excluded) {
        return RiskDeck.territoryCards(game.getMap()).stream()
                .filter(card -> card.getSymbol() == field.getCardSymbol())
                .filter(card -> !excluded.contains(card))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static class FixedRandom extends Random {
        private final int[] rolls;
        private int index;

        FixedRandom(int... rolls) {
            this.rolls = rolls;
        }

        @Override
        public int nextInt(int bound) {
            return rolls[index++ % rolls.length] - 1;
        }
    }

    private static class FinishAI implements PlayerInterface {
        @Override
        public void move(Game game) {
            captureEverything(game);
        }

        @Override
        public void reinforce(Game game) {
            captureEverything(game);
        }

        @Override
        public void attack(Game game) {
            captureEverything(game);
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        private void captureEverything(Game game) {
            Player player = game.getPlayer();
            game.getFields().forEach(field -> {
                field.setPlayer(player);
                field.setArmy(Math.max(1, field.getArmy()));
            });
        }
    }
}
