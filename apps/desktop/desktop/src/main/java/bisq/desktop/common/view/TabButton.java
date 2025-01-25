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
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class TabButton extends Pane implements Toggle {
    public static final double BADGE_PADDING = 7.5;

    private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    @Getter
    private final Label label = new Label();
    @Getter
    private final NavigationTarget navigationTarget;
    @Getter
    private final Styles styles;
    @Getter
    private final Button clearNotificationsButton;
    private final Pane numMessagesBadgeBox;
    private Timeline animateIn, animateOut;
    private ImageView icon;
    private ImageView iconSelected;
    private ImageView iconHover;
    @Getter
    private final Badge numMessagesBadge;

    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> labelWidthListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (newValue.doubleValue() > 0) {
                numMessagesBadgeBox.setLayoutX(label.getWidth() + BADGE_PADDING);
            }
        }
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> hoverListener = (ov, wasHovered, isHovered) -> {
        if (isSelected()) return;
        Layout.chooseStyleClass(label, getStyles().getHoover(), getStyles().getNormal(), isHovered);
        label.setGraphic(isHovered ? iconHover : icon);
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> selectedListener = (ov, oldValue, newValue) -> {
        label.setMouseTransparent(newValue);
        setCursor(newValue ? null : Cursor.HAND);
    };

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

        label.setText(title.toUpperCase());
        label.setPadding(new Insets(7, 0, 0, 0));
        label.setMouseTransparent(true);

        label.getStyleClass().addAll("bisq-tab-button-label", styles.getNormal());
        label.setGraphic(icon);

        numMessagesBadge = new Badge();

        clearNotificationsButton = BisqIconButton.createIconButton("clear-notifications");
        clearNotificationsButton.setOpacity(0);
        Pane clearNotificationsButtonPane = new Pane(clearNotificationsButton);
        clearNotificationsButtonPane.setLayoutX(-1.5);
        clearNotificationsButtonPane.setLayoutY(-1.5);
        numMessagesBadgeBox = new Pane(clearNotificationsButtonPane, numMessagesBadge);
        numMessagesBadgeBox.setVisible(false);
        numMessagesBadgeBox.setManaged(false);
        numMessagesBadgeBox.setLayoutY(BADGE_PADDING);

        getChildren().addAll(label, numMessagesBadgeBox);

        selectedProperty().addListener(new WeakChangeListener<>(selectedListener));
        hoverProperty().addListener(new WeakChangeListener<>(hoverListener));
        label.widthProperty().addListener(new WeakChangeListener<>(labelWidthListener));
    }

    public final void setOnAction(Runnable handler) {
        if (handler == null) {
            setOnMouseClicked(null);
        } else {
            setOnMouseClicked(e -> handler.run());
        }
    }

    public void setNumNotifications(long numNotifications) {
        boolean hasNotifications = numNotifications > 0;
        numMessagesBadgeBox.setVisible(hasNotifications);
        numMessagesBadgeBox.setManaged(hasNotifications);
        if (!hasNotifications) {
            return;
        }
        numMessagesBadge.setText(String.valueOf(numNotifications));
    }

    public void showClearButton(String tooltip, Runnable clearNotificationsHandler) {
        clearNotificationsButton.setTooltip(new BisqTooltip(tooltip));
        clearNotificationsButton.setOnAction(e -> clearNotificationsHandler.run());

        numMessagesBadgeBox.setOnMouseEntered(e -> {
            if (animateOut != null) {
                animateOut.stop();
                animateOut = null;
            }
            animateIn = animate(clearNotificationsButton, Transitions.DEFAULT_DURATION / 2, () -> {
                UIThread.runOnNextRenderFrame(() -> {
                    animateIn = null;
                });
            }, true);
        });
        numMessagesBadgeBox.setOnMouseExited(e -> {
            if (animateIn != null) {
                animateIn.stop();
                animateIn = null;
            }
            animateOut = animate(clearNotificationsButton, Transitions.DEFAULT_DURATION / 4, () -> {
                UIThread.runOnNextRenderFrame(() -> {
                    animateOut = null;
                });
            }, false);
        });
    }

    public void disposeClearButton() {
        clearNotificationsButton.setTooltip(null);
        clearNotificationsButton.setOnAction(null);
        numMessagesBadgeBox.setOnMouseEntered(null);
        numMessagesBadgeBox.setOnMouseExited(null);
        if (animateIn != null) {
            animateIn.stop();
            animateIn = null;
        }
        if (animateOut != null) {
            animateOut.stop();
            animateOut = null;
        }
    }


    /* --------------------------------------------------------------------- */
    // Toggle implementation
    /* --------------------------------------------------------------------- */

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

    private Timeline animate(Region node, int duration, Runnable onFinishedHandler, boolean animateIn) {
        Timeline timeline = new Timeline();
        double rightX = numMessagesBadge.getWidth() + 8;
        double end = animateIn ? rightX : 0;
        double opacityEnd = animateIn ? 1 : 0;
        if (Transitions.getUseAnimations()) {
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            double start = animateIn ? 0 : rightX;
            double opacityStart = animateIn ? 0 : 1;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(node.opacityProperty(), opacityStart, Interpolator.EASE_IN),
                    new KeyValue(node.translateXProperty(), start, Interpolator.EASE_IN)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(node.opacityProperty(), opacityEnd, Interpolator.EASE_OUT),
                    new KeyValue(node.translateXProperty(), end, Interpolator.EASE_OUT)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else {
            node.setTranslateX(end);
            node.setOpacity(opacityEnd);
            onFinishedHandler.run();
        }
        return timeline;
    }

}