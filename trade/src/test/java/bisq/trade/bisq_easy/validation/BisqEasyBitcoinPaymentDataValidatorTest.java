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
import bisq.common.validation.BitcoinAddressValidation;
import bisq.common.validation.LightningInvoiceValidation;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyBitcoinPaymentDataValidatorTest {
    // A bech32m address of 62 characters, the longest address
    private static final String LONGEST_ADDRESS = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0";

    @Test
    void acceptsLongestAddress() {
        assertEquals(BitcoinAddressValidation.MAX_LENGTH, LONGEST_ADDRESS.length());
        assertDoesNotThrow(() -> BisqEasyBitcoinPaymentDataValidator.validate(LONGEST_ADDRESS, BitcoinPaymentRail.MAIN_CHAIN));
    }

    @Test
    void rejectsTooLongAddressAsExpectedFailure() {
        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> BisqEasyBitcoinPaymentDataValidator.validate(LONGEST_ADDRESS + "0", BitcoinPaymentRail.MAIN_CHAIN));

        assertEquals(TradeProtocolFailure.INVALID_BITCOIN_PAYMENT_DATA, exception.getTradeProtocolFailure());
        assertEquals("Bitcoin address length must not be longer than 62", exception.getMessage());
    }

    @Test
    void rejectsEmptyData() {
        assertTrue(BisqEasyBitcoinPaymentDataValidator.findError("", BitcoinPaymentRail.MAIN_CHAIN).isPresent());
        assertTrue(BisqEasyBitcoinPaymentDataValidator.findError("", BitcoinPaymentRail.LN).isPresent());
    }

    @Test
    void lightningInvoiceHasItsOwnLimit() {
        assertTrue(BisqEasyBitcoinPaymentDataValidator.findError(LONGEST_ADDRESS + LONGEST_ADDRESS, BitcoinPaymentRail.LN).isEmpty());
        assertTrue(BisqEasyBitcoinPaymentDataValidator.findError("l".repeat(LightningInvoiceValidation.MAX_LENGTH + 1),
                BitcoinPaymentRail.LN).isPresent());
    }
}
