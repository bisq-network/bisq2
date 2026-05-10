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

import bisq.common.asset.FiatCurrency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketRepositoryTest {

    @Test
    @DisplayName("default btc fiat market has btc base")
    void default_btc_fiat_market_has_btc_base() {
        Market market = MarketRepository.getDefaultBtcFiatMarket();
        assertEquals("BTC", market.getBaseCurrencyCode());
        assertTrue(FiatCurrency.isFiat(market.getQuoteCurrencyCode()));
    }

    @Test
    @DisplayName("usd bitcoin market has usd quote")
    void usd_bitcoin_market_has_usd_quote() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        assertEquals("BTC", market.getBaseCurrencyCode());
        assertEquals("USD", market.getQuoteCurrencyCode());
    }

    @Test
    @DisplayName("major fiat markets non empty and all btc base")
    void major_fiat_markets_non_empty_and_all_btc_base() {
        List<Market> markets = MarketRepository.getMajorFiatMarkets();
        assertFalse(markets.isEmpty());
        markets.forEach(m -> assertEquals("BTC", m.getBaseCurrencyCode()));
    }

    @Test
    @DisplayName("major fiat markets all have fiat quote")
    void major_fiat_markets_all_have_fiat_quote() {
        MarketRepository.getMajorFiatMarkets().forEach(m ->
                assertTrue(FiatCurrency.isFiat(m.getQuoteCurrencyCode()),
                        "Expected fiat quote for " + m.getMarketCodes()));
    }

    @Test
    @DisplayName("all fiat markets contains major")
    void all_fiat_markets_contains_major() {
        List<Market> all = MarketRepository.getAllFiatMarkets();
        List<Market> major = MarketRepository.getMajorFiatMarkets();
        assertTrue(all.containsAll(major));
    }

    @Test
    @DisplayName("all fiat markets no duplicates")
    void all_fiat_markets_no_duplicates() {
        List<Market> all = MarketRepository.getAllFiatMarkets();
        assertEquals(all.size(), new HashSet<>(all).size());
    }

    @Test
    @DisplayName("major crypto asset markets all have btc quote")
    void major_crypto_asset_markets_all_have_btc_quote() {
        MarketRepository.getMajorCryptoAssetMarkets().forEach(m ->
                assertEquals("BTC", m.getQuoteCurrencyCode()));
    }

    @Test
    @DisplayName("all crypto asset markets excludes btc")
    void all_crypto_asset_markets_excludes_btc() {
        MarketRepository.getAllCryptoAssetMarkets().forEach(m ->
                assertNotEquals("BTC", m.getBaseCurrencyCode()));
    }

    @Test
    @DisplayName("all markets contains fiat and crypto")
    void all_markets_contains_fiat_and_crypto() {
        List<Market> all = MarketRepository.getAllMarkets();
        assertTrue(all.stream().anyMatch(Market::isBtcFiatMarket));
    }

    @Test
    @DisplayName("all markets no duplicates")
    void all_markets_no_duplicates() {
        List<Market> all = MarketRepository.getAllMarkets();
        assertEquals(all.size(), new HashSet<>(all).size());
    }

    @Test
    @DisplayName("find any market by market codes present")
    void find_any_market_by_market_codes_present() {
        assertTrue(MarketRepository.findAnyMarketByMarketCodes("BTC/USD").isPresent());
    }

    @Test
    @DisplayName("find any market by market codes absent")
    void find_any_market_by_market_codes_absent() {
        assertTrue(MarketRepository.findAnyMarketByMarketCodes("FAKE/COIN").isEmpty());
    }

    @Test
    @DisplayName("find any fiat market by market codes present")
    void find_any_fiat_market_by_market_codes_present() {
        assertTrue(MarketRepository.findAnyFiatMarketByMarketCodes("BTC/USD").isPresent());
    }

    @Test
    @DisplayName("find any fiat market by market codes absent")
    void find_any_fiat_market_by_market_codes_absent() {
        assertTrue(MarketRepository.findAnyFiatMarketByMarketCodes("XMR/BTC").isEmpty());
    }

    @Test
    @DisplayName("xmr btc market correct")
    void xmr_btc_market_correct() {
        Market xmrBtc = MarketRepository.getXmrBtcMarket();
        assertEquals("XMR", xmrBtc.getBaseCurrencyCode());
        assertEquals("BTC", xmrBtc.getQuoteCurrencyCode());
    }

    @Test
    @DisplayName("bsq market correct")
    void bsq_market_correct() {
        Market bsq = MarketRepository.getBsqMarket();
        assertEquals("BSQ", bsq.getBaseCurrencyCode());
        assertEquals("BTC", bsq.getQuoteCurrencyCode());
    }
}
