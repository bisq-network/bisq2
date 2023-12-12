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

package bisq.desktop.common.view;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Styles;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.Badge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.Getter;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class TabButton extends Pane implements Toggle {
    public static final double BADGE_PADDING = 7.5;

    private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    @Getter
    private final Label label;
    @Getter
    private final NavigationTarget navigationTarget;
    private final Styles styles;
    private ImageView icon;
    private ImageView iconSelected;
    private ImageView iconHover;
    @Getter
    private final Badge numMessagesBadge;
    private final ChangeListener<Number> labelWidthListener;

    public TabButton(String title, ToggleGroup toggleGroup,
                     NavigationTarget navigationTarget,
                     Styles styles,
                     @Nullable String iconId) {
        this.navigationTarget = navigationTarget;
        this.styles = styles;

        if (iconId != null) {
            this.icon = ImageUtil.getImageViewById(iconId + "-grey");
            this.iconSelected = ImageUtil.getImageViewById(iconId);
            this.iconHover = ImageUtil.getImageViewById(iconId + "-white");
        }

        setCursor(Cursor.HAND);

        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);

        selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));

        label = new Label(title.toUpperCase());
        label.setPadding(new Insets(7, 0, 0, 0));
        label.setMouseTransparent(true);

        label.getStyleClass().addAll("bisq-tab-button-label", styles.getNormal());
        label.setGraphic(icon);

        numMessagesBadge = new Badge();
        numMessagesBadge.setLayoutY(7.5);

        label.widthProperty().addListener((observable, oldValue, newValue) -> {
            numMessagesBadge.setLayoutX(label.getWidth() + BADGE_PADDING);
        });

        getChildren().addAll(label, numMessagesBadge);

        hoverProperty().addListener(new WeakReference<>((ChangeListener<Boolean>) (ov, wasHovered, isHovered) -> {
            if (isSelected()) return;
            Layout.chooseStyleClass(label, styles.getHoover(), styles.getNormal(), isHovered);
            label.setGraphic(isHovered ? iconHover : icon);
        }).get());

        labelWidthListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue.doubleValue() > 0) {
                    numMessagesBadge.setLayoutX(label.getWidth() + BADGE_PADDING);
                    UIThread.runOnNextRenderFrame(() -> label.widthProperty().removeListener(labelWidthListener));
                }
            }
        };
        label.widthProperty().addListener(labelWidthListener);
    }

    public final void setOnAction(Runnable handler) {
        if (handler == null) {
            setOnMouseClicked(null);
        } else {
            setOnMouseClicked(e -> handler.run());
        }
    }

    public void setNumNotifications(long numNotifications) {
        if (numNotifications == 0) {
            numMessagesBadge.setText("");
            return;
        }
        numMessagesBadge.setText(String.valueOf(numNotifications));
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
        Layout.chooseStyleClass(label, styles.getSelected(), styles.getNormal(), selected);
        label.setGraphic(selected ? iconSelected : icon);
    }
}