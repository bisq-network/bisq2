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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import lombok.Getter;
import lombok.Setter;

public class DropdownListMenu<T> extends HBox {
    @Getter
    private final BooleanProperty isMenuShowing = new SimpleBooleanProperty(false);
    @Getter
    private final HBox hBox = new HBox();
    private final BisqPopup popup = new BisqPopup();
    @Getter
    private final BisqTableView<T> tableView;
    @Setter
    private boolean openUpwards = false;
    @Setter
    private boolean openToTheRight = false;
    private ImageView defaultIcon, activeIcon, buttonIcon;

    public DropdownListMenu(String defaultIconId,
                            String activeIconId,
                            boolean useIconOnly,
                            SortedList<T> sortedList) {
        defaultIcon = ImageUtil.getImageViewById(defaultIconId);
        activeIcon = ImageUtil.getImageViewById(activeIconId);
        buttonIcon = defaultIcon;

        getChildren().addAll(hBox, buttonIcon);
        hBox.getStyleClass().add("dropdown-menu-content-hbox");
        hBox.setAlignment(Pos.BASELINE_LEFT);

        tableView = new BisqTableView<>(sortedList);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        popup.setContentNode(tableView);
        popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);
        popup.getStyleClass().add("dropdown-menu-popup");

        if (useIconOnly) {
            double size = 29;
            setMaxSize(size, size);
            setMinSize(size, size);
            setPrefSize(size, size);
            setAlignment(Pos.CENTER);
        } else {
            setSpacing(5);
            setAlignment(Pos.BASELINE_LEFT);
        }

        getStyleClass().addAll("dropdown-menu", "dropdown-list-menu");
    }

    public void initialize() {
        tableView.initialize();

        setOnMouseClicked(e -> togglePopup());
        setOnMouseExited(e -> updateIcon(popup.isShowing() ? activeIcon : defaultIcon));
        setOnMouseEntered(e -> updateIcon(activeIcon));
        popup.setOnShowing(e -> {
            getStyleClass().add("dropdown-menu-active");
            updateIcon(activeIcon);
            isMenuShowing.setValue(true);
        });
        popup.setOnHidden(e -> {
            getStyleClass().remove("dropdown-menu-active");
            updateIcon(isHover() ? activeIcon : defaultIcon);
            isMenuShowing.setValue(false);
        });
        tableView.setOnMouseClicked(e -> popup.hide());
    }

    public void dispose() {
        tableView.dispose();

        setOnMouseClicked(null);
        setOnMouseExited(null);
        setOnMouseEntered(null);
        popup.setOnShowing(null);
        popup.setOnHidden(null);
        tableView.setOnMouseClicked(null);
    }

    public void setIcons(String newDefaultIconId, String newActiveIconId) {
        ImageView newDefault = ImageUtil.getImageViewById(newDefaultIconId);
        ImageView newActive = ImageUtil.getImageViewById(newActiveIconId);

        defaultIcon = newDefault;
        activeIcon = newActive;

        if (isMenuShowing.get() || isHover()) {
            updateIcon(activeIcon);
        } else {
            updateIcon(defaultIcon);
        }
    }

    public void setLabelAsContent(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.BASELINE_LEFT);
        setContent(label);
    }

    public void setContent(Node content) {
        hBox.getChildren().setAll(content);
    }

    public void useSpaceBetweenContentAndIcon() {
        getChildren().setAll(hBox, Spacer.fillHBox(), buttonIcon);
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

    protected void togglePopup() {
        if (!popup.isShowing()) {
            popup.setAnchorLocation(getAnchorLocation());
            Bounds bounds = localToScreen(getBoundsInLocal());
            double x = openToTheRight ? bounds.getMinX() : bounds.getMaxX();
            double y = openUpwards ? bounds.getMinY() - 3 : bounds.getMaxY() + 3;
            popup.show(this, x, y);
        } else {
            popup.hide();
        }
    }

    protected void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            getChildren().remove(buttonIcon);
            buttonIcon = newIcon;
            getChildren().add(buttonIcon);
        }
    }

    private PopupWindow.AnchorLocation getAnchorLocation() {
        if (!openUpwards) {
            return openToTheRight
                    ? PopupWindow.AnchorLocation.WINDOW_TOP_LEFT
                    : PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT;
        } else {
            return openToTheRight
                    ? PopupWindow.AnchorLocation.WINDOW_BOTTOM_LEFT
                    : PopupWindow.AnchorLocation.WINDOW_BOTTOM_RIGHT;
        }
    }
}
