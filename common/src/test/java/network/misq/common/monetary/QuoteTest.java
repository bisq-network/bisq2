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

package network.misq.common.monetary;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class QuoteTest {
    @Test
    void testToQuoteMonetary() {
        Coin btc = Coin.asBtc(1.0);
        Quote quote = Quote.fromFiatPrice(50000, "USD");
        Monetary quoteMonetary = Quote.toQuoteMonetary(btc, quote);
        assertTrue(quoteMonetary instanceof Fiat);
        assertEquals(500000000, quoteMonetary.value);

        btc = Coin.asBtc(2.0);
        quote = Quote.fromFiatPrice(50000, "USD");
        quoteMonetary = Quote.toQuoteMonetary(btc, quote);
        assertEquals(1000000000, quoteMonetary.value);
    }

    @Test
    void testOffsetOf() {
        Quote marketQuote = Quote.fromFiatPrice(50000, "USD");
        Quote offerQuote = Quote.fromFiatPrice(50000, "USD");

        double offset = Quote.offsetOf(marketQuote, offerQuote);
        assertEquals(0, offset);

        offerQuote = Quote.fromFiatPrice(55000, "USD");
        offset = Quote.offsetOf(marketQuote, offerQuote);
        log.error("" + offset);

        assertEquals(0.1d, offset);

        offerQuote = Quote.fromFiatPrice(45000, "USD");
        offset = Quote.offsetOf(marketQuote, offerQuote);
        assertEquals(-0.1, offset);

        offerQuote = Quote.fromFiatPrice(100000, "USD");
        offset = Quote.offsetOf(marketQuote, offerQuote);
        assertEquals(1, offset); // 100% of marketQuote

        offerQuote = Quote.fromFiatPrice(150000, "USD");
        offset = Quote.offsetOf(marketQuote, offerQuote);
        assertEquals(2, offset); // 200% of marketQuote

        offerQuote = Quote.fromFiatPrice(0, "USD");
        offset = Quote.offsetOf(marketQuote, offerQuote);
        assertEquals(-1, offset);

        assertThrows(IllegalArgumentException.class,
                () -> Quote.offsetOf(Quote.fromFiatPrice(0, "USD"),
                        Quote.fromFiatPrice(50000, "USD")));
    }

    @Test
    void testFromMarketPriceOffset() {
        Quote marketQuote = Quote.fromFiatPrice(50000, "USD");

        Quote quote = Quote.fromMarketPriceOffset(marketQuote, 0);
        assertEquals(500000000, quote.getValue());
        assertEquals(4, quote.getSmallestUnitExponent());
        assertEquals("BTC/USD", quote.getQuoteCode());

        quote = Quote.fromMarketPriceOffset(marketQuote, 1);
        assertEquals(1000000000, quote.getValue());

        quote = Quote.fromMarketPriceOffset(marketQuote, -1);
        assertEquals(0, quote.getValue());

        quote = Quote.fromMarketPriceOffset(marketQuote, 0.1);
        assertEquals(550000000, quote.getValue());

        quote = Quote.fromMarketPriceOffset(marketQuote, -0.1);
        assertEquals(450000000, quote.getValue());
    }
}