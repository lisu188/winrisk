package com.winrisk.gui;

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
        new StartGame().setVisible(true);
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

}
