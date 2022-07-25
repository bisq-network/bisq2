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
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
class LeftNavButton extends Pane implements Toggle {
    protected final static double LABEL_X_POS_EXPANDED = 56;
    static final int HEIGHT = 40;

    @Getter
    protected final NavigationTarget navigationTarget;
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
    private Node expandedIcon, autoCollapseIcon;

    @Getter
    private final BooleanProperty isAutoCollapse = new SimpleBooleanProperty();
    @Getter
    @Setter
    private boolean wasSelected;
    Optional<CookieKey> cookieKey = Optional.empty();

    LeftNavButton(String title,
                  @Nullable String iconId,
                  ToggleGroup toggleGroup,
                  NavigationTarget navigationTarget,
                  boolean hasSubmenu) {
        this.icon = iconId != null ? ImageUtil.getImageViewById(iconId) : null;
        this.iconActive = iconId != null ? ImageUtil.getImageViewById(iconId + "-active") : null;
        this.iconHover = iconId != null ? ImageUtil.getImageViewById(iconId + "-hover") : null;
        this.navigationTarget = navigationTarget;

        if (navigationTarget == NavigationTarget.TRADE_OVERVIEW) {
            cookieKey = Optional.of(CookieKey.TRADE_OVERVIEW_PIN);
        } else if (navigationTarget == NavigationTarget.ACADEMY) {
            cookieKey = Optional.of(CookieKey.ACADEMY_PIN);
        }

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
            cookieKey.flatMap(cookieKey -> SettingsService.getInstance().getCookie().asBoolean(cookieKey))
                    .ifPresent(isAutoCollapse::set);

            expandedIcon = BisqIconButton.createIconButton("nav-expand");
            autoCollapseIcon = BisqIconButton.createIconButton("nav-auto-collapse");

            expandedIcon.setVisible(false);
            expandedIcon.setManaged(false);
            expandedIcon.setLayoutX(LeftNavView.EXPANDED_WIDTH - 20);
            expandedIcon.setLayoutY(10);
            Tooltip.install(expandedIcon, new BisqTooltip(Res.get("navigation.expandedIcon.tooltip")));
            expandedIcon.setOnMouseClicked(e -> {
                isAutoCollapse.set(true);
                cookieKey.ifPresent(cookieKey -> SettingsService.getInstance().setCookie(cookieKey, true));
                expandedIcon.setVisible(false);
                expandedIcon.setManaged(false);
                autoCollapseIcon.setVisible(true);
                autoCollapseIcon.setManaged(true);
            });

            autoCollapseIcon.setVisible(false);
            autoCollapseIcon.setManaged(false);
            autoCollapseIcon.setLayoutX(LeftNavView.EXPANDED_WIDTH - 20);
            autoCollapseIcon.setLayoutY(12);
            Tooltip.install(autoCollapseIcon, new BisqTooltip(Res.get("navigation.autoCollapseIcon.tooltip")));
            autoCollapseIcon.setOnMouseClicked(e -> {
                isAutoCollapse.set(false);
                cookieKey.ifPresent(cookieKey -> SettingsService.getInstance().setCookie(cookieKey, false));
                autoCollapseIcon.setVisible(false);
                autoCollapseIcon.setManaged(false);
                expandedIcon.setVisible(true);
                expandedIcon.setManaged(true);
            });

            getChildren().addAll(autoCollapseIcon, expandedIcon);
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
        Layout.addStyleClass(this, "bisq-darkest-bg");
        // Layout.chooseStyleClass(this, "bisq-dark-bg", "bisq-darkest-bg", isHighlighted);
        Layout.toggleStyleClass(label, "bisq-text-logo-green", isSelected());
        Layout.toggleStyleClass(label, "bisq-text-white", isHighlighted());
        Layout.toggleStyleClass(label, "bisq-text-grey-9", !isHighlighted);

        if (icon != null) {
            getChildren().set(0, isSelected() ? iconActive : isHighlighted ? iconHover : icon);
        }
        if (autoCollapseIcon != null && expandedIcon != null) {
            if (wasSelected) {
                boolean value = isAutoCollapse.get();
                if (value) {
                    expandedIcon.setVisible(false);
                    expandedIcon.setManaged(false);
                    autoCollapseIcon.setVisible(isHighlighted);
                    autoCollapseIcon.setManaged(isHighlighted);
                } else {
                    expandedIcon.setVisible(true);
                    expandedIcon.setManaged(true);
                    autoCollapseIcon.setVisible(false);
                    autoCollapseIcon.setManaged(false);
                }
            }
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
            if (autoCollapseIcon != null) {
                Transitions.fadeIn(autoCollapseIcon, 3 * duration);
            }
            if (expandedIcon != null) {
                Transitions.fadeIn(expandedIcon, 3 * duration);
            }
        } else {
            Tooltip.install(this, tooltip);
            if (autoCollapseIcon != null) {
                Transitions.fadeOut(autoCollapseIcon, duration / 2);
            }
            if (expandedIcon != null) {
                Transitions.fadeOut(expandedIcon, duration / 2);
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
    }
}