package com.winrisk.gui;

import com.winrisk.game.data.Params;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class StartGameLaunchTests {

    @Test
    public void buildHeadlessConfigParsesFlagsAndDefaults() {
        StartGame.HeadlessConfig config = StartGame.buildHeadlessConfig(new String[]{
                "--headless-play",
                "--fog-of-war",
                "--skynet",
                "--attack-with-all",
                "--ai-players=4",
                "--max-turns=250",
                "--map=custom.map"
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
    public void launchWithoutHeadlessFlagRunsUiBranchOnly() {
        AtomicBoolean uiLaunched = new AtomicBoolean(false);
        AtomicBoolean headlessLaunched = new AtomicBoolean(false);

        StartGame.launch(new String[]{"--fog-of-war"},
                () -> uiLaunched.set(true),
                config -> headlessLaunched.set(true));

        assertTrue(uiLaunched.get());
        assertFalse(headlessLaunched.get());
    }

    @Test
    public void launchWithHeadlessFlagRunsHeadlessBranchOnlyWithDeterministicDefaults() {
        AtomicBoolean uiLaunched = new AtomicBoolean(false);
        AtomicReference<StartGame.HeadlessConfig> captured = new AtomicReference<>();

        StartGame.launch(new String[]{"--headless-play"},
                () -> uiLaunched.set(true),
                captured::set);

        assertFalse(uiLaunched.get());
        assertNotNull(captured.get());
        assertEquals(6, captured.get().getParams().getAiPlayers());
        assertEquals(0, captured.get().getParams().getHumanPlayers());
        assertEquals(5000, captured.get().getMaxTurns());
    }
}
