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

package bisq.desktop.components.controls.validator;

import bisq.common.monetary.Monetary;
import bisq.i18n.Res;
import bisq.presentation.parser.AmountParser;
import javafx.scene.control.TextInputControl;

/**
 * Validates an amount text with the same parser the payload creation uses, so that text which the
 * validator accepts cannot make the parse throw later. Blank text is valid.
 */
public class AmountRangeValidator extends ValidatorBase {
    private final String code;
    private final long minValue;
    private final long maxValue;

    /**
     * @param min Minimum amount as face value text, e.g. 0.001
     * @param max Maximum amount as face value text, e.g. 1
     */
    public AmountRangeValidator(String code, String min, String max) {
        super(Res.get("validation.invalidNumber"));
        this.code = code;
        this.minValue = AmountParser.parse(min, code).getValue();
        this.maxValue = AmountParser.parse(max, code).getValue();
    }

    @Override
    protected void eval() {
        if (!(srcControl.get() instanceof TextInputControl control)) {
            return;
        }
        String text = control.getText();
        if (text == null || text.isBlank()) {
            hasErrors.set(false);
            return;
        }
        try {
            Monetary amount = AmountParser.parse(text, code);
            hasErrors.set(amount.getValue() < minValue || amount.getValue() > maxValue);
        } catch (IllegalArgumentException e) {
            hasErrors.set(true);
        }
    }
}
