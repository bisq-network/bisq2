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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

public class DrawerMenu extends HBox {
    private final BisqMenuItem menuButton;
    private final HBox itemsHBox = new HBox();

    public DrawerMenu(String defaultIconId, String activeIconId) {
        menuButton = new BisqMenuItem(defaultIconId, activeIconId);
        menuButton.useIconOnly();

        itemsHBox.getStyleClass().add("drawer-menu-items");
        itemsHBox.setVisible(false);
        itemsHBox.setManaged(false);
        itemsHBox.setSpacing(5);
        itemsHBox.setAlignment(Pos.CENTER);
        itemsHBox.setPadding(new Insets(0, 0, 0, 5));

        getChildren().addAll(menuButton, itemsHBox);
        getStyleClass().add("drawer-menu");

        attachListeners();
    }

    public void addItems(BisqMenuItem... items) {
        itemsHBox.getChildren().addAll(items);
    }

    public void hideMenu() {
        itemsHBox.setVisible(false);
        itemsHBox.setManaged(false);
    }

    private void attachListeners() {
        menuButton.setOnAction(e -> {
            toggleItemsHBox();
        });
    }

    private void toggleItemsHBox() {
        // TODO: Add width transition
        itemsHBox.setVisible(!itemsHBox.isVisible());
        itemsHBox.setManaged(!itemsHBox.isManaged());
    }
}
