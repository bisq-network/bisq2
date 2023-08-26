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

package bisq.desktop.overlay.bisq_easy.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BigAmountInput extends AmountInput {

    private static final double TEXT_INPUT_PREF_WIDTH = 250;
    private static final String BIG_TEXT_INPUT_ID = "base-amount-text-field";
    private static final String SMALL_TEXT_INPUT_ID = "base-amount-text-field-small";
    private static final int TEXT_LENGTH_THRESHOLD = 6;

    public BigAmountInput(boolean isBaseCurrency) {
        super(isBaseCurrency);
        this.controller.setView(new BigAmountInputView(controller.model, controller));
    }

    private static class BigAmountInputView extends View {

        protected BigAmountInputView(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        public void initView() {
            HBox.setMargin(textInput, new Insets(0, 0, 0, -30));
            root.setAlignment(Pos.BASELINE_CENTER);
            root.setSpacing(10);
        }

        @Override
        protected TextField createTextInput() {
            var textInput = new TextField();
            textInput.setPrefWidth(TEXT_INPUT_PREF_WIDTH);
            textInput.setId(BIG_TEXT_INPUT_ID);
            textInput.setAlignment(Pos.BASELINE_RIGHT);
            textInput.setPadding(new Insets(0, 0, 5, 0));
            return textInput;
        }

        @Override
        protected Label createCodeLabel() {
            var codeLabel = new Label();
            codeLabel.setPadding(new Insets(0, 0, 0, 0));
            codeLabel.getStyleClass().add("bisq-text-9");
            codeLabel.setAlignment(Pos.BASELINE_LEFT);
            return codeLabel;
        }

        @Override
        protected void adjustTextFieldStyle() {
            if (textInput.getText().length() > TEXT_LENGTH_THRESHOLD) {
                textInput.setId(SMALL_TEXT_INPUT_ID);
            } else {
                textInput.setId(BIG_TEXT_INPUT_ID);
            }
        }
    }
}