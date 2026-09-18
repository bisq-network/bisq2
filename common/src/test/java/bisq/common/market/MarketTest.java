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

package bisq.common.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class MarketTest {

    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final Market BTC_EUR = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Market BTC_USDT = new Market("BTC", "USDT", "Bitcoin", "Tether USD");
    private static final Market BTC_USDC = new Market("BTC", "USDC", "Bitcoin", "USD Coin");
    private static final Market BTC_DAI = new Market("BTC", "DAI", "Bitcoin", "Dai");
    private static final Market XMR_BTC = new Market("XMR", "BTC", "Monero", "Bitcoin");
    private static final Market ETH_BTC = new Market("ETH", "BTC", "Ethereum", "Bitcoin");

    @Test
    @DisplayName("is btc fiat market true for btc usd")
    void is_btc_fiat_market_true_for_btc_usd() {
        assertTrue(BTC_USD.isBtcFiatMarket());
    }

    @Test
    @DisplayName("is btc fiat market true for btc eur")
    void is_btc_fiat_market_true_for_btc_eur() {
        assertTrue(BTC_EUR.isBtcFiatMarket());
    }

    @Test
    @DisplayName("is btc fiat market false for btc usdt")
    void is_btc_fiat_market_false_for_btc_usdt() {
        assertFalse(BTC_USDT.isBtcFiatMarket());
    }

    @Test
    @DisplayName("is btc fiat market false for crypto btc")
    void is_btc_fiat_market_false_for_crypto_btc() {
        assertFalse(XMR_BTC.isBtcFiatMarket());
    }

    @Test
    @DisplayName("is btc stable coin market true for btc usdt")
    void is_btc_stable_coin_market_true_for_btc_usdt() {
        assertTrue(BTC_USDT.isBtcStableCoinMarket());
    }

    @Test
    @DisplayName("is btc stable coin market true for btc usdc")
    void is_btc_stable_coin_market_true_for_btc_usdc() {
        assertTrue(BTC_USDC.isBtcStableCoinMarket());
    }

    @Test
    @DisplayName("is btc stable coin market true for btc dai")
    void is_btc_stable_coin_market_true_for_btc_dai() {
        assertTrue(BTC_DAI.isBtcStableCoinMarket());
    }

    @Test
    @DisplayName("is btc stable coin market false for btc usd")
    void is_btc_stable_coin_market_false_for_btc_usd() {
        assertFalse(BTC_USD.isBtcStableCoinMarket());
    }

    @Test
    @DisplayName("is btc stable coin market false for crypto btc")
    void is_btc_stable_coin_market_false_for_crypto_btc() {
        assertFalse(XMR_BTC.isBtcStableCoinMarket());
    }

    @Test
    @DisplayName("is crypto true when quote is btc")
    void is_crypto_true_when_quote_is_btc() {
        assertTrue(XMR_BTC.isCrypto());
        assertTrue(ETH_BTC.isCrypto());
    }

    @Test
    @DisplayName("is crypto false when quote is fiat")
    void is_crypto_false_when_quote_is_fiat() {
        assertFalse(BTC_USD.isCrypto());
    }

    @Test
    @DisplayName("is crypto false when quote is stablecoin")
    void is_crypto_false_when_quote_is_stablecoin() {
        assertFalse(BTC_USDT.isCrypto());
    }

    @Test
    @DisplayName("is xmr true only for xmr btc")
    void is_xmr_true_only_for_xmr_btc() {
        assertTrue(XMR_BTC.isXmr());
        assertFalse(ETH_BTC.isXmr());
        assertFalse(BTC_USD.isXmr());
    }

    @Test
    @DisplayName("is usd market true for btc usd")
    void is_usd_market_true_for_btc_usd() {
        assertTrue(BTC_USD.isUsdMarket());
    }

    @Test
    @DisplayName("is usd market false for btc eur")
    void is_usd_market_false_for_btc_eur() {
        assertFalse(BTC_EUR.isUsdMarket());
    }

    @Test
    @DisplayName("is usd market false for stablecoin")
    void is_usd_market_false_for_stablecoin() {
        assertFalse(BTC_USDC.isUsdMarket());
    }

    @Test
    @DisplayName("is base currency bitcoin")
    void is_base_currency_bitcoin() {
        assertTrue(BTC_USD.isBaseCurrencyBitcoin());
        assertTrue(BTC_USDT.isBaseCurrencyBitcoin());
        assertFalse(XMR_BTC.isBaseCurrencyBitcoin());
    }

    @Test
    @DisplayName("get market codes format")
    void get_market_codes_format() {
        assertEquals("BTC/USD", BTC_USD.getMarketCodes());
        assertEquals("XMR/BTC", XMR_BTC.getMarketCodes());
    }

    @Test
    @DisplayName("get relevant currency code returns non btc side")
    void get_relevant_currency_code_returns_non_btc_side() {
        assertEquals("USD", BTC_USD.getRelevantCurrencyCode());
        assertEquals("XMR", XMR_BTC.getRelevantCurrencyCode());
        assertEquals("USDT", BTC_USDT.getRelevantCurrencyCode());
    }

    @Test
    @DisplayName("equals based on currency codes")
    void equals_based_on_currency_codes() {
        Market same = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        assertEquals(BTC_USD, same);
        assertNotEquals(BTC_USD, BTC_EUR);
    }

    @Test
    @DisplayName("compare to uses market codes")
    void compare_to_uses_market_codes() {
        assertTrue(BTC_EUR.compareTo(BTC_USD) < 0);
        assertEquals(0, BTC_USD.compareTo(new Market("BTC", "USD", "Bitcoin", "US Dollar")));
    }
}
