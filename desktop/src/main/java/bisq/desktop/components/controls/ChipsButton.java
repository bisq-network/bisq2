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

import bisq.desktop.common.utils.Icons;
import bisq.desktop.components.containers.Spacer;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
public class ChipsButton extends HBox {
    private final ToggleButton toggleButton;

    public ChipsButton(String text) {
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("chips-button");

        toggleButton = new ToggleButton();
        toggleButton.setText(text);
        toggleButton.setMouseTransparent(true);
        getChildren().add(toggleButton);

        toggleButton.selectedProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) -> {
            getStyleClass().remove("chips-button-hover");
            getStyleClass().remove("chips-button-selected-hover");
            if (newValue) {
                getStyleClass().add("chips-button-selected");
            } else {
                getStyleClass().remove("chips-button-selected");
            }
        }).get());

        setOnMouseClicked(e -> toggleButton.setSelected(!toggleButton.isSelected()));

        setOnMouseEntered(e -> {
            getStyleClass().remove("chips-button-selected");
            if (toggleButton.isSelected()) {
                getStyleClass().remove("chips-button-hover");
                getStyleClass().add("chips-button-selected-hover");
            } else {
                getStyleClass().remove("chips-button-selected-hover");
                getStyleClass().add("chips-button-hover");
            }
        });
        setOnMouseExited(e -> {
            getStyleClass().remove("chips-button-selected");
            getStyleClass().remove("chips-button-selected-hover");
            if (toggleButton.isSelected()) {
                getStyleClass().add("chips-button-selected");
            }
        });

    }

    public void setLeftIcon(Node icon) {
        getChildren().add(0, icon);
    }

    public void setRightIcon(Node icon) {
        getChildren().addAll(Spacer.fillHBox(), icon);
    }

    public void setTooltip(Tooltip tooltip) {
        Tooltip.install(this, tooltip);
    }

    public Label setRightIcon(AwesomeIcon awesomeIcon) {
        Label labelIcon = Icons.getIcon(awesomeIcon, "23");
        labelIcon.setCursor(Cursor.HAND);
        labelIcon.setOpacity(0.35);
        HBox.setMargin(labelIcon, new Insets(-1, 0, 0, 20));
        getChildren().addAll(Spacer.fillHBox(), labelIcon);
        return labelIcon;
    }

    public void setSelected(boolean value) {
        toggleButton.setSelected(value);
    }
}