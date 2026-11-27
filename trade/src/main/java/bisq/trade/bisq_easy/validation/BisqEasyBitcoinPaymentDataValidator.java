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

package bisq.trade.bisq_easy.validation;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.common.util.StringUtils;
import bisq.common.validation.BitcoinAddressValidation;
import bisq.common.validation.LightningInvoiceValidation;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;

import java.util.Optional;

/**
 * The limits of the bitcoin payment data which the seller's node accepts from the buyer. The buyer's node applies the
 * same limits before it sends the data, so that a mistyped address does not fail the trade at the seller (#5210).
 * See docs/specifications/trade/bisq-easy-bitcoin-payment-data.md.
 */
public final class BisqEasyBitcoinPaymentDataValidator {
    private BisqEasyBitcoinPaymentDataValidator() {
    }

    public static int getMaxLength(BitcoinPaymentRail paymentRail) {
        // We do not check for the min. length as we do not enforce the validation in the UI
        return paymentRail == BitcoinPaymentRail.MAIN_CHAIN
                ? BitcoinAddressValidation.MAX_LENGTH
                : LightningInvoiceValidation.MAX_LENGTH;
    }

    public static void validate(String bitcoinPaymentData, BitcoinPaymentRail paymentRail) {
        Optional<String> error = findError(bitcoinPaymentData, paymentRail);
        if (error.isPresent()) {
            throw new TradeProtocolException(error.get(), TradeProtocolFailure.INVALID_BITCOIN_PAYMENT_DATA);
        }
    }

    // Returns the reason why the seller's node would not accept the data
    public static Optional<String> findError(String bitcoinPaymentData, BitcoinPaymentRail paymentRail) {
        if (StringUtils.isEmpty(bitcoinPaymentData)) {
            return Optional.of("Bitcoin payment data must not be empty");
        }
        int maxLength = getMaxLength(paymentRail);
        if (bitcoinPaymentData.length() > maxLength) {
            return Optional.of(paymentRail == BitcoinPaymentRail.MAIN_CHAIN
                    ? "Bitcoin address length must not be longer than " + maxLength
                    : "Lightning invoice length must not be longer than " + maxLength);
        }
        return Optional.empty();
    }
}
