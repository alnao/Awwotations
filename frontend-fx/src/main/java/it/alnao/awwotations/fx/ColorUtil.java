package it.alnao.awwotations.fx;

import javafx.scene.paint.Color;

/** Small helpers to convert between hex strings and JavaFX colors. */
public final class ColorUtil {
    public static final String DEFAULT_HEX = "#ffd966";

    private ColorUtil() {
    }

    public static Color parse(String value) {
        if (value == null || value.isBlank()) {
            return Color.web(DEFAULT_HEX);
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException e) {
            return Color.web(DEFAULT_HEX);
        }
    }

    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
