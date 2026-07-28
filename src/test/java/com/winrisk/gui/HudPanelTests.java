package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.mission.Mission;
import com.winrisk.game.view.Editor;
import com.winrisk.game.view.Game;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HudPanelTests {

    @Test
    public void legendListsEverySeatIncludingNeutral() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createGame(0, 2, GameMode.CLASSIC);
            assertEquals("classic two-player games add a neutral seat",
                    3, game.getPlayers().size());

            HudPanel hud = new HudPanel(game, null);

            List<String> seatRows = seatRows(hud);
            assertEquals("one legend row per seat", game.getPlayers().size(),
                    seatRows.size());
            assertTrue("legend should include the neutral seat: " + seatRows,
                    seatRows.stream().anyMatch(text -> text.contains("Neutral")));
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void reinforcementsLabelTracksCurrentPlayer() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createGame(0, 2, GameMode.CLASSIC);
            HudPanel hud = new HudPanel(game, null);

            game.getPlayer().setRein(7);
            hud.refresh();

            List<String> labels = collectLabelTexts(hud);
            assertTrue("expected 'Reinforcements: 7' among " + labels,
                    labels.stream().anyMatch(text -> text.contains("Reinforcements: 7")));
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void endPhaseButtonAdvancesGameState() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createGame(1, 1, GameMode.CLASSIC);
            HudPanel hud = new HudPanel(game, null);

            GamePhase phaseBefore = game.getPhase();
            int playerBefore = game.getCurPlayer();

            JButton endPhase = findButton(hud, "End Phase");
            assertNotNull("game HUD should contain an End Phase button", endPhase);
            fire(endPhase);

            assertTrue("clicking End Phase should change the (phase, player) pair",
                    phaseBefore != game.getPhase()
                            || playerBefore != game.getCurPlayer());
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void winnerIsShownWhenAMissionCompletes() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createGame(0, 3, GameMode.SECRET_MISSION);
            game.getPlayers().get(0).setMission(new Mission("Auto win", (g, p) -> true));
            for (int i = 1; i < game.getPlayers().size(); i++) {
                game.getPlayers().get(i).setMission(new Mission("Never", (g, p) -> false));
            }

            HudPanel hud = new HudPanel(game, null);
            hud.refresh();

            List<String> labels = collectLabelTexts(hud);
            assertTrue("expected a winner announcement among " + labels,
                    labels.stream().anyMatch(text -> text.contains("Winner")));
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void editorHudAddsContinents() {
        String originalHeadless = forceHeadless();
        try {
            Map map = new Map();
            Editor editor = new Editor(map);
            HudPanel hud = new HudPanel(editor, null);

            JButton addContinent = findButton(hud, "Add Continent");
            assertNotNull("editor HUD should contain an Add Continent button",
                    addContinent);

            int continentsBefore = map.getContinents().size();
            fire(addContinent);

            assertEquals("clicking Add Continent should add one continent",
                    continentsBefore + 1, map.getContinents().size());
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    private static Game createGame(int humanPlayers, int aiPlayers, GameMode mode) {
        Params params = new Params();
        params.setHumanPlayers(humanPlayers);
        params.setAiPlayers(aiPlayers);
        params.setGameMode(mode);
        params.setRandomSeed(1234L);
        return new Game(params);
    }

    /**
     * Legend rows are the labels showing a territory count, e.g.
     * "Player 1 (AI)  14 terr, 0 cards" or "Neutral  14 terr".
     */
    private static List<String> seatRows(Container container) {
        List<String> rows = new ArrayList<>();
        for (String text : collectLabelTexts(container)) {
            if (text.contains(" terr")) {
                rows.add(text);
            }
        }
        return rows;
    }

    private static List<String> collectLabelTexts(Container container) {
        List<String> labels = new ArrayList<>();
        collectLabelTexts(container, labels);
        return labels;
    }

    private static void collectLabelTexts(Container container, List<String> labels) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getText() != null) {
                labels.add(((JLabel) child).getText());
            }
            if (child instanceof Container) {
                collectLabelTexts((Container) child, labels);
            }
        }
    }

    private static JButton findButton(Container container, String label) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton
                    && label.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton found = findButton((Container) child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void fire(JButton button) {
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED,
                button.getText());
        for (ActionListener listener : button.getActionListeners()) {
            listener.actionPerformed(event);
        }
    }

    private static String forceHeadless() {
        String original = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
        return original;
    }

    private static void restoreHeadless(String original) {
        if (original == null) {
            System.clearProperty("java.awt.headless");
        } else {
            System.setProperty("java.awt.headless", original);
        }
    }
}
