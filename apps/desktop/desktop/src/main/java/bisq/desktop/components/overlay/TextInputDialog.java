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

import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.validator.ValidatorBase;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Slf4j
public class TextInputDialog extends Popup {
    private final MaterialTextArea inputTextArea = new MaterialTextArea();
    @Nullable
    private Consumer<String> inputHandler;

    public TextInputDialog(String headline) {
        this(headline, "");
    }

    public TextInputDialog(String headline, String text) {
        inputTextArea.setMinHeight(100);
        inputTextArea.setMaxHeight(inputTextArea.getMinHeight());

        content(inputTextArea);

        if (headline != null) {
            headline(headline);
        }
        // instruction("");

        doCloseOnAction = false;
        onAction(() -> {
            if (inputTextArea.validate()) {
                if (inputHandler != null) {
                    inputHandler.accept(inputTextArea.getText());
                }
                hide();
            }
        });
    }


    /* --------------------------------------------------------------------- */
    // Popup
    /* --------------------------------------------------------------------- */
    @Override
    protected void addContent() {
        super.addContent();
        GridPane.setMargin(content, new Insets(3, 0, 50, 0));
    }

    @Override
    protected TextInputDialog cast() {
        return this;
    }


    /* --------------------------------------------------------------------- */
    // MaterialTextArea delegates
    /* --------------------------------------------------------------------- */

    public TextInputDialog promptText(String value) {
        inputTextArea.setPromptText(value);
        return this;
    }

    public TextInputDialog description(String value) {
        inputTextArea.setDescription(value);
        return this;
    }

    public TextInputDialog helpText(String value) {
        inputTextArea.setHelpText(value);
        return this;
    }

    public TextInputDialog validator(ValidatorBase validator) {
        inputTextArea.setValidator(validator);
        return this;
    }

    public TextInputDialog onResult(Consumer<String> inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }
}