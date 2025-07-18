package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.map.Map;
import org.junit.Test;

public class GameMethodsTest {
    @Test
    public void testMisc() throws Exception {
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
}
