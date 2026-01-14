package com.fakedata.generator;

import java.awt.Color;

/**
 * Shared color constants used across file generators.
 * Provides a consistent visual theme for all generated documents.
 */
public final class GeneratorColors {

    private GeneratorColors() {
        // Utility class - prevent instantiation
    }

    // Primary accent color (blue)
    public static final Color ACCENT_BLUE = new Color(66, 133, 244);

    // Text colors
    public static final Color TEXT_DARK = new Color(51, 51, 51);
    public static final Color TEXT_MEDIUM = new Color(102, 102, 102);
    public static final Color TEXT_LIGHT = new Color(153, 153, 153);

    // Background colors
    public static final Color BG_ALT_ROW = new Color(245, 245, 245);
    public static final Color BG_GRID = new Color(220, 220, 220);

    // Status colors
    public static final Color STATUS_POSITIVE = new Color(15, 157, 88);
    public static final Color STATUS_NEGATIVE = new Color(219, 68, 55);

    // Chart color palette
    public static final Color[] CHART_PALETTE = {
            ACCENT_BLUE,                  // Blue
            STATUS_NEGATIVE,              // Red
            new Color(244, 180, 0),       // Yellow
            STATUS_POSITIVE,              // Green
            new Color(171, 71, 188),      // Purple
            new Color(255, 112, 67)       // Orange
    };
}
