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
    void exactConversionsMatchThePlainOnesForRepresentableValues() {
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Coin btc = Coin.asBtcFromFaceValue(1.5);
        assertEquals(priceQuote.toQuoteSideMonetary(btc).value, priceQuote.toQuoteSideMonetaryExact(btc).value);
        Fiat usd = Fiat.fromFaceValue(12345.67, "USD");
        assertEquals(priceQuote.toBaseSideMonetary(usd).value, priceQuote.toBaseSideMonetaryExact(usd).value);
    }

    @Test
    void exactQuoteSideConversionFailsOnOverflowInsteadOfWrapping() {
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        Coin btc = Coin.asBtcFromValue(1_844_674_407_371_955_162L);
        // The plain conversion wraps into a plausible looking value.
        assertTrue(priceQuote.toQuoteSideMonetary(btc).value > 0);
        assertThrows(ArithmeticException.class, () -> priceQuote.toQuoteSideMonetaryExact(btc));
    }

    @Test
    void exactBaseSideConversionFailsOnOverflowInsteadOfWrapping() {
        // A near-zero price makes the base side amount of a large fiat amount overflow:
        // 10M USD (value 1e11) shifted by the base precision (1e8) exceeds a long.
        PriceQuote priceQuote = new PriceQuote(1, Coin.asBtcFromValue(100_000_000L), Fiat.fromFaceValue(0.000001, "USD"));
        Fiat usd = Fiat.fromFaceValue(10_000_000, "USD");
        assertTrue(priceQuote.toBaseSideMonetary(usd).value < 0);
        assertThrows(ArithmeticException.class, () -> priceQuote.toBaseSideMonetaryExact(usd));
    }

    @Test
    void clampRejectsAQuoteFromADifferentMarket() {
        // A BTC/EUR quote must not be silently clamped against BTC/USD limits, even when its
        // numeric value falls inside them.
        PriceQuote btcEur = PriceQuote.fromFiatPrice(50_000, "EUR");
        PriceQuote usdMin = PriceQuote.fromFiatPrice(40_000, "USD");
        PriceQuote usdMax = PriceQuote.fromFiatPrice(60_000, "USD");
        assertThrows(IllegalArgumentException.class, () -> btcEur.clamp(usdMin, usdMax));
    }

    @Test
    void clampWithinTheSameMarket() {
        PriceQuote min = PriceQuote.fromFiatPrice(40_000, "USD");
        PriceQuote max = PriceQuote.fromFiatPrice(60_000, "USD");

        PriceQuote inRange = PriceQuote.fromFiatPrice(50_000, "USD");
        assertSame(inRange, inRange.clamp(min, max));

        assertEquals(min.getQuoteSideMonetary().getValue(),
                PriceQuote.fromFiatPrice(30_000, "USD").clamp(min, max).getQuoteSideMonetary().getValue());
        assertEquals(max.getQuoteSideMonetary().getValue(),
                PriceQuote.fromFiatPrice(70_000, "USD").clamp(min, max).getQuoteSideMonetary().getValue());
    }
}
