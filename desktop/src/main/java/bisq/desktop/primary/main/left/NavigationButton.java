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

import bisq.desktop.common.utils.Icons;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

class NavigationButton extends Pane implements Toggle {
    static final int HEIGHT = 45;
    private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
    private final Label icon;
    private final Label label;
    private final Tooltip tooltip;
    private boolean menuExpanded;

    NavigationButton(String title, AwesomeIcon awesomeIcon, ToggleGroup toggleGroup) {
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);

        setCursor(Cursor.HAND);
        tooltip = new Tooltip(title);
        tooltip.setShowDelay(Duration.millis(200));

        setToggleGroup(toggleGroup);
        toggleGroup.getToggles().add(this);
        selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));

        icon = Icons.getIcon(awesomeIcon, "16");
        icon.setMouseTransparent(true);
        icon.setLayoutX(17);
        icon.setLayoutY(12);

        label = new Label(title.toUpperCase());
        label.setLayoutX(50);
        label.setLayoutY(13);
        label.setMouseTransparent(true);
        label.setStyle("-fx-text-fill: -bs-rd-font-dark-gray");

        getChildren().addAll(icon, label);
    }

    public final void setOnAction(Runnable handler) {
        setOnMouseClicked(e -> handler.run());
    }

    public void setMenuExpanded(boolean menuExpanded, int width) {
        this.menuExpanded = menuExpanded;
        setMinWidth(width);
        setMaxWidth(width);
        label.setVisible(menuExpanded);
        label.setManaged(menuExpanded);
        if (menuExpanded) {
            Tooltip.uninstall(this, tooltip);
        } else {
            Tooltip.install(this, tooltip);
        }
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
            setStyle("-fx-background-color: -bisq-menu-selected; -fx-background-radius: 3;");
            label.setStyle("-fx-text-fill: -fx-light-text-color;");
            icon.setOpacity(1);
        } else {
            setStyle("-fx-background-color: -bisq-menu-bg; -fx-background-radius: 3;");
            label.setStyle("-fx-text-fill: -fx-mid-text-color;");
            icon.setOpacity(0.7);
        }
    }
}