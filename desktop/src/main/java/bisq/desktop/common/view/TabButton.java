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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import lombok.Getter;

public class TabButton extends Pane implements Toggle {
    private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    private final Label label;
    @Getter
    private final NavigationTarget navigationTarget;

    public TabButton(String title, ToggleGroup toggleGroup, NavigationTarget navigationTarget) {
        this.navigationTarget = navigationTarget;

        setCursor(Cursor.HAND);
        
        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);
        
        selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));

        label = new Label(title.toUpperCase());
        label.setPadding(new Insets(12, 0, 0, 0));
        label.setMouseTransparent(true);

        label.setStyle("-fx-text-fill: -bisq-grey-8; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.4em;");

        getChildren().addAll(label);
    }

    public final void setOnAction(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
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

        if (selected) {
            label.setStyle("-fx-text-fill: -bisq-green; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.4em;");
        } else {
            label.setStyle("-fx-text-fill: -bisq-grey-8; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.4em;");
        }
    }
}