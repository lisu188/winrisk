package com.winrisk.gui;

import com.winrisk.game.Play;
import com.winrisk.game.data.GameMode;
import com.winrisk.game.map.BuiltinMaps;
import com.winrisk.game.map.Map;
import com.winrisk.game.view.Editor;
import com.winrisk.game.view.Game;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class StartGame extends JFrame {

    public StartGame() {
        setTitle("WinRisk");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 200, 200);
        setContentPane(createMenuPanel(
                () -> {
                    new HostGameWindow().setVisible(true);
                    StartGame.this.dispose();
                },
                this::handleNewMapAction,
                this::handleLoadMapAction,
                this::handleLoadGameAction));
    }

    static JPanel createMenuPanel(Runnable hostGameAction,
                                  Runnable newMapAction,
                                  Runnable loadMapAction,
                                  Runnable loadGameAction) {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new GridLayout(0, 1, 0, 0));

        JButton buttonHostGame = new JButton("HOST GAME");
        JButton btnNewMap = new JButton("NEW MAP");
        JButton button = new JButton("LOAD MAP");
        JButton btnLoadGame = new JButton("LOAD GAME");

        buttonHostGame.addActionListener(arg0 -> EventQueue.invokeLater(hostGameAction));

        btnNewMap.addActionListener(arg0 -> EventQueue.invokeLater(newMapAction));

        button.addActionListener(arg0 -> EventQueue.invokeLater(loadMapAction));

        btnLoadGame.addActionListener(arg0 -> EventQueue.invokeLater(loadGameAction));

        contentPane.add(buttonHostGame);
        contentPane.add(btnNewMap);
        contentPane.add(button);
        contentPane.add(btnLoadGame);
        return contentPane;
    }

    public static void main(String[] args) {
        launch(args,
                () -> EventQueue.invokeLater(() -> new StartGame().setVisible(true)),
                config -> System.out.println(new Play(config.getParams(), config.getMaxTurns())
                        .playResult()
                        .toReport()));
    }

    static void launch(String[] args,
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

    private Map createEmptyMap(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        Map map = new Map();
        map.setImage(bos.toByteArray());
        return map;
    }

    void handleNewMapAction() {
        try {
            String path = getFilePath();
            if (path == null) {
                return;
            }
            Map map = createEmptyMap(path);
            new GamePanel(new Editor(map));
        } catch (Exception e) {
            handleException("Error loading background image.",
                    "Invalid file format.", e);
        }
    }

    void handleLoadMapAction() {
        try {
            final String file = getFilePath();
            if (file == null) {
                return;
            }
            new GamePanel(new Editor(file));
        } catch (Exception e) {
            handleException("Chosen map file was not a correct map file.",
                    "Invalid file format.", e);
        }
    }

    void handleLoadGameAction() {
        try {
            final String file = getFilePath();
            if (file == null) {
                return;
            }
            new GamePanel(Game.loadGame(file));
        } catch (Exception e) {
            handleException("Chosen file was not a valid saved game.",
                    "Invalid file format.", e);
        }
    }

    String getFilePath() {
        FileDialog dialog = createFileDialog();
        dialog.setDirectory("save");
        dialog.setVisible(true);
        return buildSelectedPath(dialog.getDirectory(), dialog.getFile());
    }

    FileDialog createFileDialog() {
        return new FileDialog(StartGame.this);
    }

    static String buildSelectedPath(String directory, String fileName) {
        if (directory == null || fileName == null) {
            return null;
        }
        return new File(directory, fileName).getAbsolutePath();
    }

    private void handleException(String text, String title, Exception e) {
        JOptionPane.showMessageDialog(StartGame.this,
                text + ": " + e.getClass().getSimpleName(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    public static class HeadlessConfig {
        private final com.winrisk.game.data.Params params = new com.winrisk.game.data.Params();
        private boolean headlessPlay;
        private int maxTurns = 5000;

        public boolean isHeadlessPlay() {
            return headlessPlay;
        }

        void setHeadlessPlay(boolean headlessPlay) {
            this.headlessPlay = headlessPlay;
        }

        public com.winrisk.game.data.Params getParams() {
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
