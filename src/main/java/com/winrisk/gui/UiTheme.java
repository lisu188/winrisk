package com.winrisk.gui;

import java.awt.Color;
import java.awt.Font;

/**
 * Shared look-and-feel constants for the Swing chrome: a dark slate frame
 * around a parchment board, with light text on dark panels.
 */
final class UiTheme {

    static final Color SLATE = new Color(0x26313C);
    static final Color SLATE_LIGHT = new Color(0x34495E);
    static final Color PARCHMENT = new Color(0xF2EBD9);
    static final Color INK = new Color(0x1B2631);
    static final Color TEXT_LIGHT = new Color(0xECF0F1);
    static final Color TEXT_MUTED = new Color(0x95A5A6);
    static final Color ACCENT = new Color(0xD68910);

    static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private UiTheme() {
    }
}
