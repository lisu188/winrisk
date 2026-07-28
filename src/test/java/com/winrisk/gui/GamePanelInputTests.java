package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Editor;
import com.winrisk.game.view.Game;
import org.junit.Test;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises {@link GamePanel} mouse input with controllable timestamps to
 * verify that the press timestamp is preserved into the ACTION_UP event, so
 * hold-to-act gestures (long press) reach the {@code FieldDetector} with a
 * real downTime.
 */
public class GamePanelInputTests {

    @Test
    public void longPressPlacesAllReinforcementsOnField() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createClassicGame();
            game.setPhase(GamePhase.REINFORCE);
            GamePanel panel = new GamePanel(game);
            panel.setSize(800, 600);

            Player player = game.getPlayer();
            Field field = player.getFields(game).get(0);
            player.setRein(3);
            int armyBefore = field.getArmy();

            pressAndRelease(panel, field.getPoint(), 600);

            assertEquals("long press should place all reinforcements",
                    armyBefore + 3, field.getArmy());
            assertEquals("long press should spend every reinforcement",
                    0, player.getCurrentReinforcements());
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void shortClickPlacesExactlyOneReinforcement() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createClassicGame();
            game.setPhase(GamePhase.REINFORCE);
            GamePanel panel = new GamePanel(game);
            panel.setSize(800, 600);

            Player player = game.getPlayer();
            Field field = player.getFields(game).get(0);
            player.setRein(3);
            int armyBefore = field.getArmy();

            pressAndRelease(panel, field.getPoint(), 100);

            assertEquals("short click should place exactly one troop",
                    armyBefore + 1, field.getArmy());
            assertEquals("short click should spend exactly one reinforcement",
                    2, player.getCurrentReinforcements());
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void editorLongPressDeletesTerritory() {
        String originalHeadless = forceHeadless();
        try {
            Map map = new Map();
            map.addField(new PointF(200, 200));
            Editor editor = new Editor(map);
            GamePanel panel = new GamePanel(editor);
            panel.setSize(800, 600);

            pressAndRelease(panel, new PointF(200, 200), 600);

            assertTrue("editor long press should delete the territory",
                    map.getFields().isEmpty());
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    private static Game createClassicGame() {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setGameMode(GameMode.CLASSIC);
        params.setRandomSeed(1234L);
        return new Game(params);
    }

    private static void pressAndRelease(GamePanel panel, PointF boardPoint, long holdMillis) {
        Point at = panel.boardToScreen(boardPoint.x, boardPoint.y);
        long pressTime = System.currentTimeMillis();
        MouseEvent press = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                pressTime, 0, at.x, at.y, 1, false, MouseEvent.BUTTON1);
        MouseEvent release = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
                pressTime + holdMillis, 0, at.x, at.y, 1, false, MouseEvent.BUTTON1);
        for (MouseListener listener : panel.getMouseListeners()) {
            listener.mousePressed(press);
            listener.mouseReleased(release);
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
