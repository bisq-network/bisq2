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
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class DrawerMenu extends Button {
    private final DrawerPopup drawerPopup = new DrawerPopup();
    private final ImageView defaultIcon, activeIcon;
    private ImageView buttonIcon;

    public DrawerMenu(String defaultIconId, String activeIconId) {
        defaultIcon = ImageUtil.getImageViewById(defaultIconId);
        activeIcon = ImageUtil.getImageViewById(activeIconId);
        buttonIcon = defaultIcon;
        setGraphic(buttonIcon);
        getStyleClass().add("drawer-menu");
        setAlignment(Pos.CENTER);

        double size = 29;
        setMaxSize(size, size);
        setMinSize(size, size);
        setPrefSize(size, size);

        attachListeners();
    }

    private void attachListeners() {
        setOnMouseEntered(e -> updateIcon(activeIcon));
        setOnMouseExited(e -> updateIcon(defaultIcon));
        setOnMouseClicked(e -> updateIcon(defaultIcon));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            buttonIcon = newIcon;
            setGraphic(buttonIcon);
        }
    }
}
