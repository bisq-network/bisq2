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

import javafx.geometry.Bounds;
import javafx.scene.layout.HBox;

public class DrawerMenu extends HBox {
    private final BisqMenuItem menuButton;
    private final HBox itemsHBox = new HBox();
    private final BisqPopup drawerPopup = new BisqPopup();

    public DrawerMenu(String defaultIconId, String activeIconId) {
        menuButton = new BisqMenuItem(defaultIconId, activeIconId);
        menuButton.useIconOnly();

        itemsHBox.getStyleClass().add("drawer-menu-items");
        drawerPopup.setContentNode(itemsHBox);

        getChildren().add(menuButton);
        getStyleClass().add("drawer-menu");

        attachListeners();
    }

    private void attachListeners() {
        menuButton.setOnAction(e -> {
            togglePopup();
        });

        drawerPopup.setOnHidden(e -> {
            setPrefWidth(menuButton.getWidth());
        });

        drawerPopup.setOnShowing(e -> {
            setPrefWidth(menuButton.getWidth() + itemsHBox.getWidth());
        });
    }

    private void togglePopup() {
        if (!drawerPopup.isShowing()) {
            Bounds bounds = localToScreen(getBoundsInLocal());
            double x = bounds.getMaxX();
            double y = bounds.getMinY();
            drawerPopup.show(this, x, y);
        } else {
            drawerPopup.hide();
        }
    }
}
