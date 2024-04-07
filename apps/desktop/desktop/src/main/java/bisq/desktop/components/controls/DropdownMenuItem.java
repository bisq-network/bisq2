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

package bisq.desktop.components.controls;

import bisq.desktop.common.utils.ImageUtil;
import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class DropdownMenuItem extends CustomMenuItem {
    private static final String ICON_CSS_STYLE = "menu-item-icon";

    private final HBox content;
    private final Label label;
    private ImageView defaultIcon, activeIcon, buttonIcon;

    public DropdownMenuItem(String defaultIconId, String activeIconId, String text) {
        label = new Label(text);
        content = new HBox(8, label);
        content.getStyleClass().add("dropdown-menu-item-content");
        content.setAlignment(Pos.CENTER_LEFT);
        setContent(content);

        if (defaultIconId != null && activeIconId != null) {
            defaultIcon = ImageUtil.getImageViewById(defaultIconId);
            activeIcon = ImageUtil.getImageViewById(activeIconId);
            defaultIcon.getStyleClass().add(ICON_CSS_STYLE);
            activeIcon.getStyleClass().add(ICON_CSS_STYLE);
            buttonIcon = defaultIcon;
            content.getChildren().add(0, buttonIcon);
            attachListeners();
        }
    }

    public DropdownMenuItem(String text) {
        this(null, null, text);
    }

    public DropdownMenuItem(String defaultIconId, String activeIconId) {
        this(defaultIconId, activeIconId, "");
    }

    public void setLabelText(String text) {
        label.setText(text);
    }

    public Double getWidth() {
        return content.getWidth();
    }

    public void updateWidth(Double width) {
        content.setPrefWidth(width);
    }

    public String getLabelText() {
        return label.getText();
    }

    private void attachListeners() {
        content.setOnMouseEntered(e -> updateIcon(activeIcon));
        content.setOnMouseExited(e -> updateIcon(defaultIcon));
        content.setOnMouseClicked(e -> updateIcon(defaultIcon));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            content.getChildren().remove(buttonIcon);
            buttonIcon = newIcon;
            if (buttonIcon != null) {
                content.getChildren().add(0, buttonIcon);
            }
        }
    }
}
