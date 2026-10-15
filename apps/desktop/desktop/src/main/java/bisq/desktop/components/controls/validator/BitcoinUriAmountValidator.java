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

import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;

public class BitcoinUriAmountValidator extends ValidatorBase {

    public BitcoinUriAmountValidator() {
        super(Res.get("validation.invalidBitcoinAmount"));
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
            BigDecimal amount = new BigDecimal(text);
            boolean valid = amount.signum() >= 0
                    && amount.scale() <= 8
                    && !text.contains("e")
                    && !text.contains("E");
            hasErrors.set(!valid);
        } catch (NumberFormatException ex) {
            hasErrors.set(true);
        }
    }
}
