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

package bisq.common.monetary;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PriceQuoteTest {
    @Test
    void testToQuoteMonetary() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertInstanceOf(Fiat.class, quoteSideMonetary);
        assertEquals(500000000, quoteSideMonetary.value);

        btc = Coin.asBtcFromFaceValue(2.0);
        priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertEquals(1000000000, quoteSideMonetary.value);
    }

    @Test
    void toQuoteSideMonetaryTruncatesTowardZero() {
        // 333_333_333 quote units per BTC: 1 sat converts to 3.33 quote units.
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(33333.3333, "USD");
        assertEquals(3, priceQuote.toQuoteSideMonetary(Coin.asBtcFromValue(1)).value);
        assertEquals(-3, priceQuote.toQuoteSideMonetary(Coin.asBtcFromValue(-1)).value);
    }

    @Test
    void toBaseSideMonetaryRoundsHalfUp() {
        // 3 quote units per BTC: 1 quote unit converts to 33_333_333.33 sats.
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(0.0003, "USD");
        assertEquals(33_333_333, priceQuote.toBaseSideMonetary(Fiat.fromValue(1, "USD")).value);
        assertEquals(66_666_667, priceQuote.toBaseSideMonetary(Fiat.fromValue(2, "USD")).value);
    }

    @Test
    void toQuoteSideMonetaryThrowsOnOverflow() {
        // 2^60 sats at 100k USD/BTC: the quote side is about 1.15 * 10^19, beyond long range.
        // Wrapping produced a plausible-looking wrong amount instead of failing.
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        assertThrows(ArithmeticException.class, () -> priceQuote.toQuoteSideMonetary(Coin.asBtcFromValue(1L << 60)));
        assertThrows(ArithmeticException.class, () -> priceQuote.toQuoteSideMonetary(Coin.asBtcFromValue(-(1L << 60))));
    }

    @Test
    void toBaseSideMonetaryThrowsOnOverflow() {
        // Price value 1: the base side is the quote value times 10^8.
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(0.0001, "USD");
        assertThrows(ArithmeticException.class, () ->
                priceQuote.toBaseSideMonetary(Fiat.fromValue(Long.MAX_VALUE, "USD")));
        assertThrows(ArithmeticException.class, () ->
                priceQuote.toBaseSideMonetary(Fiat.fromValue(-Long.MAX_VALUE, "USD")));
    }

    @Test
    void fromThrowsOnOverflow() {
        // 1 sat of base against a Long.MAX_VALUE quote amount: the price value overflows.
        assertThrows(ArithmeticException.class, () ->
                PriceQuote.from(Coin.asBtcFromValue(1), Fiat.fromValue(Long.MAX_VALUE, "USD")));
    }
}