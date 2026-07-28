package com.winrisk.game;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.BuiltinMaps;

import java.util.function.Consumer;

/**
 * Command-line entry point and argument parsing for headless play. Extracted
 * from the former Swing launcher so simulations work without any UI layer:
 * {@code --headless-play} plus optional {@code --map=}, {@code --mode=},
 * {@code --ai-players=}, {@code --seed=}, {@code --max-turns=} and rule flags.
 */
public final class HeadlessCli {

    private HeadlessCli() {
    }

    public static void main(String[] args) {
        launch(args,
                () -> System.out.println(usage()),
                config -> System.out.println(new Play(config.getParams(), config.getMaxTurns())
                        .playResult()
                        .toReport()));
    }

    static String usage() {
        return "WinRisk headless simulator. Pass --headless-play with optional flags:\n"
                + "  --map=<builtin name or file path>  --mode=<classic|secret|capital>\n"
                + "  --ai-players=<1..5>  --seed=<long>  --max-turns=<n>\n"
                + "  --fog-of-war --skynet --attack-with-all --incremental-cards\n"
                + "  --expanded-maneuver --attack-card-reroll --commander-die";
    }

    public static void launch(String[] args,
                              Runnable uiLauncher,
                              Consumer<HeadlessConfig> headlessLauncher) {
        HeadlessConfig config = buildHeadlessConfig(args);
        if (config.isHeadlessPlay()) {
            headlessLauncher.accept(config);
            return;
        }
        uiLauncher.run();
    }

    public static HeadlessConfig buildHeadlessConfig(String[] args) {
        HeadlessConfig config = new HeadlessConfig();
        if (args == null) {
            return config;
        }
        for (String arg : args) {
            if ("--headless-play".equalsIgnoreCase(arg)
                    || "headless-play".equalsIgnoreCase(arg)) {
                config.setHeadlessPlay(true);
            } else if ("--fog-of-war".equalsIgnoreCase(arg)) {
                config.getParams().setFogOfWar(true);
            } else if ("--skynet".equalsIgnoreCase(arg)) {
                config.getParams().setSkynetMode(true);
            } else if ("--attack-with-all".equalsIgnoreCase(arg)) {
                config.getParams().setAttackWithAll(true);
            } else if ("--incremental-cards".equalsIgnoreCase(arg)) {
                config.getParams().getRulesOptions().setIncrementalCardSetValues(true);
            } else if ("--expanded-maneuver".equalsIgnoreCase(arg)) {
                config.getParams().getRulesOptions().setExpandedManeuver(true);
            } else if ("--attack-card-reroll".equalsIgnoreCase(arg)) {
                config.getParams().getRulesOptions().setAttackCardReroll(true);
            } else if ("--commander-die".equalsIgnoreCase(arg)) {
                config.getParams().getRulesOptions().setCommanderDie(true);
            } else if (arg.startsWith("--ai-players=")) {
                config.getParams().setAiPlayers(parsePositiveInt(arg, "--ai-players"));
            } else if (arg.startsWith("--max-turns=")) {
                config.setMaxTurns(parsePositiveInt(arg, "--max-turns"));
            } else if (arg.startsWith("--map=")) {
                String value = parseRequiredValue(arg, "--map");
                if (BuiltinMaps.isBuiltin(value)) {
                    config.getParams().setBuiltinMap(value);
                } else {
                    config.getParams().setMap(value);
                }
            } else if (arg.startsWith("--mode=")) {
                config.getParams().setGameMode(GameMode.fromCli(parseRequiredValue(arg, "--mode")));
            } else if (arg.startsWith("--seed=")) {
                config.getParams().setRandomSeed(parseLong(arg, "--seed"));
            }
        }
        if (config.getParams().getAiPlayers() == 0) {
            config.getParams().setAiPlayers(3);
        }
        config.getParams().setHumanPlayers(0);
        return config;
    }

    private static String parseRequiredValue(String arg, String option) {
        String value = arg.substring((option + "=").length());
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return value;
    }

    private static int parsePositiveInt(String arg, String option) {
        String value = parseRequiredValue(arg, option);
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(option + " must be greater than zero");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " requires a positive integer: " + value, e);
        }
    }

    private static long parseLong(String arg, String option) {
        String value = parseRequiredValue(arg, option);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " requires an integer seed: " + value, e);
        }
    }

    public static class HeadlessConfig {
        private final Params params = new Params();
        private boolean headlessPlay;
        private int maxTurns = 5000;

        public boolean isHeadlessPlay() {
            return headlessPlay;
        }

        void setHeadlessPlay(boolean headlessPlay) {
            this.headlessPlay = headlessPlay;
        }

        public Params getParams() {
            return params;
        }

        public int getMaxTurns() {
            return maxTurns;
        }

        void setMaxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
        }
    }
}
