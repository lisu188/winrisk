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
        frame = new JFrame("Host Game");
        frame.setSize(460, 560);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel();
        root.setBackground(UiTheme.SLATE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        frame.setContentPane(root);

        final JComboBox<MapChoice> mapBox = new JComboBox<>();

        final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
        modeBox.setSelectedItem(GameMode.CLASSIC);

        final JCheckBox skynetModeBox = new JCheckBox("Skynet Mode");
        final JCheckBox attackWithAllBox = new JCheckBox("Attack with All");
        final JCheckBox fogOfWarCkeckBox = new JCheckBox("Fog of War");
        final JCheckBox incrementalCardsBox = new JCheckBox("Incremental Card Values");
        final JCheckBox expandedManeuverBox = new JCheckBox("Expanded Maneuver");
        final JCheckBox attackCardRerollBox = new JCheckBox("Attack Card Reroll");
        final JCheckBox commanderDieBox = new JCheckBox("Commander Die");

        final JTextField seedField = new JTextField();
        seedField.setToolTipText("Random seed");

        final JSpinner aiPlayers = new JSpinner();
        aiPlayers.setModel(new SpinnerNumberModel(2, 1, 4, 1));
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

        JButton startButton = new JButton("START");
        startButton.setFont(UiTheme.HEADER_FONT);
        startButton.setBackground(UiTheme.ACCENT);
        startButton.setForeground(UiTheme.INK);
        startButton.setFocusPainted(false);
        startButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
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
                        "Invalid file format.", JOptionPane.ERROR_MESSAGE);
            }

        });

        root.add(labeledSection("Board", mapBox));
        root.add(Box.createVerticalStrut(10));
        root.add(labeledSection("Mode", modeBox));
        root.add(Box.createVerticalStrut(10));
        root.add(labeledSection("AI Players", aiPlayers));
        root.add(Box.createVerticalStrut(10));
        root.add(rulesSection(skynetModeBox, attackWithAllBox, fogOfWarCkeckBox,
                incrementalCardsBox, expandedManeuverBox, attackCardRerollBox,
                commanderDieBox));
        root.add(Box.createVerticalStrut(10));
        root.add(labeledSection("Random Seed (optional)", seedField));
        root.add(Box.createVerticalGlue());
        root.add(startButton);

        for (MapChoice choice : getAvailableMaps(new File("maps"))) {
            mapBox.addItem(choice);
        }
    }

    /**
     * A labelled form row: a small header above the control. Static and
     * headless-safe so layout can be verified in tests.
     */
    static JPanel labeledSection(String label, JComponent control) {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setOpaque(false);
        JLabel header = new JLabel(label);
        header.setFont(UiTheme.SMALL_FONT);
        header.setForeground(UiTheme.TEXT_MUTED);
        section.add(header, BorderLayout.NORTH);
        section.add(control, BorderLayout.CENTER);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return section;
    }

    /**
     * The optional-rules group: a two-column grid of checkboxes under a
     * "Rules" header. Static and headless-safe for tests.
     */
    static JPanel rulesSection(JCheckBox... checkBoxes) {
        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 2));
        grid.setOpaque(false);
        for (JCheckBox checkBox : checkBoxes) {
            checkBox.setOpaque(false);
            checkBox.setFont(UiTheme.SMALL_FONT);
            checkBox.setForeground(UiTheme.TEXT_LIGHT);
            grid.add(checkBox);
        }
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setOpaque(false);
        JLabel header = new JLabel("Rules");
        header.setFont(UiTheme.SMALL_FONT);
        header.setForeground(UiTheme.TEXT_MUTED);
        section.add(header, BorderLayout.NORTH);
        section.add(grid, BorderLayout.CENTER);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        return section;
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
