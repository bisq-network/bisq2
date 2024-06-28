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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import lombok.Getter;

public class DrawerMenu extends HBox {
    private final Button menuButton = new Button();
    private final HBox itemsHBox = new HBox();
    private final ImageView defaultIcon, hoverIcon, activeIcon;
    @Getter
    private final BooleanProperty isMenuShowing = new SimpleBooleanProperty(false);
    private ImageView buttonIcon;

    public DrawerMenu(String defaultIconId, String hoverIconId, String activeIconId) {
        defaultIcon = ImageUtil.getImageViewById(defaultIconId);
        hoverIcon = ImageUtil.getImageViewById(hoverIconId);
        activeIcon = ImageUtil.getImageViewById(activeIconId);
        defaultIcon.getStyleClass().add(BisqMenuItem.ICON_CSS_STYLE);
        hoverIcon.getStyleClass().add(BisqMenuItem.ICON_CSS_STYLE);
        activeIcon.getStyleClass().add(BisqMenuItem.ICON_CSS_STYLE);
        buttonIcon = defaultIcon;

        double size = 29;
        menuButton.getStyleClass().add("bisq-menu-item");
        menuButton.setGraphic(buttonIcon);
        menuButton.setMaxSize(size, size);
        menuButton.setMinSize(size, size);
        menuButton.setPrefSize(size, size);
        menuButton.setAlignment(Pos.CENTER);

        itemsHBox.getStyleClass().add("drawer-menu-items");
        itemsHBox.setVisible(false);
        itemsHBox.setManaged(false);
        itemsHBox.setAlignment(Pos.CENTER);

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
        menuButton.setOnAction(e -> toggleItemsHBox());
        menuButton.setOnMouseEntered(e -> {
            if (!itemsHBox.isVisible()) {
                updateIcon(hoverIcon);
            } else {
                updateIcon(activeIcon);
            }
        });
        menuButton.setOnMouseExited(e -> {
            if (!itemsHBox.isVisible()) {
                updateIcon(defaultIcon);
            } else {
                updateIcon(activeIcon);
            }
        });
        menuButton.setOnMouseClicked(e -> {
            if (!itemsHBox.isVisible()) {
                updateIcon(defaultIcon);
            } else {
                updateIcon(activeIcon);
            }
        });

        itemsHBox.visibleProperty().addListener(change -> {
            if (itemsHBox.isVisible()) {
                getStyleClass().add("drawer-menu-active");
                isMenuShowing.setValue(true);
            } else {
                getStyleClass().remove("drawer-menu-active");
                updateIcon(defaultIcon);
                isMenuShowing.setValue(false);
            }
        });

        sceneProperty().addListener(new WeakChangeListener<>((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                    if (itemsHBox.isVisible() && !clickedOnDrawerMenu(e)) {
                        hideMenu();
                        e.consume();
                    }
                });
            }
        }));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            buttonIcon = newIcon;
            menuButton.setGraphic(buttonIcon);
        }
    }

    private void toggleItemsHBox() {
        // TODO: Add width transition
        itemsHBox.setVisible(!itemsHBox.isVisible());
        itemsHBox.setManaged(!itemsHBox.isManaged());
    }

    private boolean clickedOnDrawerMenu(MouseEvent e) {
        double mouseX = e.getSceneX();
        double mouseY = e.getSceneY();
        double nodeMinX = localToScene(getBoundsInLocal()).getMinX();
        double nodeMinY = localToScene(getBoundsInLocal()).getMinY();
        double nodeMaxX = localToScene(getBoundsInLocal()).getMaxX();
        double nodeMaxY = localToScene(getBoundsInLocal()).getMaxY();

        return mouseX >= nodeMinX && mouseX <= nodeMaxX && mouseY >= nodeMinY && mouseY <= nodeMaxY;
    }
}
