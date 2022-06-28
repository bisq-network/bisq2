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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// Derived from JfxBadge
@Slf4j
@DefaultProperty(value = "control")
public class Badge extends StackPane {

    private Group badge;
    protected Node control;
    private boolean enabled = true;
    @Setter
    private String tooltip;

    public Badge() {
        this(null);
    }

    public Badge(Node control) {
        this(control, Pos.TOP_RIGHT);
    }

    public Badge(Node control, Pos pos) {
        getStyleClass().add("bisq-badge");
        setPosition(pos);
        setControl(control);
        position.addListener((o, oldVal, newVal) -> StackPane.setAlignment(badge, newVal));
    }

    /***************************************************************************
     * * Setters / Getters * *
     **************************************************************************/

    public void setControl(Node control) {
        if (control != null) {
            this.control = control;
            this.badge = new Group();
            this.getChildren().add(control);
            this.getChildren().add(badge);

            // if the control got resized the badge must be rest
            if (control instanceof Region) {
                ((Region) control).widthProperty().addListener((o, oldVal, newVal) -> refreshBadge());
                ((Region) control).heightProperty().addListener((o, oldVal, newVal) -> refreshBadge());
            }
            text.addListener((o, oldVal, newVal) -> refreshBadge());
        }
    }

    public Node getControl() {
        return this.control;
    }

    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    public void refreshBadge() {
        badge.getChildren().clear();
        int textLength = text.get().length();
        if (enabled && textLength > 0) {
            Label labelControl = new Label(text.get());
            labelControl.getStyleClass().add("badge-label");

            StackPane badgePane = new StackPane();
            badgePane.getStyleClass().add("badge-pane");
            badgePane.getChildren().add(labelControl);
            badgePane.setPrefWidth(textLength * 15d);

            //todo not working yet ?
            if (tooltip != null) {
                Tooltip.uninstall(badgePane, new Tooltip(tooltip));
            }

            badge.getChildren().add(badgePane);
            StackPane.setAlignment(badge, getPosition());

            FadeTransition ft = new FadeTransition(Duration.millis(666), badge);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.setCycleCount(1);
            ft.setAutoReverse(true);
            ft.play();
        }
    }


    protected ObjectProperty<Pos> position = new SimpleObjectProperty<>();

    public Pos getPosition() {
        return position == null ? Pos.TOP_RIGHT : position.get();
    }

    public ObjectProperty<Pos> positionProperty() {
        return this.position;
    }

    public void setPosition(Pos position) {
        this.position.set(position);
    }

    private final SimpleStringProperty text = new SimpleStringProperty("");

    public final String getText() {
        return text.get();
    }

    public final void setText(String value) {
        text.set(value);
    }

    public final StringProperty textProperty() {
        return text;
    }
}
