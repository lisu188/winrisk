package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.BuiltinMaps;
import com.winrisk.game.view.Game;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        frame.setBounds(100, 100, 320, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));

        final JComboBox<MapChoice> mapBox = new JComboBox<>();
        frame.getContentPane().add(mapBox);

        final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
        modeBox.setSelectedItem(GameMode.CLASSIC);
        frame.getContentPane().add(modeBox);

        final JCheckBox skynetModeBox = new JCheckBox("Skynet Mode");
        frame.getContentPane().add(skynetModeBox);

        final JCheckBox attackWithAllBox = new JCheckBox("Attack with All");
        frame.getContentPane().add(attackWithAllBox);

        final JCheckBox fogOfWarCkeckBox = new JCheckBox("Fog of War");
        frame.getContentPane().add(fogOfWarCkeckBox);

        final JCheckBox incrementalCardsBox = new JCheckBox("Incremental Card Values");
        frame.getContentPane().add(incrementalCardsBox);

        final JCheckBox expandedManeuverBox = new JCheckBox("Expanded Maneuver");
        frame.getContentPane().add(expandedManeuverBox);

        final JCheckBox attackCardRerollBox = new JCheckBox("Attack Card Reroll");
        frame.getContentPane().add(attackCardRerollBox);

        final JCheckBox commanderDieBox = new JCheckBox("Commander Die");
        frame.getContentPane().add(commanderDieBox);

        final JTextField seedField = new JTextField();
        seedField.setToolTipText("Random seed");
        frame.getContentPane().add(seedField);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel);
        panel.setLayout(new GridLayout(0, 2, 0, 0));

        JPanel panel_1 = new JPanel();
        frame.getContentPane().add(panel_1);
        panel_1.setLayout(new GridLayout(0, 2, 0, 0));

        final JSpinner aiPlayers = new JSpinner();
        aiPlayers.setModel(new SpinnerNumberModel(2, 1, 4, 1));
        panel_1.add(aiPlayers);
        aiPlayers.setToolTipText("AI Players");
        modeBox.addActionListener(event -> {
            GameMode mode = (GameMode) modeBox.getSelectedItem();
            int minimumAi = mode == GameMode.CLASSIC ? 1 : 2;
            SpinnerNumberModel model = (SpinnerNumberModel) aiPlayers.getModel();
            model.setMinimum(minimumAi);
            if ((int) aiPlayers.getValue() < minimumAi) {
                aiPlayers.setValue(minimumAi);
            }
        });

        JLabel lblAiPlayers = new JLabel("AI Players");
        lblAiPlayers.setHorizontalAlignment(SwingConstants.CENTER);
        panel_1.add(lblAiPlayers);

        JButton startButton = new JButton("START");
        startButton.addActionListener(arg0 -> {
            try {
                Params params = new Params();
                params.setAiPlayers((int) aiPlayers.getValue());
                params.setGameMode((GameMode) modeBox.getSelectedItem());
                params.setFogOfWar(fogOfWarCkeckBox.isSelected());
                params.setAttackWithAll(attackWithAllBox.isSelected());
                params.setSkynetMode(skynetModeBox.isSelected());
                params.getRulesOptions().setIncrementalCardSetValues(incrementalCardsBox.isSelected());
                params.getRulesOptions().setExpandedManeuver(expandedManeuverBox.isSelected());
                params.getRulesOptions().setAttackCardReroll(attackCardRerollBox.isSelected());
                params.getRulesOptions().setCommanderDie(commanderDieBox.isSelected());
                if (!seedField.getText().trim().isEmpty()) {
                    params.setRandomSeed(Long.parseLong(seedField.getText().trim()));
                }
                ((MapChoice) mapBox.getSelectedItem()).applyTo(params);
                new GamePanel(new Game(params));
                frame.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(HostGameWindow.this.frame,
                        "Chosen map file was not a correct map file: " + e,
                        "Inalid file format.", JOptionPane.ERROR_MESSAGE);
            }

        });
        frame.getContentPane().add(startButton);

        for (MapChoice choice : getAvailableMaps(new File("maps"))) {
            mapBox.addItem(choice);
        }
    }

    static List<MapChoice> getAvailableMaps(File folder) {
        List<MapChoice> maps = new ArrayList<>();
        for (String name : BuiltinMaps.names()) {
            maps.add(MapChoice.builtin(name));
        }
        File[] customMaps = folder == null ? null : folder.listFiles();
        if (customMaps != null) {
            for (File file : customMaps) {
                if (file.isFile()) {
                    maps.add(MapChoice.file(file));
                }
            }
        }
        return maps;
    }

    /**
     * A selectable entry in the map picker: either one of the built-in boards
     * or a custom map file from the {@code maps/} folder.
     */
    static final class MapChoice {
        private final String builtinName;
        private final File file;

        private MapChoice(String builtinName, File file) {
            this.builtinName = builtinName;
            this.file = file;
        }

        static MapChoice builtin(String name) {
            return new MapChoice(name, null);
        }

        static MapChoice file(File file) {
            return new MapChoice(null, file);
        }

        boolean isBuiltin() {
            return builtinName != null;
        }

        String getBuiltinName() {
            return builtinName;
        }

        File getFile() {
            return file;
        }

        void applyTo(Params params) {
            if (builtinName != null) {
                params.setBuiltinMap(builtinName);
            } else {
                params.setMap(file.getAbsolutePath());
            }
        }

        @Override
        public String toString() {
            if (builtinName != null) {
                return Character.toUpperCase(builtinName.charAt(0)) + builtinName.substring(1);
            }
            return file.getName();
        }
    }

    public void setVisible(boolean arg0) {
        frame.setVisible(arg0);
    }

}
