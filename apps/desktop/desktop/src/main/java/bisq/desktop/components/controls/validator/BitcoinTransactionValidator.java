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

import bisq.common.validation.BitcoinTransactionValidation;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

public class BitcoinTransactionValidator extends ValidatorBase {

    public BitcoinTransactionValidator(String message) {
        super(message);
    }

    public BitcoinTransactionValidator() {
        super(Res.get("validation.invalidBitcoinTransactionId"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String txId = textField.getText();
        if (txId != null && !txId.isEmpty()) {
            hasErrors.set(!BitcoinTransactionValidation.validateTransactionId(txId));
        } else {
            hasErrors.set(false);
        }
    }
}
