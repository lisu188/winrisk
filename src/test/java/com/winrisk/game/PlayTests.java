package com.winrisk.game;

import com.winrisk.game.ai.PlayerInterface;
import com.winrisk.game.ai.BalancedAI;
import com.winrisk.game.ai.BorderGuardAI;
import com.winrisk.game.ai.RandomAI;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Player;
import com.winrisk.gui.StartGame;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayTests {

    @Test
    public void headlessPlayCompletesWithConfiguredParams() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        params.setAiFactory(index -> new ConqueringAI());
        Player winner = new Play(params, 250).play();
        assertNotNull(winner);
    }

    @Test(expected = IllegalStateException.class)
    public void headlessPlayEnforcesTurnLimit() {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(2);
        new Play(params, 0).play();
    }

    @Test
    public void headlessPlayReportsDrawWhenNoWinnerWithinTurnLimit() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        // Players that never attack can never be eliminated, so the game cannot
        // end within the turn limit: this must be reported as a draw, not crash.
        params.setAiFactory(index -> new PassiveRecorderAI());

        Play.Result result = new Play(params, 5).playResult();

        assertTrue(result.isDraw());
        assertEquals(null, result.getWinner());
        assertTrue(result.getWinReason().contains("turn limit"));
    }

    @Test
    public void headlessPlayPrefersAggressiveAiWhenMixedWithPassiveOpponents() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(4);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());

        params.setAiFactory(index -> index == 0 ? new ConqueringAI() : new PassiveRecorderAI());

        Player winner = new Play(params, 500).play();

        assertNotNull(winner);
        assertTrue(winner.getPlayerInterface() instanceof ConqueringAI);
        assertTrue(PassiveRecorderAI.getCreatedCount() >= 3);
    }

    @Test
    public void headlessPlayRunsMultipleAiTurnsBeforeFinish() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());

        PacingConquerorAI.reset();
        params.setAiFactory(index -> new PacingConquerorAI());

        Player winner = new Play(params, 1500).play();

        assertNotNull(winner);
        assertTrue(winner.getPlayerInterface() instanceof PacingConquerorAI);
        assertTrue(PacingConquerorAI.getPhaseRecord().contains(GamePhase.ATTACK));
        assertTrue(PacingConquerorAI.getPhaseRecord().contains(GamePhase.MOVE));
        assertTrue(PacingConquerorAI.getPhaseRecord().stream().filter(phase -> phase == GamePhase.REINFORCE).count() > 3);
    }

    @Test
    public void headlessPlaySupportsNewAis() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        params.setAttackWithAll(true);
        params.setAiFactory(index -> index == 0 ? new BalancedAI() : new ConqueringAI());

        Player winner = new Play(params, 200).play();

        assertNotNull(winner);
        assertTrue(winner.getPlayerInterface() instanceof PlayerInterface);
    }

    @Test
    public void headlessArgumentParsingEnablesToggles() {
        StartGame.HeadlessConfig config = StartGame.buildHeadlessConfig(new String[]{
                "--headless-play",
                "--fog-of-war",
                "--skynet",
                "--attack-with-all",
                "--ai-players=4",
                "--max-turns=250"
        });

        Params params = config.getParams();
        assertTrue(config.isHeadlessPlay());
        assertTrue(params.isFogOfWar());
        assertTrue(params.isSkynetMode());
        assertTrue(params.isAttackWithAll());
        assertEquals(4, params.getAiPlayers());
        assertEquals(0, params.getHumanPlayers());
        assertEquals(250, config.getMaxTurns());
    }

    @Test
    public void headlessArgumentParsingHandlesNullArgsWithDefaults() {
        StartGame.HeadlessConfig config = StartGame.buildHeadlessConfig(null);

        Params params = config.getParams();
        assertFalse(config.isHeadlessPlay());
        assertEquals(0, params.getAiPlayers());
        assertEquals(1, params.getHumanPlayers());
        assertEquals(5000, config.getMaxTurns());
    }

    @Test
    public void headlessPlayInvokesRandomAndBorderGuardAis() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(4);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        params.setAttackWithAll(true);
        params.setAiFactory(index -> {
            if (index == 0) {
                return new BorderGuardAI();
            }
            if (index == 1) {
                return new RandomAI();
            }
            return new ConqueringAI();
        });

        Player winner = new Play(params, 200).play();

        assertNotNull(winner);
        assertTrue(winner.getPlayerInterface() instanceof PlayerInterface);
    }

    private static class PassiveRecorderAI implements PlayerInterface {

        private static final AtomicInteger created = new AtomicInteger();

        PassiveRecorderAI() {
            created.incrementAndGet();
        }

        @Override
        public void move(com.winrisk.game.view.Game game) {
        }

        @Override
        public void reinforce(com.winrisk.game.view.Game game) {
        }

        @Override
        public void attack(com.winrisk.game.view.Game game) {
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        static int getCreatedCount() {
            return created.get();
        }
    }

    private static class ConqueringAI implements PlayerInterface {

        @Override
        public void move(com.winrisk.game.view.Game game) {
            captureEverything(game);
        }

        @Override
        public void reinforce(com.winrisk.game.view.Game game) {
            captureEverything(game);
        }

        @Override
        public void attack(com.winrisk.game.view.Game game) {
            captureEverything(game);
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        private void captureEverything(com.winrisk.game.view.Game game) {
            Player player = game.getPlayer();
            game.getFields().forEach(field -> {
                field.setPlayer(player);
                if (field.getArmy() == 0) {
                    field.setArmy(1);
                }
            });
        }
    }

    private static class PacingConquerorAI implements PlayerInterface {

        private static final AtomicInteger turns = new AtomicInteger();
        private static final List<GamePhase> phases = Collections.synchronizedList(new ArrayList<>());
        private static boolean seenAttack = false;
        private static boolean seenMove = false;

        static void reset() {
            turns.set(0);
            phases.clear();
            seenAttack = false;
            seenMove = false;
        }

        static List<GamePhase> getPhaseRecord() {
            return phases;
        }

        @Override
        public void move(com.winrisk.game.view.Game game) {
            recordPhase(game);
            seenMove = true;
            maybeFinish(game, GamePhase.MOVE);
        }

        @Override
        public void reinforce(com.winrisk.game.view.Game game) {
            recordPhase(game);
            maybeFinish(game, GamePhase.REINFORCE);
        }

        @Override
        public void attack(com.winrisk.game.view.Game game) {
            recordPhase(game);
            seenAttack = true;
            maybeFinish(game, GamePhase.ATTACK);
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        private void recordPhase(com.winrisk.game.view.Game game) {
            phases.add(game.getPhase());
        }

        private void maybeFinish(com.winrisk.game.view.Game game, GamePhase phase) {
            // Allow several full rounds before domination so other AIs get turns.
            if (turns.incrementAndGet() < 15 || !seenAttack || !seenMove) {
                return;
            }
            Player player = game.getPlayer();
            game.getFields().forEach(field -> {
                field.setPlayer(player);
                if (phase == GamePhase.REINFORCE) {
                    field.setArmy(Math.max(field.getArmy(), 3));
                }
            });
        }
    }
}
