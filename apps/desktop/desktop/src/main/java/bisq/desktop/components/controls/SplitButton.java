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

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class SplitButton extends DropdownMenu {
    @Getter
    private final Label label = new Label();
    private final VBox labelBox = new VBox(label);
    private final HBox button = new HBox();

    public SplitButton(String defaultIconId, String activeIconId) {
        super(defaultIconId, activeIconId, false);

        double height = 18;
        setupButton(height);
        setupLabel(height);
        setupLabelBox(height);
        setupSplitButtonMenu(height);
        initialize();
    }

    public SplitButton() {
        this("chevron-drop-menu-grey", "chevron-drop-menu-white");
    }

    private void initialize() {
        setOnMouseClicked(null);
        setOnMouseExited(null);
        setOnMouseEntered(null);

        button.setOnMouseClicked(e -> toggleContextMenu());
        button.setOnMouseExited(e -> updateIcon(contextMenu.isShowing() ? activeIcon : defaultIcon));
        button.setOnMouseEntered(e -> updateIcon(activeIcon));
    }

    public void setupButton(double height) {
        button.getChildren().setAll(buttonIcon);
        button.getStyleClass().add("split-button-dropdown-button");
        button.setMaxHeight(height);
        button.setMinHeight(height);
        button.setPrefHeight(height);
        double width = 24;
        button.setMaxWidth(width);
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setAlignment(Pos.CENTER_LEFT);
    }

    public void setupLabel(double height) {
        label.getStyleClass().add("split-button-dropdown-label");
        label.setMaxHeight(height);
        label.setMinHeight(height);
        label.setPrefHeight(height);
    }

    public void setupLabelBox(double height) {
        labelBox.getStyleClass().add("split-button-dropdown-label-box");
        labelBox.setMaxHeight(height);
        labelBox.setMinHeight(height);
        labelBox.setPrefHeight(height);
    }

    private void setupSplitButtonMenu(double height) {
        setMaxHeight(height);
        setMinHeight(height);
        setPrefHeight(height);
        getChildren().setAll(labelBox, button);
        setSpacing(1);
        getStyleClass().add("split-button");
    }

    @Override
    protected void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            buttonIcon = newIcon;
            button.getChildren().setAll(buttonIcon);
        }
    }
}
