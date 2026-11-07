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

package bisq.desktop.main.content.wallet.send;

import bisq.common.monetary.Coin;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;
import java.util.Optional;

public class WalletSendAmountValidator extends ValidatorBase {
    private static final BigDecimal MAX_BTC = BigDecimal.valueOf(21_000_000);

    public WalletSendAmountValidator() {
        super(Res.get("validation.invalidBitcoinAmount"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        hasErrors.set(parse(textField.getText()).isEmpty());
    }

    // A positive BTC amount with at most 8 decimals and no exponent; a comma counts as the decimal separator.
    static Optional<Coin> parse(String text) {
        if (StringUtils.isEmpty(text)) {
            return Optional.empty();
        }
        String trimmed = text.trim().replace(',', '.');
        if (trimmed.contains("e") || trimmed.contains("E")) {
            return Optional.empty();
        }
        try {
            BigDecimal amount = new BigDecimal(trimmed).stripTrailingZeros();
            if (amount.signum() <= 0 || amount.scale() > 8 || amount.compareTo(MAX_BTC) > 0) {
                return Optional.empty();
            }
            return Optional.of(Coin.asBtcFromValue(amount.movePointRight(8).longValueExact()));
        } catch (ArithmeticException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
