package com.winrisk.gui;

import com.winrisk.game.HeadlessCli;
import com.winrisk.game.Play;
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

public class StartGame extends JFrame {

    public StartGame() {
        setTitle("WinRisk");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(380, 440);
        setLocationRelativeTo(null);
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
        contentPane.setBackground(UiTheme.SLATE);
        contentPane.setBorder(new EmptyBorder(30, 36, 30, 36));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("WinRisk");
        title.setFont(UiTheme.TITLE_FONT);
        title.setForeground(UiTheme.PARCHMENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("World domination, one turn at a time");
        subtitle.setFont(UiTheme.SMALL_FONT);
        subtitle.setForeground(UiTheme.TEXT_MUTED);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        contentPane.add(title);
        contentPane.add(Box.createVerticalStrut(4));
        contentPane.add(subtitle);
        contentPane.add(Box.createVerticalStrut(28));
        // The buttons stay direct children of the content pane.
        contentPane.add(menuButton("HOST GAME", hostGameAction));
        contentPane.add(Box.createVerticalStrut(10));
        contentPane.add(menuButton("NEW MAP", newMapAction));
        contentPane.add(Box.createVerticalStrut(10));
        contentPane.add(menuButton("LOAD MAP", loadMapAction));
        contentPane.add(Box.createVerticalStrut(10));
        contentPane.add(menuButton("LOAD GAME", loadGameAction));
        contentPane.add(Box.createVerticalGlue());
        return contentPane;
    }

    private static JButton menuButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setFont(UiTheme.HEADER_FONT);
        button.setBackground(UiTheme.SLATE_LIGHT);
        button.setForeground(UiTheme.TEXT_LIGHT);
        button.setFocusPainted(false);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.addActionListener(arg0 -> EventQueue.invokeLater(action));
        return button;
    }

    public static void main(String[] args) {
        HeadlessCli.launch(args,
                () -> EventQueue.invokeLater(() -> new StartGame().setVisible(true)),
                config -> System.out.println(new Play(config.getParams(), config.getMaxTurns())
                        .playResult()
                        .toReport()));
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

}
