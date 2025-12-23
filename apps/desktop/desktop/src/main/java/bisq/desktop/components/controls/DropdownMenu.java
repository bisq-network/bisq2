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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

public class DropdownMenu extends HBox {
    public static final Double INITIAL_WIDTH = 24.0;

    @Getter
    private final BooleanProperty isMenuShowing = new SimpleBooleanProperty(false);
    @Getter
    private final HBox hBox = new HBox();
    private boolean isFirstRun = false;
    @Setter
    private boolean openUpwards = false;
    @Setter
    private boolean openToTheRight = false;
    private Double prefWidth = null;
    protected ImageView defaultIcon, activeIcon;
    protected ImageView buttonIcon;
    @Getter
    protected final ContextMenu contextMenu = new ContextMenu();

    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Window> windowListener;
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> widthPropertyChangeListener;
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Scene> sceneListener;

    public DropdownMenu(String defaultIconId, String activeIconId, boolean useIconOnly) {
        defaultIcon = ImageUtil.getImageViewById(defaultIconId);
        activeIcon = ImageUtil.getImageViewById(activeIconId);

        buttonIcon = defaultIcon;

        getChildren().addAll(hBox, buttonIcon);
        hBox.getStyleClass().add("dropdown-menu-content-hbox");
        hBox.setAlignment(Pos.BASELINE_LEFT);

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
            setAlignment(Pos.BASELINE_LEFT);
        }

        widthPropertyChangeListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > INITIAL_WIDTH && !isFirstRun) {
                isFirstRun = true;
                // Once the contextMenu has calculated the width on the first render time we update the items
                // so that they all have the same size.
                prefWidth = contextMenu.getWidth() - 18; // Remove margins
                updateMenuItemWidth();
            }
        };
        windowListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                newValue.addEventHandler(WindowEvent.WINDOW_HIDING, e -> contextMenu.hide());
            }
        };
        sceneListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                newValue.windowProperty().addListener(new WeakChangeListener<>(windowListener));
            }
        };

        attachListeners();
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

    public void addMenuItems(Collection<? extends MenuItem> items) {
        contextMenu.getItems().addAll(items);
    }

    public void addMenuItems(MenuItem... items) {
        contextMenu.getItems().addAll(items);
    }

    public ObservableList<MenuItem> getMenuItems() {
        return contextMenu.getItems();
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

    protected void toggleContextMenu() {
        if (!contextMenu.isShowing()) {
            contextMenu.setAnchorLocation(getAnchorLocation());
            Bounds bounds = localToScreen(getBoundsInLocal());
            double x = openToTheRight ? bounds.getMinX() : bounds.getMaxX();
            double y = openUpwards ? bounds.getMinY() - 3 : bounds.getMaxY() + 3;
            contextMenu.show(this, x, y);
        } else {
            contextMenu.hide();
        }
    }

    protected void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            getChildren().remove(buttonIcon);
            buttonIcon = newIcon;
            getChildren().add(buttonIcon);
        }
    }

    private void attachListeners() {
        setOnMouseClicked(e -> toggleContextMenu());
        setOnMouseExited(e -> updateIcon(contextMenu.isShowing() ? activeIcon : defaultIcon));
        setOnMouseEntered(e -> updateIcon(activeIcon));

        contextMenu.setOnShowing(e -> {
            getStyleClass().add("dropdown-menu-active");
            updateIcon(activeIcon);
            isMenuShowing.setValue(true);
            if (prefWidth != null && !contextMenu.getItems().isEmpty()
                    && contextMenu.getItems().getFirst() instanceof DropdownMenuItem) {
                updateMenuItemWidth();
            }
        });
        contextMenu.setOnHidden(e -> {
            getStyleClass().remove("dropdown-menu-active");
            updateIcon(isHover() ? activeIcon : defaultIcon);
            isMenuShowing.setValue(false);
        });

        sceneProperty().addListener(new WeakChangeListener<>(sceneListener));
        contextMenu.widthProperty().addListener(new WeakChangeListener<>(widthPropertyChangeListener));
    }

    private void updateMenuItemWidth() {
        for (MenuItem item : contextMenu.getItems()) {
            if (item instanceof DropdownBisqMenuItem dropdownBisqMenuItem) {
                dropdownBisqMenuItem.updateWidth(prefWidth);
            }
            if (item instanceof DropdownMenuItem dropdownMenuItem) {
                dropdownMenuItem.updateWidth(prefWidth);
            }
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
