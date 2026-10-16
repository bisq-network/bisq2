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

import java.util.regex.Pattern;

public class BitcoinUriAmountValidator extends ValidatorBase {
    // Accepts non-negative decimal amounts with up to 8 decimal places.
    // Rejects leading zeros, ".1", "1.", signs, and scientific notation.
    private static final Pattern BITCOIN_AMOUNT =
            Pattern.compile("^(?:0|[1-9]\\d*)(?:\\.\\d{1,8})?$");

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

        hasErrors.set(!BITCOIN_AMOUNT.matcher(text).matches());
    }
}
