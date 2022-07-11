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

import bisq.desktop.common.utils.validation.InputValidator;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
public class TextAreaBox extends Pane {
    @Getter
    private final TextArea textArea;
    @Getter
    private final Label descriptionLabel;
    @Setter
    private String prompt;
    private final StringProperty descriptionProperty = new SimpleStringProperty();

    public TextAreaBox(String description, String prompt) {
        this();

        this.prompt = prompt;
        descriptionProperty.set(description);
        textArea.setPromptText(this.prompt);
    }

    public TextAreaBox() {
        getStyleClass().add("bisq-input-box-top-pane");

        descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("bisq-input-box-description-label");
        descriptionLabel.setLayoutY(6);
        descriptionLabel.setLayoutX(9);

        textArea = new TextArea();
        textArea.setLayoutY(20);
        textArea.setLayoutX(0.5);
        textArea.getStyleClass().add("bisq-input-box-text-input");

       /* setMinHeight(50);
        setMaxHeight(50);*/
        getChildren().addAll(descriptionLabel, textArea);
        EasyBind.subscribe(prefWidthProperty(), w -> {
            double width = w.doubleValue();
            textArea.setMinWidth(width);
            textArea.setMaxWidth(width);
            setMinWidth(width);
            setMaxWidth(width);
        });

        EasyBind.subscribe(descriptionProperty, description -> {
            if (description != null) {
                descriptionLabel.setText(description.toUpperCase());
            }
        });
    }

    public void setBoxHeight(double height) {
        setMinHeight(height);
        setMaxHeight(height);
        textArea.setMinHeight(height - 30);
        textArea.setMaxHeight(height - 30);
    }

    public void requestFocus() {
        textArea.requestFocus();
    }

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        textArea.setOnMousePressed(handler);
    }

    public final StringProperty promptTextProperty() {
        return textArea.promptTextProperty();
    }

    public final StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public final StringProperty textProperty() {
        return textArea.textProperty();
    }

    public ReadOnlyBooleanProperty inputTextFieldFocusedProperty() {
        return textArea.focusedProperty();
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String value) {
        textArea.setText(value);
    }

    public void setValidator(InputValidator validator) {
        //todo
    }

    public void setDescription(String description) {
        descriptionProperty.set(description);
    }
}