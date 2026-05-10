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

package bisq.presentation.formatters;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmountFormatterTest {

    @Test
    @DisplayName("format")
    void format() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        Assertions.assertEquals("1,00000000", AmountFormatter.formatAmount(btc, Locale.GERMAN, false));
        assertEquals("1.00000000", AmountFormatter.formatAmount(btc, Locale.US, false));

        btc = Coin.asBtcFromFaceValue(20123456.1234);
        assertEquals("20123456.12340000", AmountFormatter.formatAmount(btc, Locale.US, false));

        Fiat usd = Fiat.fromFaceValue(51234.1234, "USD");
        assertEquals("51234.1234", AmountFormatter.formatAmount(usd, Locale.US, false));
        usd = Fiat.fromFaceValue(51234.56, "USD");
        assertEquals("51234.5600", AmountFormatter.formatAmount(usd, Locale.US, false));
    }

    @Test
    @DisplayName("format base and quote amount use monetary type precision")
    void format_base_and_quote_amount_use_monetary_type_precision() {
        Coin btc = Coin.asBtcFromFaceValue(1.12345678);
        Fiat usd = Fiat.fromFaceValue(1234.5678, "USD");

        assertEquals(AmountFormatter.formatAmount(btc, false), AmountFormatter.formatBaseAmount(btc));
        assertEquals(AmountFormatter.formatAmount(btc, false), AmountFormatter.formatQuoteAmount(btc));

        assertEquals(AmountFormatter.formatAmount(usd, true), AmountFormatter.formatBaseAmount(usd));
        assertEquals(AmountFormatter.formatAmount(usd, true), AmountFormatter.formatQuoteAmount(usd));
    }

    @Test
    @DisplayName("format base and quote amount with code use monetary type precision")
    void format_base_and_quote_amount_with_code_use_monetary_type_precision() {
        Coin btc = Coin.asBtcFromFaceValue(1.12345678);
        Fiat usd = Fiat.fromFaceValue(1234.5678, "USD");

        assertEquals(AmountFormatter.formatAmountWithCode(btc, false), AmountFormatter.formatBaseAmountWithCode(btc));
        assertEquals(AmountFormatter.formatAmountWithCode(btc, false), AmountFormatter.formatQuoteAmountWithCode(btc));

        assertEquals(AmountFormatter.formatAmountWithCode(usd, true), AmountFormatter.formatBaseAmountWithCode(usd));
        assertEquals(AmountFormatter.formatAmountWithCode(usd, true), AmountFormatter.formatQuoteAmountWithCode(usd));
    }

    @Test
    @DisplayName("USDC formats with 2 decimals like fiat, not 6 like generic crypto")
    void usdc_formats_with_two_decimals() {
        Coin usdc = Coin.fromFaceValue(1000.50, "USDC");

        assertEquals("1000.50", AmountFormatter.formatQuoteAmount(usdc));
        assertEquals("1000.50 USDC", AmountFormatter.formatQuoteAmountWithCode(usdc));

        assertEquals(2, usdc.getLowPrecision());
    }

    @Test
    @DisplayName("USDC formatAmountByMonetaryType uses low precision like fiat")
    void usdc_format_amount_by_monetary_type_uses_low_precision() {
        Coin usdc = Coin.fromFaceValue(42.50, "USDC");
        Fiat usd = Fiat.fromFaceValue(42.50, "USD");

        String usdcFormatted = AmountFormatter.formatAmountByMonetaryType(usdc);
        String usdFormatted = AmountFormatter.formatAmountByMonetaryType(usd);

        assertEquals("42.50", usdcFormatted);
        assertEquals("42.50", usdFormatted);
    }

    @Test
    @DisplayName("USDC full precision still available when explicitly requested")
    void usdc_full_precision_available_when_requested() {
        Coin usdc = Coin.fromFaceValue(1000.123456, "USDC");
        assertEquals("1000.123456", AmountFormatter.formatAmount(usdc, Locale.US, false));
    }

    @Test
    @DisplayName("Monetary.from dispatches USDC to Coin with correct lowPrecision")
    void monetary_from_usdc_has_correct_low_precision() {
        Monetary usdc = Monetary.from(1_000_000, "USDC");
        assertEquals(2, usdc.getLowPrecision());
        assertEquals(6, usdc.getPrecision());
    }

    @Test
    @DisplayName("BTC formatting unchanged -- still shows full precision")
    void btc_formatting_unchanged() {
        Coin btc = Coin.asBtcFromFaceValue(1.12345678);
        String formatted = AmountFormatter.formatQuoteAmount(btc);
        assertTrue(formatted.contains("1234"), "BTC should still show full 8-decimal precision");
    }
}
