package com.winrisk.gui;

import com.winrisk.game.Play;
import com.winrisk.game.map.Map;
import com.winrisk.game.view.Editor;

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
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new GridLayout(0, 1, 0, 0));

        JButton buttonHostGame = new JButton("HOST GAME");
        JButton btnNewMap = new JButton("NEW MAP");
        JButton button = new JButton("LOAD MAP");

        buttonHostGame.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
            new HostGameWindow().setVisible(true);
            StartGame.this.dispose();
        }));

        btnNewMap.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
            try {
                String path = getFilePath();
                Map map = createEmptyMap(path);
                new GamePanel(new Editor(map));
            } catch (Exception e) {
                handleException("Error loading background image.",
                        "Inalid file format.", e);
            }

        }));

        button.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
            try {
                final String file = getFilePath();
                if (file == null) {
                    return;
                }
                new GamePanel(new Editor(file));
            } catch (Exception e) {
                handleException("Chosen map file was not a correct map file.",
                        "Inalid file format.", e);
            }

        }));

        contentPane.add(buttonHostGame);
        contentPane.add(btnNewMap);
        contentPane.add(button);
    }

    public static void main(String[] args) {
        launch(args,
                () -> EventQueue.invokeLater(() -> new StartGame().setVisible(true)),
                config -> new Play(config.getParams(), config.getMaxTurns()).play());
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
            } else if (arg.startsWith("--ai-players=")) {
                config.getParams().setAiPlayers(Integer.parseInt(arg.replace("--ai-players=", "")));
            } else if (arg.startsWith("--max-turns=")) {
                config.setMaxTurns(Integer.parseInt(arg.replace("--max-turns=", "")));
            } else if (arg.startsWith("--map=")) {
                config.getParams().setMap(arg.replace("--map=", ""));
            }
        }
        if (config.getParams().getAiPlayers() == 0) {
            config.getParams().setAiPlayers(6);
        }
        config.getParams().setHumanPlayers(0);
        return config;
    }

    private Map createEmptyMap(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        Map map = new Map();
        map.setImage(bos.toByteArray());
        return map;
    }

    private String getFilePath() {
        FileDialog dialog = new FileDialog(StartGame.this);
        dialog.setDirectory("save");
        dialog.setVisible(true);
        return new File(dialog.getDirectory(), dialog.getFile())
                .getAbsolutePath();
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
