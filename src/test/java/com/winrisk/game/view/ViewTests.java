package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.ai.ContinentAI;
import com.winrisk.game.ai.EasyAI;
import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.data.Params;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

public class ViewTests {

    @Test
    public void gameHandlersExtended() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        game.onFieldDrag(a, b);
        game.onShortClick(a);
        game.onLongClick(a);
        game.onFromField(a);
        game.next();
        game.onFieldDrag(a, b);
        game.next();
        game.onFieldDrag(a, b);
        game.next();
    }

    @Test
    public void gameHandlers() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        game.onFieldDrag(a, b);
        game.onFromField(a);
        game.onLongClick(a);
        game.onShortClick(a);
        game.onAction();
    }

    @Test
    public void gameMiscMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        game.setMap(new Map());
        game.setPhase(GamePhase.ATTACK);
        PlayerList list = new PlayerList();
        list.add(game.getPlayers().get(0));
        game.setPlayers(list);
        game.getParams();
        game.getMap();
        game.getPlayer();
        game.getMapSketch();
        game.setParams(game.getParams());
        game.end();
        game.next();
    }

    @Test
    public void gameAi128() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(5);
        params.setAiFactory(i -> new ViewConqueringAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void gameContinental() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(5);
        params.setAiFactory(i -> new ContinentAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void gameEasy() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(5);
        params.setAiFactory(i -> new EasyAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    private static class ViewConqueringAI implements com.winrisk.game.ai.PlayerInterface {
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
            com.winrisk.game.object.Player player = game.getPlayer();
            game.getFields().forEach(field -> {
                field.setPlayer(player);
                if (field.getArmy() == 0) {
                    field.setArmy(1);
                }
            });
        }
    }
}
