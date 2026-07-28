package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import com.winrisk.game.view.Viewable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Side panel next to the board canvas. For a running {@link Game} it shows the
 * phase, the current player's reinforcements, a player legend and an
 * "End Phase" button; for the map editor it shows the editing gestures and an
 * "Add Continent" button. All state is re-read in {@link #refresh()}.
 */
public class HudPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int PANEL_WIDTH = 250;

    private final Viewable viewable;
    private final Runnable onStateChanged;
    private final Game game;

    private JLabel phaseLabel;
    private JLabel reinforcementsLabel;
    private JLabel infoLabel;
    private final List<PlayerRow> playerRows = new ArrayList<>();

    HudPanel(Viewable viewable, Runnable onStateChanged) {
        this.viewable = viewable;
        this.onStateChanged = onStateChanged;
        this.game = viewable instanceof Game ? (Game) viewable : null;
        setBackground(UiTheme.SLATE);
        setBorder(new EmptyBorder(16, 14, 16, 14));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        if (game != null) {
            buildGameHud();
        } else {
            buildEditorHud();
        }
        refresh();
    }

    private void buildGameHud() {
        add(title("WinRisk"));
        add(muted("Mode: " + game.getParams().getGameMode().toCliValue()));
        add(Box.createVerticalStrut(12));

        phaseLabel = header("");
        add(phaseLabel);
        reinforcementsLabel = body("");
        add(reinforcementsLabel);
        add(Box.createVerticalStrut(12));

        add(header("Players"));
        add(Box.createVerticalStrut(4));
        for (int i = 0; i < game.getPlayers().size(); i++) {
            PlayerRow row = new PlayerRow(game.getPlayers().get(i), i);
            playerRows.add(row);
            add(row.component);
        }
        add(Box.createVerticalStrut(12));

        infoLabel = body("");
        add(infoLabel);
        add(Box.createVerticalGlue());

        add(actionButton("End Phase"));
        add(Box.createVerticalStrut(6));
        add(muted("Click: place 1  |  Drag: attack/move"));
        add(muted("Hold: place all reinforcements"));
    }

    private void buildEditorHud() {
        add(title("Map Editor"));
        add(Box.createVerticalStrut(12));
        add(header("Gestures"));
        add(muted("Click empty space: add territory"));
        add(muted("Drag between territories: toggle link"));
        add(muted("Click territory: cycle continent"));
        add(muted("Drag territory to empty: bonus -1"));
        add(muted("Drag empty to territory: bonus +1"));
        add(muted("Hold on territory: delete it"));
        add(Box.createVerticalGlue());
        add(actionButton("Add Continent"));
    }

    private JButton actionButton(String label) {
        JButton button = new JButton(label);
        button.setFont(UiTheme.HEADER_FONT);
        button.setBackground(UiTheme.ACCENT);
        button.setForeground(UiTheme.INK);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.addActionListener(event -> {
            viewable.onAction();
            refresh();
            if (onStateChanged != null) {
                onStateChanged.run();
            }
        });
        return button;
    }

    /**
     * Re-reads the game state and updates every label. Safe to call at any
     * time, including after the game has ended.
     */
    public void refresh() {
        if (game == null) {
            return;
        }
        boolean over = game.end();
        phaseLabel.setText(over ? "Game over" : "Phase: " + game.getPhase());
        reinforcementsLabel.setText("Reinforcements: "
                + game.getPlayer().getCurrentReinforcements());
        for (PlayerRow row : playerRows) {
            row.refresh();
        }
        infoLabel.setText(wrapped(infoText(over)));
        repaint();
    }

    private String infoText(boolean over) {
        if (over) {
            return "Winner: " + seatName(game.getPlayers().indexOf(game.getWinner()))
                    + " - " + game.getWinReason();
        }
        if (game.getParams().getGameMode() == GameMode.SECRET_MISSION
                && game.getPlayer().getMission() != null) {
            return "Mission: " + game.getPlayer().getMission().getDescription();
        }
        if (game.getParams().getGameMode() == GameMode.CAPITAL
                && game.getPlayer().getHeadquarters() != null) {
            return "Headquarters: " + game.getPlayer().getHeadquarters().getDisplayName();
        }
        return " ";
    }

    private String seatName(int index) {
        if (index < 0) {
            return "nobody";
        }
        Player player = game.getPlayers().get(index);
        return player.isNeutral() ? "Neutral" : "Player " + (index + 1);
    }

    private String wrapped(String text) {
        return "<html><body style='width:" + (PANEL_WIDTH - 40) + "px'>"
                + text + "</body></html>";
    }

    private JLabel title(String text) {
        return styled(text, UiTheme.TITLE_FONT, UiTheme.PARCHMENT);
    }

    private JLabel header(String text) {
        return styled(text, UiTheme.HEADER_FONT, UiTheme.TEXT_LIGHT);
    }

    private JLabel body(String text) {
        return styled(text, UiTheme.BODY_FONT, UiTheme.TEXT_LIGHT);
    }

    private JLabel muted(String text) {
        return styled(text, UiTheme.SMALL_FONT, UiTheme.TEXT_MUTED);
    }

    private JLabel styled(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * One legend row: colour swatch plus a live text label for a seat.
     */
    private final class PlayerRow {
        private final Player player;
        private final int index;
        private final JLabel label;
        final JComponent component;

        PlayerRow(Player player, int index) {
            this.player = player;
            this.index = index;
            this.label = body("");
            // BorderLayout keeps the label on one line: overflowing text clips
            // instead of wrapping below the row's fixed height and vanishing.
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            JPanel swatchHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            swatchHolder.setOpaque(false);
            swatchHolder.add(new Swatch(player));
            row.add(swatchHolder, BorderLayout.WEST);
            row.add(label, BorderLayout.CENTER);
            this.component = row;
        }

        void refresh() {
            int territories = player.getFieldState(game);
            boolean dead = player.isDead(game);
            boolean current = index == game.getCurPlayer();
            StringBuilder text = new StringBuilder(seatName(index));
            if (!player.isNeutral()) {
                text.append(player.getPlayerInterface().isInteractive() ? " (You)" : " (AI)");
            }
            text.append("  ").append(territories).append(" terr");
            if (!player.isNeutral()) {
                text.append(", ").append(player.getRiskCards().size()).append(" cards");
            }
            label.setText(text.toString());
            label.setForeground(dead ? UiTheme.TEXT_MUTED
                    : current ? UiTheme.ACCENT : UiTheme.TEXT_LIGHT);
        }
    }

    private static final class Swatch extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Player player;

        Swatch(Player player) {
            this.player = player;
            setPreferredSize(new Dimension(14, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(player.getColor());
            g.fillOval(0, 0, getWidth(), getHeight());
            g.setColor(UiTheme.INK);
            g.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
