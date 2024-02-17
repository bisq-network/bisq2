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
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.PopupWindow;
import javafx.stage.WindowEvent;

public class DropdownMenu extends HBox {
    public static final Double INITIAL_WIDTH = 24.0;

    private final Label label = new Label();
    private final ImageView defaultIcon, activeIcon;
    private final ContextMenu contextMenu = new ContextMenu();
    private ImageView buttonIcon;
    private boolean isFirstRun = false;

    public DropdownMenu(String defaultIconId, String activeIconId, boolean useIconOnly) {
        defaultIcon = ImageUtil.getImageViewById(defaultIconId);
        activeIcon = ImageUtil.getImageViewById(activeIconId);

        buttonIcon = defaultIcon;

        getChildren().addAll(label, buttonIcon);

        getStyleClass().add("dropdown-menu");
        contextMenu.getStyleClass().add("dropdown-menu-popup");

        if (useIconOnly) {
            double size = 29;
            setMaxSize(size, size);
            setMinSize(size, size);
            setPrefSize(size, size);
            setAlignment(Pos.CENTER);
        } else {
            setSpacing(5);
            setAlignment(Pos.CENTER_RIGHT);
            setPadding(new Insets(0, 5, 0, 0));
        }

        attachListeners();
    }

    public void setLabel(String text) {
        label.setText(text);
    }

    private void toggleContextMenu() {
        if (!contextMenu.isShowing()) {
            contextMenu.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);
            Bounds bounds = this.localToScreen(this.getBoundsInLocal());
            double x = bounds.getMaxX();
            double y = bounds.getMaxY() + 3;
            contextMenu.show(this, x, y);
        } else {
            contextMenu.hide();
        }
    }

    public void addMenuItems(DropdownMenuItem... items) {
        contextMenu.getItems().addAll(items);
    }

    public void clearMenuItems() {
        contextMenu.getItems().clear();
    }

    public void setTooltip(String tooltip) {
        if (tooltip != null) {
            Tooltip.install(this, new BisqTooltip(tooltip));
        }
    }

    public void setTooltip(Tooltip tooltip) {
        if (tooltip != null) {
            Tooltip.install(this, tooltip);
        }
    }

    private void attachListeners() {
        setOnMouseClicked(event -> toggleContextMenu());
        setOnMouseExited(e -> updateIcon(contextMenu.isShowing() ? activeIcon : defaultIcon));
        setOnMouseEntered(e -> updateIcon(activeIcon));

        sceneProperty().addListener(new WeakChangeListener<>((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener(new WeakChangeListener<>((obs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.addEventHandler(WindowEvent.WINDOW_HIDING, e -> contextMenu.hide());
                    }
                }));
            }
        }));

        contextMenu.setOnShowing(e -> {
            getStyleClass().add("dropdown-menu-active");
            updateIcon(activeIcon);

        });
        contextMenu.setOnHidden(e -> {
            getStyleClass().remove("dropdown-menu-active");
            updateIcon(defaultIcon);
        });

        contextMenu.widthProperty().addListener(new WeakChangeListener<Number>((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > INITIAL_WIDTH && !isFirstRun) {
                isFirstRun = true;
                // Once the contextMenu has calculated the width on the first render time we update the items
                // so that they all have the same size.
                for (MenuItem item : contextMenu.getItems()) {
                    if (item instanceof DropdownMenuItem) {
                        DropdownMenuItem dropdownMenuItem = (DropdownMenuItem) item;
                        dropdownMenuItem.updateWidth(contextMenu.getWidth() - 5); // Remove margins
                    }
                }
            }
        }));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            getChildren().remove(buttonIcon);
            buttonIcon = newIcon;
            getChildren().add(buttonIcon);
        }
    }
}
