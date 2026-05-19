package de.jensd.fx.fontawesome;

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphsFactory;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class AwesomeDude {
    public static final String DEFAULT_ICON_SIZE = GlyphIcon.DEFAULT_ICON_SIZE.toString();
    public static final String DEFAULT_FONT_SIZE = GlyphIcon.DEFAULT_FONT_SIZE;

    private static final GlyphsFactory FACTORY = new GlyphsFactory(FontAwesomeIcon.class);

    public static Label createIconLabel(AwesomeIcon icon) {
        return createIconLabel(icon, DEFAULT_ICON_SIZE);
    }

    public static Label createIconLabel(AwesomeIcon icon, String iconSize) {
        return FACTORY.createIconLabel(icon.delegate(), "", iconSize, DEFAULT_FONT_SIZE, ContentDisplay.LEFT);
    }

    public static Button createIconButton(AwesomeIcon icon) {
        return FACTORY.createIconButton(icon.delegate());
    }

    public static Button createIconButton(AwesomeIcon icon, String text) {
        return FACTORY.createIconButton(icon.delegate(), text);
    }

    public static Button createIconButton(AwesomeIcon icon,
                                          String text,
                                          String iconSize,
                                          String fontSize,
                                          ContentDisplay contentDisplay) {
        return FACTORY.createIconButton(icon.delegate(), text, iconSize, fontSize, contentDisplay);
    }

    public static ToggleButton createIconToggleButton(AwesomeIcon icon,
                                                      String text,
                                                      String iconSize,
                                                      ContentDisplay contentDisplay) {
        return FACTORY.createIconToggleButton(icon.delegate(), text, iconSize, contentDisplay);
    }

    public static ToggleButton createIconToggleButton(AwesomeIcon icon,
                                                      String text,
                                                      String iconSize,
                                                      String fontSize,
                                                      ContentDisplay contentDisplay) {
        return FACTORY.createIconToggleButton(icon.delegate(), text, iconSize, fontSize, contentDisplay);
    }

    public static void setIcon(Label label, AwesomeIcon icon) {
        FACTORY.setIcon(label, icon.delegate());
    }

    public static void setIcon(Label label, AwesomeIcon icon, String iconSize) {
        FACTORY.setIcon(label, icon.delegate(), iconSize);
    }

    public static void setIcon(Button button,
                               AwesomeIcon icon,
                               String iconSize,
                               String fontSize,
                               ContentDisplay contentDisplay) {
        FACTORY.setIcon(button, icon.delegate(), iconSize, contentDisplay);
        button.setStyle(String.format("-fx-font-size: %s;", fontSize));
    }

    public static void setIcon(ToggleButton button,
                               AwesomeIcon icon,
                               String iconSize,
                               String fontSize,
                               ContentDisplay contentDisplay) {
        FACTORY.setIcon(button, icon.delegate(), iconSize, contentDisplay);
        button.setStyle(String.format("-fx-font-size: %s;", fontSize));
    }
}
