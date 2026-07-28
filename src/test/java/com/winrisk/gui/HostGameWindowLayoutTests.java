package com.winrisk.gui;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HostGameWindowLayoutTests {

    @Test
    public void labeledSectionContainsHeaderLabelAndControl() {
        System.setProperty("java.awt.headless", "true");
        JComboBox<String> combo = new JComboBox<>();
        JPanel section = HostGameWindow.labeledSection("Board", combo);
        List<Component> tree = flatten(section);
        assertTrue("combo missing from section tree", tree.contains(combo));
        assertTrue("no JLabel with text 'Board'", tree.stream()
                .anyMatch(c -> c instanceof JLabel && "Board".equals(((JLabel) c).getText())));
    }

    @Test
    public void rulesSectionContainsAllCheckBoxesInTwoColumnGrid() {
        System.setProperty("java.awt.headless", "true");
        JCheckBox[] checkBoxes = {
                new JCheckBox("Skynet Mode"),
                new JCheckBox("Attack with All"),
                new JCheckBox("Fog of War"),
                new JCheckBox("Incremental Card Values"),
                new JCheckBox("Expanded Maneuver"),
                new JCheckBox("Attack Card Reroll"),
                new JCheckBox("Commander Die"),
        };
        JPanel section = HostGameWindow.rulesSection(checkBoxes);
        List<Component> tree = flatten(section);
        for (JCheckBox checkBox : checkBoxes) {
            assertTrue("missing checkbox " + checkBox.getText(), tree.contains(checkBox));
        }
        assertTrue("no JLabel with text 'Rules'", tree.stream()
                .anyMatch(c -> c instanceof JLabel && "Rules".equals(((JLabel) c).getText())));
        Container grid = checkBoxes[0].getParent();
        assertTrue("checkbox grid must use GridLayout", grid.getLayout() instanceof GridLayout);
        assertEquals(2, ((GridLayout) grid.getLayout()).getColumns());
        for (JCheckBox checkBox : checkBoxes) {
            assertSame("all checkboxes share one grid", grid, checkBox.getParent());
        }
    }

    private static List<Component> flatten(Component root) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        return all;
    }

    private static void collect(Component component, List<Component> into) {
        into.add(component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collect(child, into);
            }
        }
    }
}
