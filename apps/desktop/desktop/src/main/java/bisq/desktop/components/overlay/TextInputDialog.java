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

package bisq.desktop.components.overlay;

import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class TextInputDialog extends Popup {
    private static final int DEFAULT_MAX_LENGTH = 1000;
    private static final int PREFERRED_WIDTH = 520;

    private final TextArea inputTextArea;
    private final Label counterLabel;
    private final Label descriptionLabel;
    private final Button confirmButton;
    private final Button cancelButton;

    private Consumer<String> inputConsumer;
    private Predicate<String> validator;
    private int maxLength = DEFAULT_MAX_LENGTH;
    private String confirmButtonText = Res.get("action.save");
    private String cancelButtonText = Res.get("action.cancel");
    private boolean showCounter = true;

    public TextInputDialog(String headline) {
        this(headline, null);
    }

    public TextInputDialog(String headline, String fieldLabel) {
        if (headline != null) {
            headline(headline);
        }

        hideCloseButton();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(15, 20, 10, 14));
        gridPane.setPrefWidth(PREFERRED_WIDTH);

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        columnConstraints.setFillWidth(true);
        columnConstraints.setPercentWidth(100);
        gridPane.getColumnConstraints().add(columnConstraints);

        descriptionLabel = new Label();
        descriptionLabel.setWrapText(true);
        descriptionLabel.setManaged(false);
        descriptionLabel.setVisible(false);
        descriptionLabel.setMaxWidth(Double.MAX_VALUE);

        inputTextArea = new TextArea(fieldLabel);
        inputTextArea.setMaxWidth(Double.MAX_VALUE);

        counterLabel = new Label(Res.get("component.TextInputDialog.text.characters.remaining", maxLength));
        counterLabel.setManaged(showCounter);
        counterLabel.setVisible(showCounter);

        inputTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > maxLength) {
                int caretPosition = inputTextArea.getCaretPosition();
                double scrollLeft = inputTextArea.getScrollLeft();
                double scrollTop = inputTextArea.getScrollTop();

                inputTextArea.setText(oldValue);

                // Try to restore the caret and scrollbars position
                int newCaretPos = Math.min(caretPosition, oldValue.length());
                inputTextArea.positionCaret(newCaretPos);

                inputTextArea.setScrollLeft(scrollLeft);
                inputTextArea.setScrollTop(scrollTop);

                return;
            }
            updateRemainingCharsCount();
        });

        confirmButton = new Button(confirmButtonText);
        cancelButton = new Button(cancelButtonText);

        confirmButton.getStyleClass().add("action-button");
        confirmButton.setDefaultButton(true);

        HBox buttonBox = new HBox(10, confirmButton, cancelButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        int rowIndex = 0;
        gridPane.add(descriptionLabel, 0, rowIndex++);
        gridPane.add(inputTextArea, 0, rowIndex++);
        gridPane.add(counterLabel, 0, rowIndex++);
        gridPane.add(buttonBox, 0, rowIndex);

        confirmButton.setOnAction(e -> {
            String input = inputTextArea.getText();
            if (validator != null && !validator.test(input)) {
                return;
            }

            if (inputConsumer != null) {
                inputConsumer.accept(input);
            }
            hide();
        });

        cancelButton.setOnAction(e -> hide());

        GridPane.setHgrow(inputTextArea, Priority.ALWAYS);
        GridPane.setHgrow(buttonBox, Priority.ALWAYS);
        GridPane.setHgrow(descriptionLabel, Priority.ALWAYS);
        GridPane.setHgrow(counterLabel, Priority.ALWAYS);

        content(gridPane);
    }

    public TextInputDialog setPromptText(String prompt) {
        inputTextArea.setPromptText(prompt);
        return this;
    }

    public TextInputDialog setInputFieldDescription(String instructions) {
        if (instructions != null && !instructions.isEmpty()) {
            descriptionLabel.setText(instructions);
            descriptionLabel.setManaged(true);
            descriptionLabel.setVisible(true);
        }
        return this;
    }

    public TextInputDialog setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        updateRemainingCharsCount();
        return this;
    }

    public TextInputDialog setConfirmButtonText(String text) {
        confirmButtonText = text;
        if (confirmButton != null) {
            confirmButton.setText(text);
        }
        return this;
    }

    public TextInputDialog setCancelButtonText(String text) {
        cancelButtonText = text;
        if (cancelButton != null) {
            cancelButton.setText(text);
        }
        return this;
    }

    public TextInputDialog showCounter(boolean show) {
        showCounter = show;
        if (counterLabel != null) {
            counterLabel.setManaged(show);
            counterLabel.setVisible(show);
        }
        return this;
    }

    public TextInputDialog validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    public TextInputDialog onResult(Consumer<String> inputConsumer) {
        this.inputConsumer = inputConsumer;
        return this;
    }

    public static Optional<String> show(String headline, String fieldLabel) {
        TextInputDialog dialog = new TextInputDialog(headline, fieldLabel);
        String[] result = {null};

        dialog.onResult(text -> result[0] = text);
        dialog.show();

        return Optional.ofNullable(result[0]);
    }

    private void updateRemainingCharsCount() {
        int remaining = maxLength - (inputTextArea.getText() != null ? inputTextArea.getText().length() : 0);
        counterLabel.setText(Res.get("component.TextInputDialog.text.characters.remaining", remaining));
    }
}