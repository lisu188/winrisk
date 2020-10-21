package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.ai.ContinentAI;
import com.winrisk.game.ai.EasyAI;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

public class GameTest {
    @Test
    public void ai128() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(128);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void continental() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(8);
        params.setAiFactory(i -> new ContinentAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void easy() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(8);
        params.setAiFactory(i -> new EasyAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

}
