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

import javafx.animation.FadeTransition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private final Label labelControl;
    @Getter
    private final StackPane badgePane;
    @Nullable
    @Getter
    protected Node control;
    @Getter
    @Setter
    private boolean enabled = true;
    protected ObjectProperty<Pos> position = new SimpleObjectProperty<>();
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private FadeTransition transition;

    public Badge() {
        this(null);
    }

    public Badge(Node control) {
        this(control, Pos.TOP_RIGHT);
    }

    public Badge(Node control, Pos position) {
        labelControl = new Label();

        badgePane = new StackPane();
        badgePane.getStyleClass().add("badge-pane");
        badgePane.getChildren().add(labelControl);

        badge = new Group();
        badge.getChildren().add(badgePane);

        transition = new FadeTransition(Duration.millis(666), badge);
        transition.setFromValue(0);
        transition.setToValue(1.0);
        transition.setCycleCount(1);
        transition.setAutoReverse(true);

        getChildren().add(badge);
        getStyleClass().add("bisq-badge");

        setPosition(position);
        setControl(control);
    }

    private void refreshBadge() {
        int textLength = text.get().length();
        boolean show = enabled && textLength > 0;
        badge.setVisible(show);
        badge.setManaged(show);
        if (show) {
            labelControl.setText(text.get());
            double prefWidth = (textLength - 1) * 7.5 + 15;
            badgePane.setPrefWidth(prefWidth);
            transition.play();
        }
    }

    /***************************************************************************
     * * Setters / Getters * *
     **************************************************************************/

    public void setControl(Node control) {
        if (control != null) {
            this.control = control;
            getChildren().add(control);

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

    public Pos getPosition() {
        return position == null ? Pos.TOP_RIGHT : position.get();
    }

    public void setPosition(Pos position) {
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
        refreshBadge();
    }

    public final StringProperty textProperty() {
        return text;
    }
}
