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

import bisq.common.validation.BitcoinDataValidation;

/**
 * A validator for Bitcoin string format data. Initially thought for txIDs and wallet addresses
 */
public abstract class BitcoinDataValidator extends ValidatorBase {

    public enum BitcoinDataType {
        WALLET_ADDRESS,
        TX_ID
    }

    @Override
    protected void eval() {
        hasErrors.set(!isValid());
    }

    /**
     * @return the data to evaluate at the time of evaluation (eval() gets called)
     */
    protected abstract String getData();

    /**
     *
     * @return type of validation to use
     */
    protected abstract BitcoinDataType getType();

    private boolean isValid() {
        String data = getData();
        if (data == null) {
            return false;
        }
        return switch (getType()) {
            case WALLET_ADDRESS -> BitcoinDataValidation.validateWalletAddressHash(getData());
            case TX_ID -> BitcoinDataValidation.validateTransactionId(getData());
        };
    }
}
