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
import javafx.beans.property.StringProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.fxmisc.easybind.EasyBind;

public class TextInputBox extends Pane {
    private final TextField inputTextField;

    public TextInputBox(String title, String prompt) {
        getStyleClass().add("bisq-input-box-top-pane");

        Label topLabel = new Label(title.toUpperCase());
        topLabel.getStyleClass().add("bisq-input-box-top-label");
        topLabel.setLayoutY(10);
        topLabel.setLayoutX(14);

        Label promptLabel = new Label(prompt);
        promptLabel.getStyleClass().add("bisq-input-box-prompt-label");
        promptLabel.setLayoutY(33);
        promptLabel.setLayoutX(14);
        promptLabel.setCursor(Cursor.TEXT);

        inputTextField = new TextField();
        inputTextField.setLayoutY(28);
        inputTextField.setLayoutX(6);

        inputTextField.getStyleClass().add("bisq-input-box-text-input");
        inputTextField.setVisible(false);
        setOnMousePressed(e -> {
            inputTextField.setVisible(true);
            promptLabel.setVisible(false);
            UIThread.runOnNextRenderFrame(inputTextField::requestFocus);
        });

        setMinHeight(75);
        setMaxHeight(75);
        getChildren().addAll(topLabel, inputTextField, promptLabel);
        EasyBind.subscribe(prefWidthProperty(), w -> {
            double width = w.doubleValue();
            promptLabel.setMinWidth(width - 50);
            promptLabel.setMaxWidth(width - 50);
            inputTextField.setMinWidth(width - 50);
            inputTextField.setMaxWidth(width - 50);
            setMinWidth(width);
            setMaxWidth(width);
        });
    }

    public final StringProperty textProperty() {
        return inputTextField.textProperty();
    }
}