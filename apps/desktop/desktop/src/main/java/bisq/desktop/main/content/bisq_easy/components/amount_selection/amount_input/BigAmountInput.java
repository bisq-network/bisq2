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

package bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BigAmountInput extends AmountInput {
    private static final String BIG_TEXT_INPUT_ID = "base-amount-text-field";
    private static final String SMALL_TEXT_INPUT_ID = "base-amount-text-field-small";
    private static final String VERY_SMALL_TEXT_INPUT_ID = "base-amount-text-field-very-small";
    private static final int TEXT_LENGTH_THRESHOLD = 9;

    public BigAmountInput(boolean isBaseCurrency, boolean showCurrencyCode) {
        super(isBaseCurrency, showCurrencyCode);

        controller.setView(new BigAmountInputView(controller.model, controller));
    }

    private static class BigAmountInputView extends View {
        protected BigAmountInputView(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        public void initView() {
            root.setSpacing(10);
            root.getStyleClass().add("big-amount-input");
        }

        @Override
        protected TextField createTextInput() {
            var textInput = new TextField();
            textInput.setId(BIG_TEXT_INPUT_ID);
            textInput.getStyleClass().add("text-input");
            return textInput;
        }

        @Override
        protected Label createCodeLabel() {
            var codeLabel = new Label();
            codeLabel.getStyleClass().add("currency-code");
            return codeLabel;
        }

        @Override
        protected void adjustTextFieldStyle() {
            if (model.useVerySmallText.get()) {
                textInput.setId(VERY_SMALL_TEXT_INPUT_ID);
            } else {
                textInput.setId(textInput.getText().length() > TEXT_LENGTH_THRESHOLD
                        ? SMALL_TEXT_INPUT_ID
                        : BIG_TEXT_INPUT_ID);
            }
        }
    }
}
