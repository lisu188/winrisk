package com.winrisk.gui;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.Params;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        int pixels = image.getWidth() * image.getHeight();
        assertTrue(name + " screenshot should not be uniform",
                changedPixels > pixels / 20);
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
