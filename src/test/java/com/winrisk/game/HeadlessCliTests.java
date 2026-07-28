package com.winrisk.game;

import com.winrisk.game.data.Params;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HeadlessCliTests {

    @Test
    public void buildHeadlessConfigParsesFlagsAndDefaults() {
        HeadlessCli.HeadlessConfig config = HeadlessCli.buildHeadlessConfig(new String[]{
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
    public void buildHeadlessConfigRejectsInvalidNumericOptions() {
        assertInvalidConfig("--ai-players=0");
        assertInvalidConfig("--ai-players=-1");
        assertInvalidConfig("--ai-players=two");
        assertInvalidConfig("--max-turns=0");
        assertInvalidConfig("--max-turns=-5");
        assertInvalidConfig("--max-turns=soon");
        assertInvalidConfig("--seed=");
        assertInvalidConfig("--seed=abc");
    }

    @Test
    public void buildHeadlessConfigResolvesBuiltinMapNames() {
        HeadlessCli.HeadlessConfig builtin = HeadlessCli.buildHeadlessConfig(new String[]{
                "--headless-play", "--map=Gondwana"});
        assertEquals("Gondwana", builtin.getParams().getBuiltinMap());
        assertEquals(26, builtin.getParams().loadMap().getFields().size());

        HeadlessCli.HeadlessConfig file = HeadlessCli.buildHeadlessConfig(new String[]{
                "--headless-play", "--map=maps/custom.map"});
        assertNull(file.getParams().getBuiltinMap());
    }

    @Test
    public void buildHeadlessConfigRejectsBlankRequiredValues() {
        assertInvalidConfig("--map=");
        assertInvalidConfig("--map=   ");
        assertInvalidConfig("--mode=");
    }

    @Test
    public void launchWithoutHeadlessFlagRunsUiBranchOnly() {
        AtomicBoolean uiLaunched = new AtomicBoolean(false);
        AtomicBoolean headlessLaunched = new AtomicBoolean(false);

        HeadlessCli.launch(new String[]{"--fog-of-war"},
                () -> uiLaunched.set(true),
                config -> headlessLaunched.set(true));

        assertTrue(uiLaunched.get());
        assertFalse(headlessLaunched.get());
    }

    @Test
    public void launchWithHeadlessFlagRunsHeadlessBranchOnlyWithDeterministicDefaults() {
        AtomicBoolean uiLaunched = new AtomicBoolean(false);
        AtomicReference<HeadlessCli.HeadlessConfig> captured = new AtomicReference<>();

        HeadlessCli.launch(new String[]{"--headless-play"},
                () -> uiLaunched.set(true),
                captured::set);

        assertFalse(uiLaunched.get());
        assertNotNull(captured.get());
        assertEquals(3, captured.get().getParams().getAiPlayers());
        assertEquals(0, captured.get().getParams().getHumanPlayers());
        assertEquals(5000, captured.get().getMaxTurns());
    }

    private void assertInvalidConfig(String arg) {
        try {
            HeadlessCli.buildHeadlessConfig(new String[]{arg});
            fail("Expected invalid headless config for " + arg);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
