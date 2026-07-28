package com.winrisk.gui;

import com.winrisk.game.ai.InteractiveHuman;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;
import com.winrisk.game.view.Viewable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * The board canvas. Draws the current {@link Viewable} through a
 * {@link BoardViewport} so the board scales uniformly to whatever size the
 * (now resizable) window has, and maps mouse input back through the same
 * transform. Window composition (canvas + HUD side panel) happens in
 * {@link #buildWindowContent()}.
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = -8440337679365334157L;

    private final GraphicsSurface surface = new GraphicsSurface();

    private final Viewable viewable;
    private final BoardViewport viewport;
    private HudPanel hud;
    private long pressWhen;

    public GamePanel(Viewable viewable) {
        setMouseListener();
        setAncestorListener();
        this.viewable = viewable;
        this.viewport = viewable instanceof Game
                ? BoardViewport.forFields(((Game) viewable).getFields())
                : BoardViewport.defaultBoard();
        setBackground(UiTheme.SLATE);
        if (!Boolean.getBoolean("java.awt.headless")
                && !GraphicsEnvironment.isHeadless()) {
            installHumanChoiceProviders(viewable);
            JFrame frame = new JFrame("WinRisk");
            frame.setSize(1100, 720);
            frame.setMinimumSize(new Dimension(900, 620));
            frame.getContentPane().add(buildWindowContent());
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    /**
     * Composes the full window content: this canvas in the centre with the
     * HUD side panel on the right.
     */
    JComponent buildWindowContent() {
        JPanel content = new JPanel(new BorderLayout());
        hud = new HudPanel(viewable, this::stateChanged);
        content.add(this, BorderLayout.CENTER);
        content.add(hud, BorderLayout.EAST);
        return content;
    }

    BoardViewport getViewport() {
        return viewport;
    }

    /**
     * Maps board coordinates to current screen pixels; used by tests to aim
     * synthetic mouse events.
     */
    Point boardToScreen(int boardX, int boardY) {
        viewport.setScreenSize(getWidth(), getHeight());
        return new Point(viewport.boardToScreenX(boardX), viewport.boardToScreenY(boardY));
    }

    private void stateChanged() {
        repaint();
        if (hud != null) {
            hud.refresh();
        }
    }

    private void installHumanChoiceProviders(Viewable viewable) {
        if (!(viewable instanceof Game)) {
            return;
        }
        SwingHumanChoiceProvider provider = new SwingHumanChoiceProvider();
        for (Player player : ((Game) viewable).getPlayers()) {
            if (player.getPlayerInterface() instanceof InteractiveHuman) {
                ((InteractiveHuman) player.getPlayerInterface()).setChoiceProvider(provider);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(UiTheme.SLATE);
        g.fillRect(0, 0, getWidth(), getHeight());
        viewport.setScreenSize(getWidth(), getHeight());
        paintBoardBackdrop(g);
        surface.setGraphics(this, g, viewport);
        viewable.onDraw(surface);
    }

    private void paintBoardBackdrop(Graphics g) {
        int x0 = viewport.boardToScreenX(viewport.getBoardX());
        int y0 = viewport.boardToScreenY(viewport.getBoardY());
        int x1 = viewport.boardToScreenX(viewport.getBoardX() + viewport.getBoardWidth());
        int y1 = viewport.boardToScreenY(viewport.getBoardY() + viewport.getBoardHeight());
        g.setColor(UiTheme.PARCHMENT);
        g.fillRect(x0, y0, x1 - x0, y1 - y0);
    }

    private void setAncestorListener() {
        this.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent a) {
                setWindowListener();
            }

            @Override
            public void ancestorMoved(AncestorEvent arg0) {

            }

            @Override
            public void ancestorRemoved(AncestorEvent arg0) {

            }

        });
    }

    private void setMouseListener() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        viewable.onAction();
                    } else {
                        pressWhen = e.getWhen();
                        viewable.onEvent(new MotionEvent(
                                MotionEvent.ACTION_DOWN, e.getWhen(), e.getWhen(),
                                toBoardX(e), toBoardY(e)));
                    }
                } finally {
                    stateChanged();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    // Preserve the press timestamp so hold-to-act gestures
                    // (reinforce all, delete territory) can be detected.
                    MotionEvent event = new MotionEvent(MotionEvent.ACTION_UP,
                            pressWhen, e.getWhen(), toBoardX(e), toBoardY(e));
                    viewable.onEvent(event);
                } finally {
                    stateChanged();
                }
            }
        });
    }

    private int toBoardX(MouseEvent e) {
        return viewport.screenToBoardX(e.getX());
    }

    private int toBoardY(MouseEvent e) {
        return viewport.screenToBoardY(e.getY());
    }

    private void setWindowListener() {
        SwingUtilities.getWindowAncestor(this).addWindowListener(
                new WindowListener() {

                    @Override
                    public void windowActivated(WindowEvent arg0) {

                    }

                    @Override
                    public void windowClosed(WindowEvent arg0) {

                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        String path = null;
                        int dialogResult = JOptionPane.showConfirmDialog(null,
                                "Would You Like to Save First?", "Warning",
                                JOptionPane.YES_NO_OPTION);
                        if (dialogResult == JOptionPane.YES_OPTION) {
                            FileDialog dialog = new FileDialog(
                                    (JFrame) SwingUtilities
                                            .getWindowAncestor(GamePanel.this));
                            dialog.setVisible(true);
                            path = dialog.getFile();
                        }
                        if (path != null) {
                            viewable.onSave(path);
                        }
                        new StartGame().setVisible(true);
                    }

                    @Override
                    public void windowDeactivated(WindowEvent arg0) {
                    }

                    @Override
                    public void windowDeiconified(WindowEvent arg0) {

                    }

                    @Override
                    public void windowIconified(WindowEvent arg0) {

                    }

                    @Override
                    public void windowOpened(WindowEvent arg0) {

                    }
                });
    }
}
