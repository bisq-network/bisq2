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

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.utils.validation.InputValidator;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

@Slf4j
public class MaterialTextField extends Pane {
    private final Region bg, line, selectionLine;
    private final Label descriptionLabel;
    private final TextField inputTextField;
    private final Label helpLabel;
    private final StringProperty promptProperty = new SimpleStringProperty();
    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty helpProperty = new SimpleStringProperty();

    public MaterialTextField() {
        this(null, null, null);
    }

    public MaterialTextField(String description) {
        this(description, null, null);
    }

    public MaterialTextField(String description, String prompt) {
        this(description, prompt, null);
    }

    public MaterialTextField(@Nullable String description, @Nullable String prompt, @Nullable String help) {
        promptProperty.set(prompt);
        descriptionProperty.set(description);
        helpProperty.set(help);

        bg = new Region();
        bg.setMinHeight(56);
        bg.setMaxHeight(56);
        bg.setStyle("-fx-background-color: #383838; -fx-background-radius: 4 4 0 0;");
        bg.setOpacity(0.45);

        line = new Region();
        line.setLayoutY(55);
        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: -bisq-grey-9");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.setLayoutY(54);
        selectionLine.setPrefHeight(2);
        selectionLine.setStyle("-fx-background-color: -bisq-green");
        selectionLine.setMouseTransparent(true);

        descriptionLabel = new Label();
        descriptionLabel.setLayoutX(16);
        descriptionLabel.setMouseTransparent(true);
        descriptionLabel.setStyle("-fx-font-family: \"IBM Plex Sans Light\";");
        if (description != null) {
            descriptionLabel.setText(description);
        }

        inputTextField = new TextField();
        inputTextField.setLayoutX(6.5);
        inputTextField.setLayoutY(18);
        inputTextField.getStyleClass().add("material-text-field");
        if (prompt != null) {
            inputTextField.setPromptText(prompt);
        }

        helpLabel = new Label();
        helpLabel.setLayoutX(16);
        helpLabel.setLayoutY(59.5);
        helpLabel.setStyle("-fx-font-size: 0.95em; -fx-text-fill: -bisq-grey-9; -fx-font-family: \"IBM Plex Sans Light\";");
        helpLabel.setMouseTransparent(true);
        if (help != null) {
            helpLabel.setText(help);
        }

        getChildren().addAll(bg, line, selectionLine, descriptionLabel, inputTextField, helpLabel);

        widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                onWidthChanged((double) newValue)).get());

        inputTextField.focusedProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                onInputTextFieldFocus(newValue)).get());
        descriptionProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        promptProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        helpProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        inputTextField.editableProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                update()).get());

        bg.setOnMousePressed(e -> inputTextField.requestFocus());
        bg.setOnMouseEntered(e -> onMouseEntered());
        bg.setOnMouseExited(e -> onMouseExited());
        inputTextField.setOnMouseEntered(e -> onMouseEntered());
        inputTextField.setOnMouseExited(e -> onMouseExited());

        update();
    }

    private void onMouseEntered() {
        bg.setOpacity(inputTextField.isFocused() ? 1 : 0.55);
    }

    private void onMouseExited() {
        bg.setOpacity(inputTextField.isFocused() ? 1 : 0.45);
    }

    private void onInputTextFieldFocus(boolean focus) {
        if (focus) {
            bg.setOpacity(1);
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animateWidth(selectionLine, getWidth());
        } else {
            bg.setOpacity(0.45);
            Transitions.fadeOut(selectionLine, 200);
        }
        update();
    }

    private void onWidthChanged(double width) {
        if (width > 0) {
            bg.setPrefWidth(width);
            line.setPrefWidth(width);
            selectionLine.setPrefWidth(inputTextField.isFocused() ? width : 0);
            descriptionLabel.setPrefWidth(width - 2 * descriptionLabel.getLayoutX());
            inputTextField.setPrefWidth(width - 2 * inputTextField.getLayoutX());
            helpLabel.setPrefWidth(width - 2 * helpLabel.getLayoutX());
        }
    }

    private void update() {
        if (descriptionProperty.get() != null) {
            if (showInputTextField()) {
                Transitions.animateLayoutY(descriptionLabel, 6.5, Transitions.DEFAULT_DURATION / 6d, null);
            } else {

                Transitions.animateLayoutY(descriptionLabel, 16.5, Transitions.DEFAULT_DURATION / 6d, null);
            }
        }
        helpLabel.setVisible(helpProperty.get() != null);
        helpLabel.setManaged(helpProperty.get() != null);

        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");
        inputTextField.getStyleClass().remove("material-text-field-read-only");
        if (showInputTextField()) {
            descriptionLabel.getStyleClass().remove("material-text-field-description-big");
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
        } else {
            descriptionLabel.getStyleClass().remove("material-text-field-description-small");
            descriptionLabel.getStyleClass().add("material-text-field-description-big");
        }
        if (inputTextField.isFocused()) {
            descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
            descriptionLabel.getStyleClass().add("material-text-field-description-selected");
        } else {
            descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
            descriptionLabel.getStyleClass().add("material-text-field-description-deselected");
        }

        if (inputTextField.isEditable()) {
            bg.setMouseTransparent(false);
            inputTextField.setMouseTransparent(false);
        } else {
            bg.setMouseTransparent(true);
            inputTextField.setMouseTransparent(true);

            descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
            descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
            descriptionLabel.getStyleClass().add("material-text-field-description-read-only");
            inputTextField.getStyleClass().add("material-text-field-read-only");
        }
    }

    private boolean showInputTextField() {
        return promptProperty.get() != null || ((inputTextField.getText() != null &&
                !inputTextField.getText().isEmpty()) ||
                inputTextField.isFocused());
    }

    public void requestFocus() {
        inputTextField.requestFocus();
    }

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        inputTextField.setOnMousePressed(handler);
    }

    public final StringProperty promptTextProperty() {
        return inputTextField.promptTextProperty();
    }

    public final StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public final StringProperty textProperty() {
        return inputTextField.textProperty();
    }

    public ReadOnlyBooleanProperty inputTextFieldFocusedProperty() {
        return inputTextField.focusedProperty();
    }

    public String getText() {
        return inputTextField.getText();
    }

    public void setText(String value) {
        inputTextField.setText(value);
        update();
    }

    public void setValidator(InputValidator validator) {
        //todo
    }

    public void setDescription(String description) {
        descriptionProperty.set(description);
    }

    public void setEditable(boolean value) {
        inputTextField.setEditable(value);
    }

    public TextField getInputTextField() {
        return inputTextField;
    }

}