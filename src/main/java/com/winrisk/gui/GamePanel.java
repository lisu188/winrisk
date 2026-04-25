package com.winrisk.gui;

import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.view.Viewable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class GamePanel extends JPanel {

    private static final long serialVersionUID = -8440337679365334157L;

    private final GraphicsSurface surface = new GraphicsSurface();

    private final Viewable viewable;

    public GamePanel(Viewable viewable) {
        setMouseListener();
        setAncestorListener();
        this.viewable = viewable;
        if (!Boolean.getBoolean("java.awt.headless")
                && !GraphicsEnvironment.isHeadless()) {
            JFrame frame = new JFrame();
            frame.setSize(800, 600);
            frame.getContentPane().add(this);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.clearRect(0, 0, getWidth(), getHeight());
        surface.setGraphics(this, g);
        viewable.onDraw(surface);
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
                        viewable.onEvent(new MotionEvent(
                                MotionEvent.ACTION_DOWN, e.getWhen(), e.getWhen(),
                                e.getX(), e.getY()));
                    }
                } finally {
                    e.getComponent().repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    MotionEvent event = new MotionEvent(MotionEvent.ACTION_UP,
                            e.getWhen(), e.getWhen(), e.getX(), e.getY());
                    viewable.onEvent(event);
                    e.getComponent().repaint();
                } finally {
                    e.getComponent().repaint();
                }
            }
        });
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
