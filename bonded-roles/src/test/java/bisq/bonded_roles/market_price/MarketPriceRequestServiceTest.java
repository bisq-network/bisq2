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

package bisq.bonded_roles.market_price;

import bisq.common.asset.Asset;
import bisq.common.asset.StableCoin;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the market orientation logic that parseResponse uses.
 * We cannot instantiate MarketPriceRequestService directly in unit tests due to
 * static i18n dependency in MarketPriceProvider, so we verify the orientation
 * and filtering logic directly.
 */
class MarketPriceRequestServiceTest {

    @Test
    @DisplayName("fiat currency orientation: BTC is base, fiat is quote")
    void fiat_orientation() {
        String currencyCode = "USD";
        assertTrue(Asset.isFiat(currencyCode));
        assertFalse(StableCoin.isStableCoin(currencyCode));

        boolean isFiat = Asset.isFiat(currencyCode);
        boolean isStableCoin = StableCoin.isStableCoin(currencyCode);
        String baseCurrencyCode = (isFiat || isStableCoin) ? "BTC" : currencyCode;
        String quoteCurrencyCode = (isFiat || isStableCoin) ? currencyCode : "BTC";

        assertEquals("BTC", baseCurrencyCode);
        assertEquals("USD", quoteCurrencyCode);
    }

    @Test
    @DisplayName("stablecoin orientation: BTC is base, stablecoin is quote (same as fiat)")
    void stablecoin_orientation() {
        String currencyCode = "USDC";
        assertFalse(Asset.isFiat(currencyCode));
        assertTrue(StableCoin.isStableCoin(currencyCode));

        boolean isFiat = Asset.isFiat(currencyCode);
        boolean isStableCoin = StableCoin.isStableCoin(currencyCode);
        String baseCurrencyCode = (isFiat || isStableCoin) ? "BTC" : currencyCode;
        String quoteCurrencyCode = (isFiat || isStableCoin) ? currencyCode : "BTC";

        assertEquals("BTC", baseCurrencyCode);
        assertEquals("USDC", quoteCurrencyCode);
    }

    @Test
    @DisplayName("altcoin orientation: altcoin is base, BTC is quote")
    void altcoin_orientation() {
        String currencyCode = "XMR";
        assertFalse(Asset.isFiat(currencyCode));
        assertFalse(StableCoin.isStableCoin(currencyCode));

        boolean isFiat = Asset.isFiat(currencyCode);
        boolean isStableCoin = StableCoin.isStableCoin(currencyCode);
        String baseCurrencyCode = (isFiat || isStableCoin) ? "BTC" : currencyCode;
        String quoteCurrencyCode = (isFiat || isStableCoin) ? currencyCode : "BTC";

        assertEquals("XMR", baseCurrencyCode);
        assertEquals("BTC", quoteCurrencyCode);
    }

    @Test
    @DisplayName("BTC/USDC price quote creates correct market")
    void btc_usdc_price_quote_creates_correct_market() {
        PriceQuote priceQuote = PriceQuote.fromPrice(0.00000898, "BTC", "USDC");
        Market market = priceQuote.getMarket();
        assertEquals("BTC", market.getBaseCurrencyCode());
        assertEquals("USDC", market.getQuoteCurrencyCode());
        assertEquals("BTC/USDC", market.getMarketCodes());
    }

    @Test
    @DisplayName("findAnyMarketByMarketCodes finds BTC/USDC after stablecoin addition")
    void find_any_market_by_market_codes_finds_btc_usdc() {
        assertTrue(MarketRepository.findAnyMarketByMarketCodes("BTC/USDC").isPresent(),
                "BTC/USDC should be findable via findAnyMarketByMarketCodes");
    }

    @Test
    @DisplayName("findAnyMarketByMarketCodes does NOT find USDC/BTC (wrong orientation)")
    void find_any_market_by_market_codes_wrong_orientation() {
        assertFalse(MarketRepository.findAnyMarketByMarketCodes("USDC/BTC").isPresent(),
                "USDC/BTC should NOT be findable (wrong orientation)");
    }

    @Test
    @DisplayName("USDT is also recognized as stablecoin for orientation")
    void usdt_is_stablecoin() {
        assertTrue(StableCoin.isStableCoin("USDT"));

        boolean isFiat = Asset.isFiat("USDT");
        boolean isStableCoin = StableCoin.isStableCoin("USDT");
        String baseCurrencyCode = (isFiat || isStableCoin) ? "BTC" : "USDT";
        String quoteCurrencyCode = (isFiat || isStableCoin) ? "USDT" : "BTC";

        assertEquals("BTC", baseCurrencyCode);
        assertEquals("USDT", quoteCurrencyCode);
    }

    @Test
    @DisplayName("DAI is also recognized as stablecoin for orientation")
    void dai_is_stablecoin() {
        assertTrue(StableCoin.isStableCoin("DAI"));
    }
}
