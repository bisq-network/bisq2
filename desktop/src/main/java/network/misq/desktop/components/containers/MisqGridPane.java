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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import network.misq.common.data.Pair;
import network.misq.desktop.components.controls.MisqLabel;
import network.misq.desktop.components.controls.MisqTextArea;
import network.misq.desktop.components.controls.MisqTextField;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.desktop.components.table.TableItem;
import network.misq.desktop.layout.Layout;

public class MisqGridPane extends GridPane {
    public MisqGridPane() {
        setVgap(5);
        setHgap(5);
        setPadding(Layout.INSETS);
    }

    public MisqLabel startSection(String text) {
        MisqLabel label = new MisqLabel(text);
        label.setLayoutX(4);
        label.setLayoutY(-8);
        label.setPadding(new Insets(0, 7, 0, 5));
        label.getStyleClass().add("titled-group-bg-label-active");
        GridPane.setMargin(label, new Insets(0, 0, 15, 0));
        GridPane.setRowIndex(label, getRowCount());
        GridPane.setColumnIndex(label, 0);
        GridPane.setColumnSpan(label, 2);
        getChildren().add(label);
        return label;
    }

    public void endSection() {
        addHSpacer();
        // todo add some separator UI element (line)
    }

    public Region addHSpacerMax() {
        Region region = new Region();
        GridPane.setVgrow(region, Priority.ALWAYS);
        GridPane.setRowIndex(region, getRowCount());
        getChildren().add(region);
        return region;
    }

    public Region addHSpacer() {
        return addHSpacer(Layout.SPACING);
    }

    public Region addHSpacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        GridPane.setRowIndex(region, getRowCount());
        getChildren().add(region);
        return region;
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
        GridPane.setRowIndex(textField, getRowCount());
        GridPane.setColumnIndex(textField, 1);
        GridPane.setMargin(textField, new Insets(0, 0, 15, 0));
        getChildren().addAll(textField);
        return textField;
    }

    public MisqTextArea addTextArea(String labelText, StringProperty textFieldText) {
        MisqTextArea textArea = addTextArea(labelText);
        textArea.textProperty().bind(textFieldText);
        return textArea;
    }

    public MisqTextArea addTextArea(String labelText) {
        MisqTextArea textArea = new MisqTextArea();
        textArea.setWrapText(true);
        textArea.setPromptText(labelText);
        GridPane.setRowIndex(textArea, getRowCount());
        GridPane.setColumnIndex(textArea, 1);
        GridPane.setMargin(textArea, new Insets(0, 0, 15, 0));
        getChildren().addAll(textArea);
        return textArea;
    }

    public Pair<Button, Label> addButton(String text) {
        Button button = new Button(text);
        Label label = new Label();
        label.setPadding(new Insets(5, 0, 0, 0));

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(button, label);
        GridPane.setRowIndex(hBox, getRowCount());
        GridPane.setColumnIndex(hBox, 1);
        getChildren().add(hBox);
        GridPane.setMargin(hBox, new Insets(0, 0, 15, 0));
        return new Pair<>(button, label);
    }

    public Pair<Button, Label> addButton(String label, Runnable handler) {
        Pair<Button, Label> pair = addButton(label);
        pair.first().setOnAction(e -> handler.run());
        return pair;
    }

    public void addTableView(MisqTableView<? extends TableItem> tableView) {
        GridPane.setRowIndex(tableView, getRowCount());
        GridPane.setColumnIndex(tableView, 1);
        GridPane.setColumnSpan(tableView, getColumnCount());
        getChildren().add(tableView);
    }


}