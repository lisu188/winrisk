package com.winrisk.game.serialization;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class GameSaveLoadTests {

    private Game startedGame(GameMode mode, int aiPlayers, long seed) throws Exception {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(aiPlayers);
        params.setGameMode(mode);
        params.setRandomSeed(seed);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        Game game = new Game(params);
        // Advance several turns so the snapshot has non-trivial state: conquests,
        // cards in hand, reinforcements and deck movement.
        for (int i = 0; i < 60 && !game.end(); i++) {
            game.next();
        }
        return game;
    }

    private void assertEquivalent(Game original, Game restored) {
        assertEquals(original.getFields().size(), restored.getFields().size());
        assertEquals(original.getPlayers().size(), restored.getPlayers().size());
        assertEquals(original.getCurPlayer(), restored.getCurPlayer());
        assertEquals(original.getPhase(), restored.getPhase());
        assertEquals(original.isCommanderDieUsed(), restored.isCommanderDieUsed());
        assertEquals(original.isManeuverUsed(), restored.isManeuverUsed());
        assertEquals(original.getCardService().getTradeCount(),
                restored.getCardService().getTradeCount());
        assertEquals(original.getCardService().getDeck().size(),
                restored.getCardService().getDeck().size());

        List<Field> a = original.getFields();
        List<Field> b = restored.getFields();
        for (int i = 0; i < a.size(); i++) {
            assertEquals("army at " + i, a.get(i).getArmy(), b.get(i).getArmy());
            Player pa = a.get(i).getPlayer();
            Player pb = b.get(i).getPlayer();
            assertEquals("owner presence at " + i, pa == null, pb == null);
            if (pa != null) {
                assertEquals("owner colour at " + i, pa.getColor(), pb.getColor());
            }
        }

        for (int i = 0; i < original.getPlayers().size(); i++) {
            Player pa = original.getPlayers().get(i);
            Player pb = restored.getPlayers().get(i);
            assertEquals(pa.getColor(), pb.getColor());
            assertEquals(pa.isNeutral(), pb.isNeutral());
            assertEquals(pa.getCurrentReinforcements(), pb.getCurrentReinforcements());
            assertEquals("card count for player " + i,
                    pa.getRiskCards().size(), pb.getRiskCards().size());
            assertEquals(cardHistogram(pa), cardHistogram(pb));
            assertEquals(pa.getMission() == null, pb.getMission() == null);
            if (pa.getMission() != null) {
                assertEquals(pa.getMission().getSpec().getKind(),
                        pb.getMission().getSpec().getKind());
            }
        }

        assertEquals(original.getNeutralPlayer() == null, restored.getNeutralPlayer() == null);
    }

    private HashMap<String, Integer> cardHistogram(Player player) {
        HashMap<String, Integer> counts = new HashMap<>();
        player.getRiskCards().forEach(card ->
                counts.merge(card.getFieldIndex() + ":" + card.getSymbol(), 1, Integer::sum));
        return counts;
    }

    @Test
    public void secretMissionGameRoundTripsInMemory() throws Exception {
        Game original = startedGame(GameMode.SECRET_MISSION, 4, 12345L);
        GameSketch sketch = new GameSketch(original);

        Game restored = new Game();
        restored.fromSketch(sketch);

        assertEquivalent(original, restored);
    }

    @Test
    public void gameRoundTripsThroughFile() throws Exception {
        Game original = startedGame(GameMode.SECRET_MISSION, 3, 999L);
        File tmp = File.createTempFile("game", ".dat");
        original.save(tmp.getAbsolutePath());

        Game restored = Game.loadGame(tmp.getAbsolutePath());
        assertEquivalent(original, restored);
        tmp.delete();
    }

    @Test
    public void classicTwoPlayerNeutralRoundTrips() throws Exception {
        Game original = startedGame(GameMode.CLASSIC, 2, 7L);
        assertNotNull("2-player classic should have a neutral player", original.getNeutralPlayer());

        GameSketch sketch = new GameSketch(original);
        Game restored = new Game();
        restored.fromSketch(sketch);

        assertEquivalent(original, restored);
        assertNotNull(restored.getNeutralPlayer());
        assertTrue(restored.getNeutralPlayer().isNeutral());
    }

    @Test
    public void capitalGameRoundTripsHeadquarters() throws Exception {
        Game original = startedGame(GameMode.CAPITAL, 3, 55L);
        GameSketch sketch = new GameSketch(original);
        Game restored = new Game();
        restored.fromSketch(sketch);

        assertEquivalent(original, restored);
        for (int i = 0; i < original.getPlayers().size(); i++) {
            Field originalHq = original.getPlayers().get(i).getHeadquarters();
            Field restoredHq = restored.getPlayers().get(i).getHeadquarters();
            assertEquals(originalHq == null, restoredHq == null);
            if (originalHq != null) {
                assertEquals(originalHq.getFieldIndex(), restoredHq.getFieldIndex());
            }
        }
    }

    @Test
    public void restoredGameKeepsPlayingWithoutError() throws Exception {
        Game original = startedGame(GameMode.SECRET_MISSION, 4, 321L);
        Game restored = Game.loadGame(saveToTemp(original));

        // A restored game must keep advancing without throwing. AI-vs-AI games
        // can stalemate, so completion is not guaranteed within a fixed bound;
        // we only require valid, error-free continuation.
        boolean ended = false;
        for (int i = 0; i < 1000; i++) {
            if (restored.end()) {
                ended = true;
                break;
            }
            restored.next();
        }
        if (ended) {
            assertNotNull(restored.getWinner());
        } else {
            // Still mid-game: the roster must remain intact and consistent.
            assertEquals(original.getPlayers().size(), restored.getPlayers().size());
        }
    }

    private String saveToTemp(Game game) throws Exception {
        File tmp = File.createTempFile("game", ".dat");
        tmp.deleteOnExit();
        game.save(tmp.getAbsolutePath());
        return tmp.getAbsolutePath();
    }
}
