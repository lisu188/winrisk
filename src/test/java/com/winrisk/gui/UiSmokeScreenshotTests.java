package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiSmokeScreenshotTests {
    private static final File REPORT_DIR = new File("build/reports/ui-smoke");

    @Test
    public void writesStartMenuScreenshotForInspection() throws Exception {
        String originalHeadless = forceHeadless();
        try {
            JPanel menu = StartGame.createMenuPanel(() -> {
            }, () -> {
            }, () -> {
            });

            BufferedImage image = render(menu, 200, 200);

            assertButtonLabels(menu, "HOST GAME", "NEW MAP", "LOAD MAP");
            assertImageHasContent("start menu", image, 4);
            writePng(image, "start-menu.png");
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void writesGameBoardScreenshotForInspection() throws Exception {
        String originalHeadless = forceHeadless();
        try {
            Params params = new Params();
            params.setHumanPlayers(0);
            params.setAiPlayers(3);
            params.setGameMode(GameMode.CLASSIC);
            params.setRandomSeed(1234L);

            GamePanel panel = new GamePanel(new Game(params));
            BufferedImage image = render(panel, 800, 600);

            assertImageHasContent("game board", image, 16);
            writePng(image, "game-board.png");
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void gameplayRespondsToEmulatedPanelClicks() throws Exception {
        String originalHeadless = forceHeadless();
        try {
            Game game = createClassicGame();
            GamePanel panel = new GamePanel(game);
            panel.setSize(800, 600);

            Player attacker = game.getPlayer();
            Field reinforced = attacker.getFields(game).get(0);
            int armyBeforeReinforce = reinforced.getArmy();
            int reinforcementsBeforeClick = attacker.getCurrentReinforcements();

            click(panel, reinforced, MouseEvent.BUTTON1);

            assertTrue("test setup should start with reinforcements",
                    reinforcementsBeforeClick > 0);
            assertEquals("left-clicking an owned field should place one troop",
                    armyBeforeReinforce + 1, reinforced.getArmy());
            assertEquals("left-clicking should spend one reinforcement",
                    reinforcementsBeforeClick - 1, attacker.getCurrentReinforcements());

            Field source = findFieldWithNeighbor(game, attacker);
            Field target = source.getNext().get(0);
            Player defender = game.getPlayers().stream()
                    .filter(player -> player != attacker)
                    .filter(player -> !player.isNeutral())
                    .findFirst()
                    .orElseThrow(AssertionError::new);
            source.setPlayer(attacker);
            source.setArmy(4);
            target.setPlayer(defender);
            target.setArmy(1);
            game.setPhase(GamePhase.ATTACK);

            int totalArmiesBeforeAttack = source.getArmy() + target.getArmy();

            drag(panel, source, target);

            assertTrue("dragging from an owned field to an adjacent enemy should resolve combat",
                    source.getArmy() + target.getArmy() < totalArmiesBeforeAttack);

            BufferedImage image = render(panel, 800, 600);
            assertImageHasContent("gameplay click emulation", image, 16);
            writePng(image, "gameplay-click-emulation.png");
        } finally {
            restoreHeadless(originalHeadless);
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

    private static BufferedImage render(Component component, int width, int height) {
        component.setSize(width, height);
        layout(component);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        component.paint(graphics);
        graphics.dispose();
        return image;
    }

    private static Game createClassicGame() {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        params.setGameMode(GameMode.CLASSIC);
        params.setRandomSeed(1234L);
        return new Game(params);
    }

    private static Field findFieldWithNeighbor(Game game, Player player) {
        return player.getFields(game).stream()
                .filter(field -> !field.getNext().isEmpty())
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static void click(GamePanel panel, Field field, int button) {
        pressAndRelease(panel, field.getPoint().x, field.getPoint().y,
                field.getPoint().x, field.getPoint().y, button);
    }

    private static void drag(GamePanel panel, Field from, Field to) {
        pressAndRelease(panel, from.getPoint().x, from.getPoint().y,
                to.getPoint().x, to.getPoint().y, MouseEvent.BUTTON1);
    }

    private static void pressAndRelease(GamePanel panel,
                                        int pressX,
                                        int pressY,
                                        int releaseX,
                                        int releaseY,
                                        int button) {
        long now = System.currentTimeMillis();
        MouseEvent press = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                now, 0, pressX, pressY, 1, false, button);
        MouseEvent release = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
                now + 50, 0, releaseX, releaseY, 1, false, button);
        for (MouseListener listener : panel.getMouseListeners()) {
            listener.mousePressed(press);
            listener.mouseReleased(release);
        }
    }

    private static void layout(Component component) {
        component.doLayout();
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                layout(child);
            }
        }
    }

    private static void writePng(BufferedImage image, String fileName) throws Exception {
        assertTrue(REPORT_DIR.mkdirs() || REPORT_DIR.isDirectory());
        File output = new File(REPORT_DIR, fileName);
        assertTrue(ImageIO.write(image, "png", output));
        assertTrue(output.isFile());
        assertTrue(output.length() > 0);
    }

    private static void assertImageHasContent(String name,
                                              BufferedImage image,
                                              int minimumDistinctColors) {
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);

        int firstColor = image.getRGB(0, 0) & 0x00ffffff;
        int changedPixels = 0;
        Set<Integer> distinctColors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y) & 0x00ffffff;
                distinctColors.add(color);
                if (color != firstColor) {
                    changedPixels++;
                }
            }
        }

        assertTrue(name + " screenshot should not be uniform",
                changedPixels > 0);
        assertTrue(name + " screenshot should contain rendered detail",
                distinctColors.size() >= minimumDistinctColors);
    }

    private static void assertButtonLabels(Container container, String... expectedLabels) {
        List<String> labels = new ArrayList<>();
        collectButtonLabels(container, labels);
        for (String label : expectedLabels) {
            assertTrue(labels.contains(label));
        }
    }

    private static void collectButtonLabels(Container container, List<String> labels) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                labels.add(((JButton) child).getText());
            }
            if (child instanceof Container) {
                collectButtonLabels((Container) child, labels);
            }
        }
    }
}
