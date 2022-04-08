/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.common.utils;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

public class Icons {
    private static final String MATERIAL_DESIGN_ICONS = "'Material Design Icons'";

    public static Text getIconForLabel(GlyphIcons icon, String iconSize, Label label) {
        return getIconForLabel(icon, iconSize, label, null);
    }

    public static Label getIconForLabel(AwesomeIcon icon, Label label, String fontSize) {
        AwesomeDude.setIcon(label, icon, fontSize);
        return label;
    }

    public static Label getIcon(AwesomeIcon icon, String fontSize) {
        Label label = new Label();
        AwesomeDude.setIcon(label, icon, fontSize);
        return label;
    }


    public static Label getIcon(AwesomeIcon icon) {
        return getIcon(icon, AwesomeDude.DEFAULT_ICON_SIZE);
    }

    public static Text getSmallIconForLabel(GlyphIcons icon, Label label, String style) {
        return getIconForLabel(icon, "0.769em", label, style);
    }

    public static Text getIconForLabel(GlyphIcons icon, String iconSize, Label label, String style) {
        if (icon.fontFamily().equals(MATERIAL_DESIGN_ICONS)) {
            final Text textIcon = MaterialDesignIconFactory.get().createIcon(icon, iconSize);
            textIcon.setOpacity(0.7);
            if (style != null) {
                textIcon.getStyleClass().add(style);
            }
            label.setContentDisplay(ContentDisplay.LEFT);
            label.setGraphic(textIcon);
            return textIcon;
        } else {
            throw new IllegalArgumentException("Not supported icon type");
        }
    }
}