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

import bisq.desktop.common.Transitions;
import javafx.animation.FadeTransition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

// Derived from JfxBadge
@Slf4j
@DefaultProperty(value = "control")
public class Badge extends StackPane {
    @Getter
    private final Group badge;
    @Getter
    private final Label label;
    @Getter
    private final StackPane badgePane;
    @Nullable
    @Getter
    protected Node control;
    @Getter
    @Setter
    private boolean enabled = true;
    protected ObjectProperty<Pos> position = new SimpleObjectProperty<>(Pos.TOP_RIGHT);
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private final FadeTransition transition;

    @Getter
    private boolean useAnimation;

    public Badge() {
        this(null, Pos.TOP_RIGHT);
    }

    public Badge(Node control) {
        this(control, Pos.TOP_RIGHT);
    }

    public Badge(Pos position) {
        this(null, position);
    }

    public Badge(Node control, Pos position) {
        label = new Label();

        badgePane = new StackPane();
        badgePane.getStyleClass().add("badge-pane");
        badgePane.getChildren().add(label);

        badge = new Group();
        badge.getChildren().add(badgePane);
        badge.setOpacity(0);

        transition = new FadeTransition(Duration.millis(Transitions.DEFAULT_DURATION), badge);
        transition.setFromValue(0);
        transition.setToValue(1.0);
        transition.setCycleCount(1);
        transition.setAutoReverse(true);

        useAnimation = Transitions.getUseAnimations();

        getChildren().add(badge);
        getStyleClass().add("bisq-badge");

        setLabelColor("-fx-light-text-color");
        setPosition(position);
        setControl(control);

        // Using weak listeners here was not safe. Some update did not get processed.
        // As we do not reference any external object there should not be any risk for causing a memory leak.
        text.addListener((observable, oldValue, newValue) -> {
            if (oldValue == null || !oldValue.equals(newValue)) {
                refreshBadge();
            }
        });
    }

    public void setUseAnimation(boolean useAnimation) {
        if (Transitions.getUseAnimations()) {
            this.useAnimation = useAnimation;
        }
    }

    // For unknown reasons the color of the style class is not applied in certain context (when used in list items)
    // when applying the styleClass.
    // Setting the style via `setStyle` works (taken from `.bisq-badge .badge-pane .label`).
    public void setLabelColor(String color) {
        label.setStyle("-fx-font-size: 0.85em; " +
                "-fx-text-fill: " + color + " !important; " +
                "-fx-font-family: \"IBM Plex Sans SemiBold\";");
    }

    public void setLabelStyle(String style) {
        label.setStyle(style);
    }

    private void refreshBadge() {
        int textLength = text.get() != null ? text.get().length() : 0;
        boolean hasDisplay = label.getGraphic() != null || textLength > 0;
        boolean show = enabled && hasDisplay;
        badge.setVisible(show);
        badge.setManaged(show);
        if (show) {
            label.setText(text.get());
            double prefWidth = (textLength - 1) * 7.5 + 15;
            badgePane.setPrefWidth(prefWidth);
            if (useAnimation) {
                transition.play();
            } else {
                badge.setOpacity(1);
            }
        }
    }

    /***************************************************************************
     * * Setters / Getters * *
     **************************************************************************/

    public void setControl(Node control) {
        if (control != null) {
            this.control = control;
            getChildren().add(0, control);

            // if the control got resized the badge must be reset
            if (control instanceof Region) {
                ((Region) control).widthProperty().addListener((o, oldVal, newVal) -> refreshBadge());
                ((Region) control).heightProperty().addListener((o, oldVal, newVal) -> refreshBadge());
            }
        }
    }

    public void setTooltip(String tooltip) {
        if (tooltip != null) {
            Tooltip.install(badgePane, new BisqTooltip(tooltip));
        }
    }

    public void setTooltip(Tooltip tooltip) {
        if (tooltip != null) {
            Tooltip.install(badgePane, tooltip);
        }
    }

    public Pos getPosition() {
        return position.get();
    }

    public void setPosition(Pos position) {
        if (position == null) {
            position = Pos.TOP_RIGHT;
        }
        this.position.set(position);
        StackPane.setAlignment(badge, position);
    }

    public ObjectProperty<Pos> positionProperty() {
        return this.position;
    }

    public final String getText() {
        return text.get();
    }

    public final void setText(String value) {
        text.set(value);
    }

    public final StringProperty textProperty() {
        return text;
    }

    public void setBadgeInsets(Insets insets) {
        StackPane.setMargin(badge, insets);
    }
}
