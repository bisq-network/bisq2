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

package bisq.desktop.main.content.chat;

import bisq.desktop.components.controls.BisqPopup;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.WindowEvent;

public class DropdownMenu extends Button {
    private final ContextMenu contextMenu;

    public DropdownMenu(String buttonText) {
        super(buttonText);
        this.contextMenu = new ContextMenu();

        // Configure the button to show the context menu
        this.setOnAction(event -> toggleContextMenu());
    }

    private void toggleContextMenu() {
        if (!contextMenu.isShowing()) {
            // Calculate the position
            contextMenu.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);
            final Bounds bounds = this.localToScreen(this.getBoundsInLocal());
            final double x = bounds.getMaxX(); // Align to right of the button
            final double y = bounds.getMaxY(); // Just below the button

            // Show the context menu at the calculated position
            contextMenu.show(this, x, y);
        } else {
            contextMenu.hide();
        }
    }

    public void addMenuItem(String itemText) {
        MenuItem item = new MenuItem(itemText);
        item.setOnAction(event -> {
            System.out.println(itemText + " selected");
            contextMenu.hide();
        });
        contextMenu.getItems().add(item);
    }

    // Add other methods as needed, for example, to remove items, clear items, etc.

    // You might also want to handle the case where the window loses focus to hide the context menu
    public void attachHideListeners() {
        // This listener ensures that the context menu is hidden when the window loses focus
        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.addEventHandler(WindowEvent.WINDOW_HIDING, e -> contextMenu.hide());
                    }
                });
            }
        });
    }
}
