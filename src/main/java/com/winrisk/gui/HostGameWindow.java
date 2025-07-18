package com.winrisk.gui;

import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.view.Game;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class HostGameWindow {

    private JFrame frame;

    /**
     * Create the application.
     */
    public HostGameWindow() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        frame = new JFrame();
        frame.setBounds(100, 100, 246, 180);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));

        final JComboBox<File> mapBox = new JComboBox<>();
        frame.getContentPane().add(mapBox);

        final JCheckBox skynetModeBox = new JCheckBox("Skynet Mode");
        frame.getContentPane().add(skynetModeBox);

        final JCheckBox attackWithAllBox = new JCheckBox("Attack with All");
        frame.getContentPane().add(attackWithAllBox);

        final JCheckBox fogOfWarCkeckBox = new JCheckBox("Fog of War");
        frame.getContentPane().add(fogOfWarCkeckBox);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel);
        panel.setLayout(new GridLayout(0, 2, 0, 0));

        JPanel panel_1 = new JPanel();
        frame.getContentPane().add(panel_1);
        panel_1.setLayout(new GridLayout(0, 2, 0, 0));

        final JSpinner aiPlayers = new JSpinner();
        panel_1.add(aiPlayers);
        aiPlayers.setToolTipText("AI Players");

        JLabel lblAiPlayers = new JLabel("AI Players");
        lblAiPlayers.setHorizontalAlignment(SwingConstants.CENTER);
        panel_1.add(lblAiPlayers);

        JButton startButton = new JButton("START");
        startButton.addActionListener(arg0 -> {
            try {
                Params params = new Params();
                params.setAiPlayers((int) aiPlayers.getValue());
                params.setFogOfWar(fogOfWarCkeckBox.isSelected());
                params.setAttackWithAll(attackWithAllBox.isSelected());
                params.setSkynetMode(skynetModeBox.isSelected());
                params.setMap(((File) mapBox.getSelectedItem())
                        .getAbsolutePath());
                new GamePanel(new Game(params));
                frame.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(HostGameWindow.this.frame,
                        "Chosen map file was not a correct map file: " + e,
                        "Inalid file format.", JOptionPane.ERROR_MESSAGE);
            }

        });
        frame.getContentPane().add(startButton);

        String path = "maps";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        try {
            mapBox.addItem(new File(Map.class.getResource("world.map").toURI()) {
                @Override
                public String toString() {
                    return this.getName();
                }

            });
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    mapBox.addItem(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVisible(boolean arg0) {
        frame.setVisible(arg0);
    }

}
