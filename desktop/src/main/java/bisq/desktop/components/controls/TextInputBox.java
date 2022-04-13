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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
public class TextInputBox extends Pane {
    @Getter
    private final TextField inputTextField;
    private final Label descriptionLabel;
    @Setter
    private String prompt;
    private final StringProperty descriptionTextProperty = new SimpleStringProperty();

    //  private final Label promptLabel;
    public TextInputBox(String description, String prompt) {
        this();

        this.prompt = prompt;
        descriptionTextProperty.set(description);
        inputTextField.setPromptText(this.prompt);
    }

    public TextInputBox() {
        getStyleClass().add("bisq-input-box-top-pane");

        descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("bisq-input-box-description-label");
        descriptionLabel.setLayoutY(6);
        descriptionLabel.setLayoutX(9);

        inputTextField = new TextField();
        inputTextField.setLayoutY(17);
        inputTextField.setLayoutX(0.5);
        inputTextField.getStyleClass().add("bisq-input-box-text-input");

        setMinHeight(50);
        setMaxHeight(50);
        getChildren().addAll(descriptionLabel, inputTextField);
        EasyBind.subscribe(prefWidthProperty(), w -> {
            double width = w.doubleValue();
            inputTextField.setMinWidth(width - 50);
            inputTextField.setMaxWidth(width - 50);
            setMinWidth(width);
            setMaxWidth(width);
        });

        setPrefWidth(300);
        EasyBind.subscribe(descriptionTextProperty, description -> {
            if (description != null) {
                descriptionLabel.setText(description.toUpperCase());
            }
        });
        UIThread.runOnNextRenderFrame(this::requestFocus);
    }

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        inputTextField.setOnMousePressed(handler);
    }

    public final StringProperty promptTextProperty() {
        return inputTextField.promptTextProperty();
    }

    public final StringProperty descriptionTextProperty() {
        return descriptionTextProperty;
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
    }

    public void setValidator(MonetaryValidator validator) {
        //todo
    }
}