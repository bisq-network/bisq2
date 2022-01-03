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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import network.misq.common.data.Pair;

@Getter
public class Form extends MisqGridPane {
    private final Label headlineLabel;

    public Form(String headline) {
        headlineLabel = new Label(headline);
        GridPane.setRowIndex(headlineLabel, 0);
        GridPane.setColumnIndex(headlineLabel, 0);
        GridPane.setColumnSpan(headlineLabel, 2);
        getChildren().add(headlineLabel);
    }

    public Pair<Label, TextField> addLabelTextField(Pair<String, String> labelValuePair) {
        Label label = new Label(labelValuePair.first());
        int rowIndex = getRowCount() + 1;
        GridPane.setRowIndex(label, rowIndex);
        GridPane.setColumnIndex(label, 0);
        getChildren().add(label);

        TextField textField = new TextField(labelValuePair.second());
        GridPane.setRowIndex(textField, rowIndex);
        GridPane.setColumnIndex(textField, 1);
        getChildren().addAll(textField);

        return new Pair<>(label, textField);
    }
    public Pair<Button, Label> addButton(String text) {
        Button button = new Button(text);
        Label label = new Label();
        label.setPadding(new Insets(5,0,0,0));

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

