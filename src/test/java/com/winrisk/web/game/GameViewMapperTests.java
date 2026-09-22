package com.winrisk.web.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
import com.winrisk.game.view.Game;
import com.winrisk.web.api.dto.GameView;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameViewMapperTests {

    private Game game(GameMode mode, int humans, int ai, long seed) {
        Params params = new Params();
        params.setHumanPlayers(humans);
        params.setAiPlayers(ai);
        params.setGameMode(mode);
        params.setRandomSeed(seed);
        return new Game(params);
    }

    @Test
    public void viewCarriesBoardStateWithoutSecretsOrImages() throws Exception {
        Game game = game(GameMode.SECRET_MISSION, 1, 3, 42L);
        GameView view = GameViewMapper.map("g1", 7, 0, game);

        assertEquals("g1", view.id);
        assertEquals(7, view.version);
        assertEquals(0, view.humanPlayerIndex);
        assertEquals(42, view.fields.size());
        assertEquals(6, view.continents.size());
        assertEquals(4, view.players.size());
        assertTrue(view.hasBackgroundImage);
        assertNotNull(view.missionText);

        String json = new ObjectMapper().writeValueAsString(view);
        assertTrue("view JSON must stay small (was " + json.length() + ")",
                json.length() < 50_000);
        assertFalse("no image bytes in the view", json.contains("\"image\""));
        assertFalse("no deck contents in the view", json.contains("drawPile"));
        assertFalse("no deck contents in the view", json.contains("discardPile"));
    }

    @Test
    public void linksAreDedupedAndOrdered() {
        Game game = game(GameMode.CLASSIC, 1, 2, 1L);
        GameView view = GameViewMapper.map("g", 0, 0, game);

        long edgeCount = game.getFields().stream()
                .mapToLong(field -> field.getNext().size())
                .sum() / 2;
        assertEquals(edgeCount, view.links.size());
        for (int[] link : view.links) {
            assertTrue(link[0] < link[1]);
        }
    }

    @Test
    public void ownersAndArmiesMatchTheEngine() {
        Game game = game(GameMode.CLASSIC, 1, 3, 5L);
        GameView view = GameViewMapper.map("g", 0, 0, game);
        for (GameView.FieldView fv : view.fields) {
            assertEquals(game.getFields().get(fv.index).getArmy(), fv.army);
            assertEquals(game.getPlayers().indexOf(
                    game.getFields().get(fv.index).getPlayer()), fv.ownerIndex);
        }
        int territorySum = view.players.stream().mapToInt(p -> p.territories).sum();
        assertEquals(42, territorySum);
    }

    @Test
    public void fogOfWarHidesFieldsOutsideHumanVisibility() {
        Params params = new Params();
        params.setHumanPlayers(1);
        params.setAiPlayers(3);
        params.setGameMode(GameMode.CLASSIC);
        params.setFogOfWar(true);
        params.setRandomSeed(9L);
        Game game = new Game(params);

        GameView view = GameViewMapper.map("g", 0, 0, game);
        int visible = game.getPlayers().get(0).getVis(game).size();
        assertEquals(visible, view.fields.size());
        assertTrue(view.fields.size() < 42);
    }

    @Test
    public void capitalModeExposesHumanHeadquarters() {
        Game game = game(GameMode.CAPITAL, 1, 3, 3L);
        GameView view = GameViewMapper.map("g", 0, 0, game);
        assertNotNull(view.hqName);
        assertTrue(view.hqIndex >= 0);
    }

    @Test
    public void spectatorViewHasNoHandOrMission() {
        Game game = game(GameMode.SECRET_MISSION, 0, 3, 8L);
        GameView view = GameViewMapper.map("g", 0, -1, game);
        assertTrue(view.hand.isEmpty());
        assertNull(view.missionText);
    }
}
