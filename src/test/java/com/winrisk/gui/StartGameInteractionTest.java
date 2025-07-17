package com.winrisk.gui;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StartGameInteractionTest {
    @Test
    public void testButtons() {
        System.setProperty("java.awt.headless", "true");
        try {
            StartGame sg = new StartGame();
            sg.setVisible(false);
            Container c = sg.getContentPane();
            for (Component comp : c.getComponents()) {
                if (comp instanceof JButton) {
                    for (java.awt.event.ActionListener l : ((JButton) comp).getActionListeners()) {
                        l.actionPerformed(new ActionEvent(comp, ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            }
        } catch (HeadlessException ignore) {
        }
    }
}
