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

        FiatCurrencyRepository.setLocale(Locale.US);
        assertEquals(1234567800, Fiat.parse("123456.78 USD").getValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> assertEquals(1234567800, Fiat.parse("123456.78USD").getValue()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> assertEquals(1234567800, Fiat.parse("123456.78 XYZ").getValue()));

        assertEquals(12345678, Coin.parse("0.12345678 BTC").getValue());
        // We do not check the cryptocurrency code as we do not want to be constrained
        assertEquals(12345678, Coin.parse("0.12345678 UNDEFINED").getValue());
        assertEquals(123456780000L, Coin.parse("0.12345678 XMR").getValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> assertEquals(1234567800, Coin.parse("123456.78 USD").getValue()));
    }

    @Test
    void testQuotes() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        Fiat usd = Fiat.fromFaceValue(50000d, "USD");
        PriceQuote priceQuote = PriceQuote.from(btc, usd);
        assertEquals(500000000, priceQuote.getValue());
        assertEquals(50000.0, priceQuote.asDouble());
        assertEquals(4, priceQuote.getPrecision());
        assertEquals("USD", priceQuote.getQuoteSideMonetary().code);
        assertEquals("BTC", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("BTC/USD", priceQuote.getMarket().getMarketCodes());

        usd = Fiat.fromFaceValue(50000.1249d, "USD");
        priceQuote = PriceQuote.from(btc, usd);
        assertEquals(500001249, priceQuote.getValue());
        assertEquals(50000.1249, priceQuote.asDouble());

        usd = Fiat.fromFaceValue(50000.1250d, "USD");
        priceQuote = PriceQuote.from(btc, usd);
        assertEquals(50000.1250, priceQuote.asDouble());

        btc = Coin.asBtcFromValue(100000000);
        usd = Fiat.fromValue(500001250, "USD");
        priceQuote = PriceQuote.from(btc, usd);
        assertEquals(500001250, priceQuote.getValue());
        assertEquals(50000.1250, priceQuote.asDouble());

        // btc - alt
        Coin xmr = Coin.asXmrFromFaceValue(150d);
        btc = Coin.asBtcFromFaceValue(1.0);
        priceQuote = PriceQuote.from(xmr, btc);
        assertEquals(666667, priceQuote.getValue());
        assertEquals(0.00666667, priceQuote.asDouble());
        assertEquals(8, priceQuote.getPrecision());
        assertEquals("BTC", priceQuote.getQuoteSideMonetary().code);
        assertEquals("XMR", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("XMR/BTC", priceQuote.getMarket().getMarketCodes());

        xmr = Coin.asXmrFromFaceValue(1d);
        btc = Coin.asBtcFromFaceValue(0.00666667);
        priceQuote = PriceQuote.from(xmr, btc);
        assertEquals(666667, priceQuote.getValue());
        assertEquals(0.00666667, priceQuote.asDouble());
        assertEquals(8, priceQuote.getPrecision());
        assertEquals("BTC", priceQuote.getQuoteSideMonetary().code);
        assertEquals("XMR", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("XMR/BTC", priceQuote.getMarket().getMarketCodes());

        // XMR/ETH
        xmr = Coin.asXmrFromFaceValue(1d);     // 250
        Coin eth = Coin.fromFaceValue(0.1, "ETH"); //2500
        priceQuote = PriceQuote.from(xmr, eth);
        assertEquals(10000000, priceQuote.getValue());
        assertEquals(0.1, priceQuote.asDouble());
        assertEquals(8, priceQuote.getPrecision());
        assertEquals("ETH", priceQuote.getQuoteSideMonetary().code);
        assertEquals("XMR", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("XMR/ETH", priceQuote.getMarket().getMarketCodes());

        // ETH/XMR
        eth = Coin.fromFaceValue(1d, "ETH"); //2500
        xmr = Coin.asXmrFromFaceValue(10d);     // 250
        priceQuote = PriceQuote.from(eth, xmr);
        assertEquals(10000000000000L, priceQuote.getValue());
        assertEquals(10, priceQuote.asDouble());
        assertEquals(12, priceQuote.getPrecision());
        assertEquals("XMR", priceQuote.getQuoteSideMonetary().code);
        assertEquals("ETH", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("ETH/XMR", priceQuote.getMarket().getMarketCodes());

        // USD/EUR
        usd = Fiat.fromFaceValue(1d, "USD");
        Fiat eur = Fiat.fromFaceValue(0.8, "EUR");
        priceQuote = PriceQuote.from(usd, eur);
        assertEquals(8000, priceQuote.getValue());
        assertEquals(0.8, priceQuote.asDouble());
        assertEquals(4, priceQuote.getPrecision());
        assertEquals("EUR", priceQuote.getQuoteSideMonetary().code);
        assertEquals("USD", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("USD/EUR", priceQuote.getMarket().getMarketCodes());

        // EUR/USD
        eur = Fiat.fromFaceValue(1d, "EUR");
        usd = Fiat.fromFaceValue(1.2d, "USD");
        priceQuote = PriceQuote.from(eur, usd);
        assertEquals(12000, priceQuote.getValue());
        assertEquals(1.2, priceQuote.asDouble());
        assertEquals(4, priceQuote.getPrecision());
        assertEquals("USD", priceQuote.getQuoteSideMonetary().code);
        assertEquals("EUR", priceQuote.getBaseSideMonetary().getCode());
        assertEquals("EUR/USD", priceQuote.getMarket().getMarketCodes());

        // large numbers just below overflow
        xmr = Coin.asXmrFromFaceValue(1500000d);
        btc = Coin.asBtcFromFaceValue(10000.0);
        priceQuote = PriceQuote.from(xmr, btc);
        assertEquals(666667, priceQuote.getValue());
        assertEquals(0.00666667, priceQuote.asDouble());

        try {
            // overflow as we movePointRight by 12 with xmr
            xmr = Coin.asXmrFromFaceValue(15000000d);
            btc = Coin.asBtcFromFaceValue(100000.0);
            priceQuote = PriceQuote.from(xmr, btc);
            assertEquals(666667, priceQuote.getValue());
            assertEquals(0.00666667, priceQuote.asDouble());
        } catch (Exception e) {
            assertTrue(e instanceof ArithmeticException);
        }
    }
}