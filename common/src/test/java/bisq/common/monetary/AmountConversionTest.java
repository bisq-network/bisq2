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

import bisq.common.market.Market;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AmountConversionTest {

    @Test
    void testFiatToBtc() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary fiatAmount = Fiat.fromFaceValue(100.0, "USD");

        Monetary btcAmount = AmountConversion.fiatToBtc(btcUsdPrice, fiatAmount);

        assertInstanceOf(Coin.class, btcAmount);
        // 100 USD at 50000 USD/BTC = 0.002 BTC = 200,000 satoshis
        assertEquals(200000, btcAmount.getValue());
    }

    @Test
    void testBtcToFiat() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.002);

        Monetary fiatAmount = AmountConversion.btcToFiat(btcUsdPrice, btcAmount);

        assertInstanceOf(Fiat.class, fiatAmount);
        // 0.002 BTC at 50000 USD/BTC = 100 USD = 1,000,000 (with precision 4)
        assertEquals(1000000, fiatAmount.getValue());
    }

    @Test
    void testUsdToBtc() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary usdAmount = Fiat.fromFaceValue(100.0, "USD");

        Monetary btcAmount = AmountConversion.usdToBtc(btcUsdPrice, usdAmount);

        assertInstanceOf(Coin.class, btcAmount);
        assertEquals(200000, btcAmount.getValue());
    }

    @Test
    void testBtcToUsd() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.002);

        Monetary usdAmount = AmountConversion.btcToUsd(btcUsdPrice, btcAmount);

        assertInstanceOf(Fiat.class, usdAmount);
        assertEquals(1000000, usdAmount.getValue());
    }

    @Test
    void testUsdToFiat() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcEurPrice = PriceQuote.fromFiatPrice(40000, "EUR");
        Monetary usdAmount = Fiat.fromFaceValue(100.0, "USD");

        // 100 USD -> 0.002 BTC -> 0.002 * 40000 = 80 EUR
        Monetary eurAmount = AmountConversion.usdToFiat(btcUsdPrice, btcEurPrice, usdAmount);

        assertInstanceOf(Fiat.class, eurAmount);
        assertEquals(800000, eurAmount.getValue());
        assertEquals("EUR", eurAmount.getCode());
    }

    @Test
    void testFiatToUsd() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcEurPrice = PriceQuote.fromFiatPrice(40000, "EUR");
        Monetary eurAmount = Fiat.fromFaceValue(80.0, "EUR");

        // 80 EUR -> 80 / 40000 = 0.002 BTC -> 0.002 * 50000 = 100 USD
        Monetary usdAmount = AmountConversion.fiatToUsd(btcUsdPrice, btcEurPrice, eurAmount);

        assertInstanceOf(Fiat.class, usdAmount);
        assertEquals(1000000, usdAmount.getValue());
        assertEquals("USD", usdAmount.getCode());
    }

    @Test
    void testBtcToOtherCrypto() {
        // PriceQuote for XMR/BTC. If 1 XMR = 0.005 BTC, then price is 0.005.
        // Base side is XMR, Quote side is BTC.
        // fromPrice(price, base, quote)
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market xmrBtcMarket = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        // btcToOtherCrypto(btcOtherCryptoPriceQuote, otherCryptoBtcMarket, btcAmount)
        // delegates to btcOtherCryptoPriceQuote.toBaseSideMonetary(btcAmount)
        // 0.01 BTC / 0.005 BTC/XMR = 2 XMR
        Monetary xmrAmount = AmountConversion.btcToOtherCrypto(xmrBtcPrice, xmrBtcMarket, btcAmount);

        assertInstanceOf(Coin.class, xmrAmount);
        assertEquals("XMR", xmrAmount.getCode());
        assertEquals(12, xmrAmount.getPrecision());
        assertEquals(2000000000000L, xmrAmount.getValue()); // 2 XMR with precision 12
    }

    @Test
    void testOtherCryptoToBtc() {
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market xmrBtcMarket = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary xmrAmount = Coin.fromFaceValue(2.0, "XMR");

        // 2 XMR * 0.005 BTC/XMR = 0.01 BTC
        Monetary btcAmount = AmountConversion.otherCryptoToBtc(xmrBtcPrice, xmrBtcMarket, xmrAmount);

        assertInstanceOf(Coin.class, btcAmount);
        assertEquals("BTC", btcAmount.getCode());
        assertEquals(1000000, btcAmount.getValue()); // 0.01 BTC = 1,000,000 satoshis
    }

    @Test
    void testUsdToOtherCrypto() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market xmrBtcMarket = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary usdAmount = Fiat.fromFaceValue(100.0, "USD");

        // 100 USD -> 0.002 BTC -> 0.002 / 0.005 = 0.4 XMR
        Monetary xmrAmount = AmountConversion.usdToOtherCrypto(btcUsdPrice, xmrBtcPrice, xmrBtcMarket, usdAmount);

        assertInstanceOf(Coin.class, xmrAmount);
        assertEquals("XMR", xmrAmount.getCode());
        assertEquals(400000000000L, xmrAmount.getValue()); // 0.4 XMR
    }

    @Test
    void testOtherCryptoToUsd() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market xmrBtcMarket = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary xmrAmount = Coin.fromFaceValue(0.4, "XMR");

        // 0.4 XMR -> 0.4 * 0.005 = 0.002 BTC -> 0.002 * 50000 = 100 USD
        Monetary usdAmount = AmountConversion.otherCryptoToUsd(btcUsdPrice, xmrBtcPrice, xmrBtcMarket, xmrAmount);

        assertInstanceOf(Fiat.class, usdAmount);
        assertEquals(1000000, usdAmount.getValue());
        assertEquals("USD", usdAmount.getCode());
    }

    @Test
    void testFiatToBtcThrowsOnNonFiatInput() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.fiatToBtc(btcUsdPrice, btcAmount));
    }

    @Test
    void testBtcToFiatThrowsOnNonBtcInput() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary usdAmount = Fiat.fromFaceValue(100, "USD");

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.btcToFiat(btcUsdPrice, usdAmount));
    }

    @Test
    void testUsdToFiatThrowsOnNonFiatUsdInput() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcEurPrice = PriceQuote.fromFiatPrice(40000, "EUR");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.usdToFiat(btcUsdPrice, btcEurPrice, btcAmount));
    }

    @Test
    void testFiatToUsdThrowsOnNonFiatInput() {
        PriceQuote btcUsdPrice = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcEurPrice = PriceQuote.fromFiatPrice(40000, "EUR");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.fiatToUsd(btcUsdPrice, btcEurPrice, btcAmount));
    }

    @Test
    void testUsdToBtcThrowsIfQuoteIsNotBtcUsd() {
        PriceQuote btcEurPrice = PriceQuote.fromFiatPrice(40000, "EUR");
        Monetary usdAmount = Fiat.fromFaceValue(100, "USD");

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.usdToBtc(btcEurPrice, usdAmount));
    }

    @Test
    void testBtcToOtherCryptoThrowsIfMarketDoesNotMatchPriceQuote() {
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market ethBtcMarket = new Market("ETH", "BTC", "Ether", "Bitcoin");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.btcToOtherCrypto(xmrBtcPrice, ethBtcMarket, btcAmount));
    }

    @Test
    void testOtherCryptoToBtcThrowsIfAmountCodeDoesNotMatchMarketBase() {
        PriceQuote xmrBtcPrice = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Market xmrBtcMarket = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary ethAmount = Coin.fromFaceValue(1, "ETH");

        assertThrows(IllegalArgumentException.class,
                () -> AmountConversion.otherCryptoToBtc(xmrBtcPrice, xmrBtcMarket, ethAmount));
    }
}
