package com.winrisk.web.game;

import com.winrisk.game.data.GamePhase;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.CardSymbol;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.web.api.dto.AttackResponse;
import com.winrisk.web.api.dto.CreateGameRequest;
import com.winrisk.web.api.dto.GameView;
import com.winrisk.web.api.dto.TradeSetView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class GameServiceTests {

    private ScheduledExecutorService scheduler;
    private GameService service;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("winrisk-web-test").toFile();
        scheduler = Executors.newScheduledThreadPool(1);
        // step delay 1ms so AI turns resolve quickly in tests
        AiStepper stepper = new AiStepper(scheduler, 1);
        SaveStore store = new SaveStore(new File(tempDir, "saves").getPath(),
                new File(tempDir, "maps").getPath());
        service = new GameService(stepper, store, null, 24);
    }

    @After
    public void tearDown() {
        scheduler.shutdownNow();
    }

    private CreateGameRequest classicRequest(long seed) {
        CreateGameRequest request = new CreateGameRequest();
        request.mode = "classic";
        request.aiPlayers = 3;
        request.seed = seed;
        CreateGameRequest.MapChoice map = new CreateGameRequest.MapChoice();
        map.builtin = "world";
        request.map = map;
        return request;
    }

    private GameView awaitHumanTurn(String id) throws Exception {
        for (int i = 0; i < 3000; i++) {
            GameView view = service.view(id);
            if (view.winnerIndex >= 0
                    || view.currentPlayerIndex == view.humanPlayerIndex) {
                return view;
            }
            Thread.sleep(2);
        }
        fail("AI turns did not hand control back to the human");
        return null;
    }

    @Test
    public void createHostsAGameWithAHumanSeat() throws Exception {
        GameView view = service.create(classicRequest(42L));
        assertNotNull(view.id);
        assertEquals(0, view.humanPlayerIndex);
        assertEquals(4, view.players.size());
        assertFalse(view.players.get(0).ai);
        assertTrue(view.players.get(1).ai);
        assertEquals(1, service.sessionCount());
        awaitHumanTurn(view.id);
    }

    @Test
    public void placeReinforcesOwnTerritoryAndRejectsForeign() throws Exception {
        GameView view = service.create(classicRequest(7L));
        view = awaitHumanTurn(view.id);
        assertEquals("REINFORCE", view.phase);
        int reinforcements = view.reinforcements;
        assertTrue(reinforcements > 0);

        int own = ownField(view);
        GameView after = service.place(view.id, own, false);
        assertEquals(reinforcements - 1, after.reinforcements);

        int foreign = foreignField(after);
        try {
            service.place(view.id, foreign, false);
            fail("expected IllegalActionException");
        } catch (IllegalActionException expected) {
            assertTrue(expected.getMessage().contains("own territory"));
        }

        GameView all = service.place(view.id, own, true);
        assertEquals(0, all.reinforcements);
    }

    @Test
    public void attackRequiresAttackPhase() throws Exception {
        GameView view = service.create(classicRequest(11L));
        view = awaitHumanTurn(view.id);
        try {
            service.attack(view.id, 0, 1);
            fail("expected IllegalActionException");
        } catch (IllegalActionException expected) {
            assertTrue(expected.getMessage().contains("ATTACK"));
        }
    }

    @Test
    public void fullHumanTurnFlowsThroughPhases() throws Exception {
        GameView view = service.create(classicRequest(21L));
        view = awaitHumanTurn(view.id);
        service.place(view.id, ownField(view), true);

        GameView attackPhase = service.endPhase(view.id);
        assertEquals("ATTACK", attackPhase.phase);

        GameView movePhase = service.endPhase(view.id);
        assertEquals("MOVE", movePhase.phase);

        GameView after = service.endPhase(view.id);
        // After MOVE the turn passes on; the stepper runs the AI turns and
        // eventually control returns to the human.
        GameView back = awaitHumanTurn(view.id);
        assertTrue(back.version > after.version || back.winnerIndex >= 0);
    }

    @Test
    public void attackResolvesCombatAgainstAdjacentEnemy() throws Exception {
        GameView view = service.create(classicRequest(31L));
        view = awaitHumanTurn(view.id);
        service.place(view.id, ownField(view), true);
        service.endPhase(view.id);

        // Arm a field and attack an adjacent enemy directly via the engine
        // state to make the scenario deterministic.
        GameSession session = sessionOf(view.id);
        int[] pair = session.withLock(game -> {
            Player human = game.getPlayers().get(0);
            for (Field field : human.getFields(game)) {
                for (Field next : field.getNext()) {
                    if (next.getPlayer() != human) {
                        field.setArmy(10);
                        return new int[]{field.getFieldIndex(), next.getFieldIndex()};
                    }
                }
            }
            throw new IllegalStateException("no border found");
        });

        AttackResponse response = service.attack(view.id, pair[0], pair[1]);
        assertNotNull(response.combat);
        assertTrue(response.combat.attackerDice.length >= 1);
        assertTrue(response.combat.attackerLosses + response.combat.defenderLosses > 0
                || response.combat.captured);
        assertTrue(response.view.version > view.version);
    }

    @Test
    public void maneuverEnforcesSingleRouteRule() throws Exception {
        GameView view = service.create(classicRequest(51L));
        view = awaitHumanTurn(view.id);
        service.place(view.id, ownField(view), true);
        service.endPhase(view.id);
        GameView movePhase = service.endPhase(view.id);
        assertEquals("MOVE", movePhase.phase);

        GameSession session = sessionOf(view.id);
        int[] route = session.withLock(game -> {
            Player human = game.getPlayers().get(0);
            for (Field field : human.getFields(game)) {
                for (Field next : field.getNext()) {
                    if (next.getPlayer() == human) {
                        field.setArmy(5);
                        return new int[]{field.getFieldIndex(), next.getFieldIndex()};
                    }
                }
            }
            return null;
        });
        if (route == null) {
            return; // no connected pair this seed; nothing to verify
        }
        service.maneuver(view.id, route[0], route[1], 2);
        try {
            service.maneuver(view.id, route[1], route[0], 1);
            fail("expected IllegalActionException for second route");
        } catch (IllegalActionException expected) {
            assertTrue(expected.getMessage().contains("route"));
        }
    }

    @Test
    public void tradeExchangesASeededSet() throws Exception {
        GameView view = service.create(classicRequest(61L));
        view = awaitHumanTurn(view.id);

        GameSession session = sessionOf(view.id);
        session.withLock(game -> {
            Player human = game.getPlayers().get(0);
            Field any = game.getFields().get(0);
            human.addCard(RiskCard.territory(cardField(game, 0, CardSymbol.INFANTRY)));
            human.addCard(RiskCard.territory(cardField(game, 1, CardSymbol.INFANTRY)));
            human.addCard(RiskCard.territory(cardField(game, 2, CardSymbol.INFANTRY)));
            return any;
        });

        List<TradeSetView> sets = service.tradeSets(view.id);
        assertFalse(sets.isEmpty());
        int before = service.view(view.id).reinforcements;
        GameView after = service.trade(view.id, sets.get(0).cardIndexes);
        assertTrue(after.reinforcements > before);
        assertEquals(0, after.hand.size());
    }

    @Test
    public void saveAndLoadRoundTripsThroughTheStore() throws Exception {
        GameView view = service.create(classicRequest(71L));
        awaitHumanTurn(view.id);
        service.save(view.id, "webtest");

        GameView loaded = service.load("webtest");
        assertNotEquals(view.id, loaded.id);
        assertEquals(view.players.size(), loaded.players.size());
        assertEquals(view.fields.size(), loaded.fields.size());
        assertEquals(2, service.sessionCount());
    }

    @Test
    public void concurrentPlacementsSerializeUnderTheSessionLock() throws Exception {
        GameView view = service.create(classicRequest(81L));
        GameView ready = awaitHumanTurn(view.id);
        int own = ownField(ready);
        int reinforcements = ready.reinforcements;
        int armyBefore = ready.fields.stream()
                .filter(field -> field.index == own).findFirst().orElseThrow().army;

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Runnable task = () -> {
            try {
                start.await();
                for (int i = 0; i < reinforcements; i++) {
                    try {
                        service.place(view.id, own, false);
                    } catch (IllegalActionException ignored) {
                        return;
                    }
                }
            } catch (InterruptedException ignored) {
            }
        };
        pool.submit(task);
        pool.submit(task);
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        // The lock must prevent double-spending: the army gained on the field
        // equals exactly the reinforcements that were available. (Placing with
        // an empty pool is a silent no-op in the engine, so counting calls
        // proves nothing - the army delta is the real invariant.)
        GameView after = service.view(view.id);
        int armyAfter = after.fields.stream()
                .filter(field -> field.index == own).findFirst().orElseThrow().army;
        assertEquals(0, after.reinforcements);
        assertEquals(reinforcements, armyAfter - armyBefore);
    }

    @Test
    public void deleteRemovesTheSession() {
        GameView view = service.create(classicRequest(91L));
        service.delete(view.id);
        assertEquals(0, service.sessionCount());
        try {
            service.view(view.id);
            fail("expected GameNotFoundException");
        } catch (GameNotFoundException expected) {
        }
    }

    private GameSession sessionOf(String id) {
        // The service does not expose sessions; reach it via a mutation hook.
        // Reflection keeps the production surface minimal.
        try {
            java.lang.reflect.Field field = GameService.class.getDeclaredField("sessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, GameSession> map =
                    (java.util.Map<String, GameSession>) field.get(service);
            return map.get(id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Field cardField(com.winrisk.game.view.Game game, int index, CardSymbol symbol) {
        Field field = game.getFields().get(index);
        field.setCardSymbol(symbol);
        return field;
    }

    private int ownField(GameView view) {
        return view.fields.stream()
                .filter(field -> field.ownerIndex == view.humanPlayerIndex)
                .findFirst()
                .orElseThrow()
                .index;
    }

    private int foreignField(GameView view) {
        return view.fields.stream()
                .filter(field -> field.ownerIndex != view.humanPlayerIndex)
                .findFirst()
                .orElseThrow()
                .index;
    }
}
