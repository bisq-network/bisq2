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
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeAmountConversionTest {

    @Test
    @DisplayName("to trade amount with base side amount in btc fiat market")
    void to_trade_amount_with_base_side_amount_in_btc_fiat_market() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, btcAmount);

        assertEquals("BTC", tradeAmount.getBaseSideAmount().getCode());
        assertEquals(1000000, tradeAmount.getBaseSideAmount().getValue()); // 0.01 BTC
        assertEquals("USD", tradeAmount.getQuoteSideAmount().getCode());
        assertEquals(5000000, tradeAmount.getQuoteSideAmount().getValue()); // 500 USD
    }

    @Test
    @DisplayName("to trade amount with quote side amount in btc fiat market")
    void to_trade_amount_with_quote_side_amount_in_btc_fiat_market() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary usdAmount = Fiat.fromFaceValue(500, "USD");

        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, usdAmount);

        assertEquals("BTC", tradeAmount.getBaseSideAmount().getCode());
        assertEquals(1000000, tradeAmount.getBaseSideAmount().getValue()); // 0.01 BTC
        assertEquals("USD", tradeAmount.getQuoteSideAmount().getCode());
        assertEquals(5000000, tradeAmount.getQuoteSideAmount().getValue()); // 500 USD
    }

    @Test
    @DisplayName("to trade amount with base side amount in altcoin market")
    void to_trade_amount_with_base_side_amount_in_altcoin_market() {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PriceQuote priceQuote = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Monetary xmrAmount = Coin.fromFaceValue(2, "XMR");

        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, xmrAmount);

        assertEquals("XMR", tradeAmount.getBaseSideAmount().getCode());
        assertEquals(2000000000000L, tradeAmount.getBaseSideAmount().getValue()); // 2 XMR
        assertEquals("BTC", tradeAmount.getQuoteSideAmount().getCode());
        assertEquals(1000000, tradeAmount.getQuoteSideAmount().getValue()); // 0.01 BTC
    }

    @Test
    @DisplayName("to trade amount with quote side amount in altcoin market")
    void to_trade_amount_with_quote_side_amount_in_altcoin_market() {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PriceQuote priceQuote = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.01);

        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, btcAmount);

        assertEquals("XMR", tradeAmount.getBaseSideAmount().getCode());
        assertEquals(2000000000000L, tradeAmount.getBaseSideAmount().getValue()); // 2 XMR
        assertEquals("BTC", tradeAmount.getQuoteSideAmount().getCode());
        assertEquals(1000000, tradeAmount.getQuoteSideAmount().getValue()); // 0.01 BTC
    }

    @Test
    @DisplayName("to trade amount throws if amount currency is not in market")
    void to_trade_amount_throws_if_amount_currency_is_not_in_market() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary eurAmount = Fiat.fromFaceValue(100, "EUR");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TradeAmountConversion.toTradeAmount(market, priceQuote, eurAmount));

        assertTrue(exception.getMessage().contains("neither base nor quote side"));
    }

    @Test
    @DisplayName("is base and quote side amount helpers")
    void is_base_and_quote_side_amount_helpers() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        Monetary btcAmount = Coin.asBtcFromFaceValue(0.5);
        Monetary usdAmount = Fiat.fromFaceValue(100, "USD");
        Monetary eurAmount = Fiat.fromFaceValue(100, "EUR");

        assertTrue(TradeAmountConversion.isBaseSideAmount(market, btcAmount));
        assertFalse(TradeAmountConversion.isQuoteSideAmount(market, btcAmount));

        assertFalse(TradeAmountConversion.isBaseSideAmount(market, usdAmount));
        assertTrue(TradeAmountConversion.isQuoteSideAmount(market, usdAmount));

        assertFalse(TradeAmountConversion.isBaseSideAmount(market, eurAmount));
        assertFalse(TradeAmountConversion.isQuoteSideAmount(market, eurAmount));
    }
}
