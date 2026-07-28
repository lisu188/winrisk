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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiSmokeScreenshotTests {
    private static final File REPORT_DIR = new File("build/reports/ui-smoke");
    private static final String[] SCREENSHOT_FILES = {
            "start-menu.png",
            "game-board.png",
            "pangaea-board.png",
            "gameplay-click-emulation.png"
    };

    @Test
    public void writesStartMenuScreenshotForInspection() throws Exception {
        String originalHeadless = forceHeadless();
        try {
            JPanel menu = StartGame.createMenuPanel(() -> {
            }, () -> {
            }, () -> {
            }, () -> {
            });

            BufferedImage image = render(menu, 380, 440);

            assertButtonLabels(menu, "HOST GAME", "NEW MAP", "LOAD MAP", "LOAD GAME");
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
            GamePanel panel = new GamePanel(createClassicGame());
            // Compose board + HUD side panel exactly like the real window.
            BufferedImage image = render(panel.buildWindowContent(), 1180, 720);

            assertImageHasContent("game board", image, 16);
            writePng(image, "game-board.png");
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void writesPangaeaBoardScreenshotForInspection() throws Exception {
        String originalHeadless = forceHeadless();
        try {
            Params params = new Params();
            params.setHumanPlayers(0);
            params.setAiPlayers(3);
            params.setGameMode(GameMode.CLASSIC);
            params.setBuiltinMap("pangaea");
            params.setRandomSeed(1234L);
            GamePanel panel = new GamePanel(new Game(params));
            BufferedImage image = render(panel, 800, 600);

            assertImageHasContent("pangaea board", image, 16);
            writePng(image, "pangaea-board.png");
        } finally {
            restoreHeadless(originalHeadless);
        }
    }

    @Test
    public void hudPanelAccompaniesBoardInWindowContent() {
        String originalHeadless = forceHeadless();
        try {
            Game game = createClassicGame();
            GamePanel panel = new GamePanel(game);

            JComponent content = (JComponent) panel.buildWindowContent();

            assertButtonLabels(content, "End Phase");
            List<String> labels = new ArrayList<>();
            collectLabelTexts(content, labels);
            assertTrue(labels.stream().anyMatch(text -> text.contains("Phase: ")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("Reinforcements: ")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("Mode: classic")));
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
        Point at = panel.boardToScreen(field.getPoint().x, field.getPoint().y);
        pressAndRelease(panel, at.x, at.y, at.x, at.y, button);
    }

    private static void drag(GamePanel panel, Field from, Field to) {
        Point start = panel.boardToScreen(from.getPoint().x, from.getPoint().y);
        Point end = panel.boardToScreen(to.getPoint().x, to.getPoint().y);
        pressAndRelease(panel, start.x, start.y, end.x, end.y, MouseEvent.BUTTON1);
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

        writeIndex();
        assertIndexReferencesScreenshots();
    }

    private static void writeIndex() throws Exception {
        File output = new File(REPORT_DIR, "index.html");
        Files.writeString(output.toPath(), buildIndex(), StandardCharsets.UTF_8);
        assertTrue(output.isFile());
        assertTrue(output.length() > 0);
    }

    private static String buildIndex() {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"utf-8\">\n");
        html.append("  <title>WinRisk UI Smoke Screenshots</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: sans-serif; margin: 2rem; background: #f7f7f7; color: #222; }\n");
        html.append("    h1 { margin-top: 0; }\n");
        html.append("    ul { display: grid; gap: 1.5rem; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); list-style: none; padding: 0; }\n");
        html.append("    li { background: #fff; border: 1px solid #ddd; padding: 1rem; }\n");
        html.append("    img { display: block; max-width: 100%; height: auto; border: 1px solid #ccc; }\n");
        html.append("    a { color: #075985; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>WinRisk UI Smoke Screenshots</h1>\n");
        html.append("  <ul>\n");
        for (String fileName : SCREENSHOT_FILES) {
            html.append("    <li>\n");
            html.append("      <h2>").append(fileName).append("</h2>\n");
            html.append("      <a href=\"").append(fileName).append("\">");
            html.append("<img src=\"").append(fileName).append("\" alt=\"").append(fileName).append("\">");
            html.append("</a>\n");
            html.append("    </li>\n");
        }
        html.append("  </ul>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private static void assertIndexReferencesScreenshots() throws Exception {
        File output = new File(REPORT_DIR, "index.html");
        String html = Files.readString(output.toPath(), StandardCharsets.UTF_8);
        for (String fileName : SCREENSHOT_FILES) {
            assertTrue("index should link " + fileName,
                    html.contains("href=\"" + fileName + "\""));
            assertTrue("index should embed " + fileName,
                    html.contains("src=\"" + fileName + "\""));
        }
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
}
