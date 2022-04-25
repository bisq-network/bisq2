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

import bisq.common.currency.FiatCurrencyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonetaryTest {
    @Test
    void testParse() {
        assertEquals(12345678, Coin.parse("0.12345678", "BTC", 8).getValue());
        assertEquals(12345678, Coin.parseBtc("0.12345678").getValue());
        assertEquals(12345678, Coin.parseBtc("0.123456789").getValue());
        assertEquals(123456780000L, Coin.parse("0.12345678", "XMR", 12).getValue());
        assertEquals(123456780000L, Coin.parseXmr("0.12345678").getValue());

        assertEquals(1234, Fiat.parse("0.1234", "USD", 4).getValue());
        assertEquals(1234, Fiat.parse("0.1234", "USD").getValue());
        assertEquals(1234, Fiat.parse("0.12341", "USD").getValue());

        FiatCurrencyRepository.initialize(Locale.US);
        assertEquals(1234567800, Fiat.parse("123456.78 USD").getValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            assertEquals(1234567800, Fiat.parse("123456.78USD").getValue());
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            assertEquals(1234567800, Fiat.parse("123456.78 XYZ").getValue());
        });

        assertEquals(12345678, Coin.parse("0.12345678 BTC").getValue());
        // We do not check the crypto-currency code as we do not want to be constrained
        assertEquals(12345678, Coin.parse("0.12345678 UNDEFINED").getValue());
        assertEquals(123456780000L, Coin.parse("0.12345678 XMR").getValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            assertEquals(1234567800, Coin.parse("123456.78 USD").getValue());
        });
    }

    @Test
    void testQuotes() {
        Coin btc = Coin.asBtc(1.0);
        Fiat usd = Fiat.of(50000d, "USD");
        Quote quote = Quote.of(btc, usd);
        assertEquals(500000000, quote.getValue());
        assertEquals(50000.0, quote.asDouble());
        assertEquals(4, quote.getPrecision());
        assertEquals("USD", quote.getQuoteMonetary().code);
        assertEquals("BTC", quote.getBaseMonetary().getCode());
        assertEquals("BTC/USD", quote.getMarket().getCurrencyCodes());

        usd = Fiat.of(50000.1249d, "USD");
        quote = Quote.of(btc, usd);
        assertEquals(500001249, quote.getValue());
        assertEquals(50000.1249, quote.asDouble());

        usd = Fiat.of(50000.1250d, "USD");
        quote = Quote.of(btc, usd);
        assertEquals(50000.1250, quote.asDouble());

        btc = Coin.asBtc(100000000);
        usd = Fiat.of(500001250, "USD");
        quote = Quote.of(btc, usd);
        assertEquals(500001250, quote.getValue());
        assertEquals(50000.1250, quote.asDouble());

        // btc - alt
        Coin xmr = Coin.asXmr(150d);
        btc = Coin.asBtc(1.0);
        quote = Quote.of(xmr, btc);
        assertEquals(666667, quote.getValue());
        assertEquals(0.00666667, quote.asDouble());
        assertEquals(8, quote.getPrecision());
        assertEquals("BTC", quote.getQuoteMonetary().code);
        assertEquals("XMR", quote.getBaseMonetary().getCode());
        assertEquals("XMR/BTC", quote.getMarket().getCurrencyCodes());

        xmr = Coin.asXmr(1d);
        btc = Coin.asBtc(0.00666667);
        quote = Quote.of(xmr, btc);
        assertEquals(666667, quote.getValue());
        assertEquals(0.00666667, quote.asDouble());
        assertEquals(8, quote.getPrecision());
        assertEquals("BTC", quote.getQuoteMonetary().code);
        assertEquals("XMR", quote.getBaseMonetary().getCode());
        assertEquals("XMR/BTC", quote.getMarket().getCurrencyCodes());

        // XMR/ETH
        xmr = Coin.asXmr(1d);     // 250
        Coin eth = Coin.of(0.1, "ETH"); //2500
        quote = Quote.of(xmr, eth);
        assertEquals(10000000, quote.getValue());
        assertEquals(0.1, quote.asDouble());
        assertEquals(8, quote.getPrecision());
        assertEquals("ETH", quote.getQuoteMonetary().code);
        assertEquals("XMR", quote.getBaseMonetary().getCode());
        assertEquals("XMR/ETH", quote.getMarket().getCurrencyCodes());

        // ETH/XMR
        eth = Coin.of(1d, "ETH"); //2500
        xmr = Coin.asXmr(10d);     // 250
        quote = Quote.of(eth, xmr);
        assertEquals(10000000000000L, quote.getValue());
        assertEquals(10, quote.asDouble());
        assertEquals(12, quote.getPrecision());
        assertEquals("XMR", quote.getQuoteMonetary().code);
        assertEquals("ETH", quote.getBaseMonetary().getCode());
        assertEquals("ETH/XMR", quote.getMarket().getCurrencyCodes());

        // USD/EUR
        usd = Fiat.of(1d, "USD");
        Fiat eur = Fiat.of(0.8, "EUR");
        quote = Quote.of(usd, eur);
        assertEquals(8000, quote.getValue());
        assertEquals(0.8, quote.asDouble());
        assertEquals(4, quote.getPrecision());
        assertEquals("EUR", quote.getQuoteMonetary().code);
        assertEquals("USD", quote.getBaseMonetary().getCode());
        assertEquals("USD/EUR", quote.getMarket().getCurrencyCodes());

        // EUR/USD
        eur = Fiat.of(1d, "EUR");
        usd = Fiat.of(1.2d, "USD");
        quote = Quote.of(eur, usd);
        assertEquals(12000, quote.getValue());
        assertEquals(1.2, quote.asDouble());
        assertEquals(4, quote.getPrecision());
        assertEquals("USD", quote.getQuoteMonetary().code);
        assertEquals("EUR", quote.getBaseMonetary().getCode());
        assertEquals("EUR/USD", quote.getMarket().getCurrencyCodes());

        // large numbers just below overflow
        xmr = Coin.asXmr(1500000d);
        btc = Coin.asBtc(10000.0);
        quote = Quote.of(xmr, btc);
        assertEquals(666667, quote.getValue());
        assertEquals(0.00666667, quote.asDouble());

        try {
            // overflow as we movePointRight by 12 with xmr
            xmr = Coin.asXmr(15000000d);
            btc = Coin.asBtc(100000.0);
            quote = Quote.of(xmr, btc);
            assertEquals(666667, quote.getValue());
            assertEquals(0.00666667, quote.asDouble());
        } catch (Exception e) {
            assertTrue(e instanceof ArithmeticException);
        }
    }
}