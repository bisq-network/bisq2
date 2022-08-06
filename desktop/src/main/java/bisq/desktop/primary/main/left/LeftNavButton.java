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

package bisq.desktop.primary.main.left;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.stream.Stream;

@Slf4j
class LeftNavButton extends Pane implements Toggle {
    protected final static double LABEL_X_POS_EXPANDED = 56;
    static final int HEIGHT = 40;

    @Getter
    protected final NavigationTarget navigationTarget;
    @Getter
    private final boolean hasSubmenu;
    protected final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    protected final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    protected final BooleanProperty highlightedProperty = new SimpleBooleanProperty();
    protected final Label label;
    protected final Tooltip tooltip;

    @Nullable
    protected final ImageView icon;
    @Nullable
    protected final ImageView iconActive;
    @Nullable
    protected final ImageView iconHover;
    @Nullable
    private Node arrowDownIcon;

    @Getter
    private BooleanProperty wasSelected = new SimpleBooleanProperty();

    LeftNavButton(String title,
                  @Nullable String iconId,
                  ToggleGroup toggleGroup,
                  NavigationTarget navigationTarget,
                  boolean hasSubmenu) {
        this.icon = iconId != null ? ImageUtil.getImageViewById(iconId) : null;
        this.iconActive = iconId != null ? ImageUtil.getImageViewById(iconId + "-active") : null;
        this.iconHover = iconId != null ? ImageUtil.getImageViewById(iconId + "-hover") : null;
        this.navigationTarget = navigationTarget;
        this.hasSubmenu = hasSubmenu;

        setMinHeight(calculateHeight());
        setMaxHeight(calculateHeight());
        setCursor(Cursor.HAND);

        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);

        tooltip = new BisqTooltip(title);
        if (iconId != null) {
            Stream.of(icon, iconActive, iconHover).forEach(icon -> {
                icon.setMouseTransparent(true);
                icon.setLayoutX(25);
                icon.setLayoutY(10.5);
            });
            getChildren().add(icon);
        }

        label = new Label(title);
        label.setLayoutX(LABEL_X_POS_EXPANDED);
        label.setLayoutY((calculateHeight() - 21) * 0.5);
        label.setMouseTransparent(true);

        getChildren().add(label);

        if (hasSubmenu) {
            arrowDownIcon = BisqIconButton.createIconButton("nav-arrow-down");
            arrowDownIcon.setOpacity(0.4);

            arrowDownIcon.setLayoutX(LeftNavView.EXPANDED_WIDTH - 20);
            arrowDownIcon.setLayoutY(10);

            getChildren().addAll(arrowDownIcon);
        }

        applyStyle();

        hoverProperty().addListener((ov, wasHovered, isHovered) -> {
            if (isSelected() || isHighlighted()) return;
            Layout.chooseStyleClass(label, "bisq-text-white", "bisq-text-grey-9", isHovered);
            if (icon != null) {
                getChildren().set(0, isHovered ? iconHover : icon);
            }
        });
    }

    protected void applyStyle() {
        boolean isHighlighted = isSelected() || isHighlighted();
        Layout.addStyleClass(this, "bisq-dark-bg");
        Layout.toggleStyleClass(label, "bisq-text-logo-green", isSelected());
        Layout.toggleStyleClass(label, "bisq-text-white", isHighlighted());
        Layout.toggleStyleClass(label, "bisq-text-grey-9", !isHighlighted);

        if (icon != null) {
            getChildren().set(0, isSelected() ? iconActive : isHighlighted ? iconHover : icon);
        }
    }

    public final void setOnAction(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
    }

    public void setMenuExpanded(boolean menuExpanded, int duration) {
        if (menuExpanded) {
            Tooltip.uninstall(this, tooltip);
            label.setVisible(true);
            label.setManaged(true);
            Transitions.fadeIn(label, duration);
            if (hasSubmenu) {
                Transitions.fadeIn(arrowDownIcon, 3 * duration, 0.4, null);
            }
        } else {
            Tooltip.install(this, tooltip);
            if (hasSubmenu) {
                Transitions.fadeOut(arrowDownIcon, duration / 2);
            }
            Transitions.fadeOut(label, duration, () -> {
                label.setVisible(false);
                label.setManaged(false);
            });
        }
    }

    protected int calculateHeight() {
        return HEIGHT;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Toggle implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ToggleGroup getToggleGroup() {
        return toggleGroupProperty.get();
    }

    @Override
    public void setToggleGroup(ToggleGroup toggleGroup) {
        toggleGroupProperty.set(toggleGroup);
    }

    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroupProperty;
    }

    @Override
    public boolean isSelected() {
        return selectedProperty.get();
    }

    @Override
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    @Override
    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
        applyStyle();
    }

    public boolean isHighlighted() {
        return highlightedProperty.get();
    }

    public void setHighlighted(boolean highlighted) {
        highlightedProperty.set(highlighted);
        applyStyle();
        arrowDownIcon.setOpacity(highlighted ? 1 : 0.4);
    }
}