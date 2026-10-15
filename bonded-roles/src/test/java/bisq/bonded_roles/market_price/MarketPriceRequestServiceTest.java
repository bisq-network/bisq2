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

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A node uses only positive market prices. The response of a price node is one of the places where prices enter the
 * node. See docs/specifications/bonded-roles/market-prices.md.
 */
class MarketPriceRequestServiceTest {
    private MarketPriceRequestService service;

    @BeforeAll
    static void setUpResources() {
        // MarketPriceProvider reads a display name from the resources
        Res.setAndApplyLanguageTag("en");
    }

    @BeforeEach
    void setUp() {
        service = new MarketPriceRequestService(new MarketPriceRequestService.Config(Set.of(), Set.of(), 60, 10),
                mock(NetworkService.class));
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void nonPositivePricesOfAResponseAreSkippedAndTheOtherPricesAreKept() {
        long now = System.currentTimeMillis();
        String json = "{\"data\":[" +
                entry("EUR", -1000, now) + "," +
                entry("CHF", 0, now) + "," +
                entry("USD", 60000, now) +
                "]}";

        Map<Market, MarketPrice> prices = service.parseResult(json);

        Market usdMarket = MarketRepository.getUSDBitcoinMarket();
        assertThat(prices).containsOnlyKeys(usdMarket);
        assertThat(prices.get(usdMarket).getPriceQuote()).isEqualTo(PriceQuote.fromFiatPrice(60000, "USD"));
    }

    // The format of an entry of the price node, see dev_market_price.json. The field timestampSec holds milliseconds.
    private static String entry(String currencyCode, double price, long timestamp) {
        return "{\"currencyCode\":\"" + currencyCode + "\"," +
                "\"price\":" + price + "," +
                "\"timestampSec\":" + timestamp + "," +
                "\"provider\":\"Bisq-Aggregate\"}";
    }
}
