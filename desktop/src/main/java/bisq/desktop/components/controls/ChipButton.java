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
import bisq.desktop.common.utils.ImageUtil;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

@Slf4j
public class ChipButton extends HBox {
    private final ToggleButton toggleButton;
    @Nullable
    private Runnable onActionHandler;

    public ChipButton(String text) {
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("chips-button");

        toggleButton = new ToggleButton();
        toggleButton.setText(text);
        toggleButton.setMouseTransparent(true);
        toggleButton.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(toggleButton);

        toggleButton.selectedProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) -> {
            removeStyles();
            if (newValue) {
                getStyleClass().add("chips-button-selected");
            }
            if (onActionHandler != null) {
                onActionHandler.run();
            }
        }).get());

        setOnMousePressed(e -> {
            removeStyles();
            if (toggleButton.isSelected()) {
                getStyleClass().add("chips-button-selected-pressed");
            } else {
                getStyleClass().add("chips-button-pressed");
            }
        });
        setOnMouseReleased(e -> toggleButton.setSelected(!toggleButton.isSelected()));
        setOnMouseEntered(e -> {
            removeStyles();
            if (toggleButton.isSelected()) {
                getStyleClass().add("chips-button-selected-hover");
            } else {
                getStyleClass().add("chips-button-hover");
            }
        });
        setOnMouseExited(e -> {
            removeStyles();
            if (toggleButton.isSelected()) {
                getStyleClass().add("chips-button-selected");
            }
        });

    }

    private void removeStyles() {
        getStyleClass().remove("chips-button-pressed");
        getStyleClass().remove("chips-button-selected-pressed");
        getStyleClass().remove("chips-button-hover");
        getStyleClass().remove("chips-button-selected");
        getStyleClass().remove("chips-button-selected-hover");
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
        labelIcon.setOpacity(0.6);
        HBox.setMargin(labelIcon, new Insets(-1, 0, 0, 20));
        getChildren().addAll(Spacer.fillHBox(), labelIcon);
        return labelIcon;
    }

    public ImageView setRightIcon(String iconId) {
        ImageView imageView = ImageUtil.getImageViewById(iconId);
        imageView.setCursor(Cursor.HAND);
        HBox.setMargin(imageView, new Insets(0, -5, 0, 20));
        getChildren().addAll(Spacer.fillHBox(), imageView);
        return imageView;
    }

    public void setSelected(boolean value) {
        toggleButton.setSelected(value);

    }

    public boolean isSelected() {
        return toggleButton.isSelected();
    }

    public void setOnAction(Runnable onActionHandler) {
        this.onActionHandler = onActionHandler;
    }
}