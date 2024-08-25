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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class DropdownMenuItem extends CustomMenuItem {
    public static final String ICON_CSS_STYLE = "menu-item-icon";

    private final HBox hBox;
    private ImageView defaultIcon, activeIcon, buttonIcon;

    public DropdownMenuItem(String defaultIconId, String activeIconId, Node node) {
        hBox = new HBox(10);
        hBox.getStyleClass().add("dropdown-menu-item-content");

        if (defaultIconId != null && activeIconId != null) {
            defaultIcon = ImageUtil.getImageViewById(defaultIconId);
            activeIcon = ImageUtil.getImageViewById(activeIconId);
            defaultIcon.getStyleClass().add(ICON_CSS_STYLE);
            activeIcon.getStyleClass().add(ICON_CSS_STYLE);
            buttonIcon = defaultIcon;
            hBox.getChildren().add(buttonIcon);
            attachListeners();
        }

        if (node != null) {
            hBox.getChildren().add(node);
        }

        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getStyleClass().add("bisq-menu-item");
        hBox.addEventFilter(ActionEvent.ANY, Event::consume);
        setContent(hBox);
    }

    public void updateWidth(Double width) {
        hBox.setPrefWidth(width);
    }

    private void attachListeners() {
        hBox.setOnMouseEntered(e -> updateIcon(activeIcon));
        hBox.setOnMouseExited(e -> updateIcon(defaultIcon));
        hBox.setOnMouseClicked(e -> updateIcon(defaultIcon));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            buttonIcon = newIcon;
            hBox.getChildren().removeFirst();
            hBox.getChildren().addFirst(buttonIcon);
        }
    }
}
