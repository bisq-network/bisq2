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

package bisq.desktop.components.containers;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTextFieldWithCopyIcon;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqGridPane extends GridPane {
    public BisqGridPane() {
        setVgap(25);
        setHgap(5);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        ColumnConstraints col2 = new ColumnConstraints();
        getColumnConstraints().addAll(col1, col2);
    }

    public Label startSection(String text) {
        Label label = new Label(text);
        label.setLayoutX(4);
        label.setLayoutY(-8);
        label.setPadding(new Insets(0, 7, 0, 5));
        label.getStyleClass().add("titled-group-bg-label-active");
        // GridPane.setMargin(label, new Insets(0, 0, 15, 0));
        GridPane.setRowIndex(label, getRowCount());
        GridPane.setColumnIndex(label, 0);
        GridPane.setColumnSpan(label, getColumnCount());
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

    public TextField addTextField(String labelText, StringProperty textFieldText) {
        TextField textField = addTextField(labelText, textFieldText.get());
        textField.textProperty().bindBidirectional(textFieldText);
        return textField;
    }

    public TextField addTextField(String labelText) {
        return addTextField(labelText, "");
    }

    public TextField addTextField(String labelText, String textFieldText) {
        TextField textField = new TextField(textFieldText);
        textField.setPromptText(labelText);
        GridPane.setRowIndex(textField, getRowCount());
        GridPane.setColumnIndex(textField, 0);
        GridPane.setColumnSpan(textField, getColumnCount());
        //GridPane.setMargin(textField, new Insets(0, 0, 15, 0));
        getChildren().addAll(textField);
        return textField;
    }

    public PasswordField addPasswordField(String labelText, StringProperty property) {
        PasswordField field = addPasswordField(labelText);
        field.textProperty().bindBidirectional(property);
        return field;
    }

    public PasswordField addPasswordField(String labelText) {
        PasswordField field = new PasswordField();
        field.setPromptText(labelText);
        GridPane.setRowIndex(field, getRowCount());
        GridPane.setColumnIndex(field, 0);
        GridPane.setColumnSpan(field, getColumnCount());
        //GridPane.setMargin(field, new Insets(0, 0, 15, 0));
        getChildren().addAll(field);
        return field;
    }

    public BisqTextFieldWithCopyIcon addTextFieldWithCopyIcon(String labelText, String textFieldText) {
        BisqTextFieldWithCopyIcon textField = new BisqTextFieldWithCopyIcon(textFieldText);
        textField.setPromptText(labelText);
        GridPane.setRowIndex(textField, getRowCount());
        GridPane.setColumnIndex(textField, 0);
        GridPane.setColumnSpan(textField, getColumnCount());
        // GridPane.setMargin(textField, new Insets(0, 0, 15, 0));
        getChildren().addAll(textField);
        return textField;
    }

    public BisqTextArea addTextArea(String labelText, StringProperty textFieldText) {
        BisqTextArea textArea = addTextArea(labelText);
        textArea.textProperty().bind(textFieldText);
        return textArea;
    }

    public BisqTextArea addTextArea(String labelText) {
        BisqTextArea textArea = new BisqTextArea();
        textArea.setWrapText(true);
        textArea.setPromptText(labelText);
        GridPane.setRowIndex(textArea, getRowCount());
        GridPane.setColumnIndex(textArea, 1);
        // GridPane.setMargin(textArea, new Insets(0, 0, 15, 0));
        getChildren().addAll(textArea);
        return textArea;
    }

    public Button addButton(String text) {
        Button button = new Button(text);
        GridPane.setRowIndex(button, getRowCount());
        GridPane.setColumnIndex(button, 0);
        getChildren().add(button);
        // GridPane.setMargin(button, new Insets(0, 0, 15, 0));
        return button;
    }

    public Button addButton(String label, Runnable handler) {
        Button button = addButton(label);
        button.setOnAction(e -> handler.run());
        return button;
    }

    public Pair<Button, Label> addButtonWithLabel(String text) {
        Button button = new Button(text);
        Label label = new Label();
        label.setPadding(new Insets(5, 0, 0, 0));

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(button, label);
        GridPane.setRowIndex(hBox, getRowCount());
        GridPane.setColumnIndex(hBox, 0);
        getChildren().add(hBox);
        // GridPane.setMargin(hBox, new Insets(0, 0, 15, 0));
        return new Pair<>(button, label);
    }

    public Pair<Button, Label> addButtonWithLabel(String label, Runnable handler) {
        Pair<Button, Label> pair = addButtonWithLabel(label);
        pair.getFirst().setOnAction(e -> handler.run());
        return pair;
    }

    public void addTableView(BisqTableView<? extends TableItem> tableView) {
        GridPane.setRowIndex(tableView, getRowCount());
        GridPane.setColumnIndex(tableView, 0);
        int columnCount = getColumnCount();
        GridPane.setColumnSpan(tableView, columnCount);
        getChildren().add(tableView);
    }

    public <T> AutoCompleteComboBox<T> addComboBox(ObservableList<T> items) {
        AutoCompleteComboBox<T> comboBox = new AutoCompleteComboBox<>(items);
        GridPane.setRowIndex(comboBox, getRowCount());
        GridPane.setColumnIndex(comboBox, 0);
        getChildren().add(comboBox);
        // GridPane.setMargin(comboBox, new Insets(0, 0, 15, 0));
        return comboBox;
    }
}