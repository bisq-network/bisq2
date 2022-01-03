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

package network.misq.desktop.components.containers;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import network.misq.common.data.Pair;
import network.misq.desktop.components.controls.AutoTooltipLabel;
import network.misq.desktop.components.controls.MisqTextField;

@Getter
public class Form extends MisqGridPane {
    private final AutoTooltipLabel label;

    public Form(String headline) {
        label = new AutoTooltipLabel(headline);
        label.setLayoutX(4);
        label.setLayoutY(-8);
        label.setPadding(new Insets(0, 7, 0, 5));
        label.getStyleClass().add("titled-group-bg-label-active");
        GridPane.setMargin(label, new Insets(0,0,15,0));
        GridPane.setRowIndex(label, 0);
        GridPane.setColumnIndex(label, 0);
        GridPane.setColumnSpan(label, 2);
        getChildren().add(label);
    }

    public MisqTextField addTextField(String labelText, StringProperty textFieldText) {
        MisqTextField textField = addTextField(labelText, textFieldText.get());
        textField.textProperty().bind(textFieldText);
        return textField;
    }

    public MisqTextField addTextField(String labelText, String textFieldText) {
        MisqTextField textField = new MisqTextField(textFieldText);
        textField.setLabelFloat(true);
        textField.setPromptText(labelText);
        GridPane.setMargin(textField, new Insets(0,0,15,0));
        GridPane.setRowIndex(textField, getRowCount() + 1);
        GridPane.setColumnIndex(textField, 1);
        getChildren().addAll(textField);

        return textField;
    }

    public Pair<Button, Label> addButton(String text) {
        Button button = new Button(text);
        Label label = new Label();
        label.setPadding(new Insets(5, 0, 0, 0));

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(button, label);
        GridPane.setRowIndex(hBox, getRowCount() + 1);
        GridPane.setColumnIndex(hBox, 1);
        getChildren().add(hBox);
        return new Pair<>(button, label);
    }

    public Pair<Button, Label> addButton(String label, Runnable handler) {
        Pair<Button, Label> pair = addButton(label);
        pair.first().setOnAction(e -> handler.run());
        return pair;
    }
}

