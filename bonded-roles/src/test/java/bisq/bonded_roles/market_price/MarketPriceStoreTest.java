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

import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.i18n.Res;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A node uses only positive market prices. Its persisted prices are one of the places where prices enter the node,
 * for example a price which an earlier version persisted. See docs/specifications/bonded-roles/market-prices.md.
 */
class MarketPriceStoreTest {
    @BeforeAll
    static void setUpResources() {
        // MarketPriceProvider reads a display name from the resources
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void persistedNonPositivePriceIsSkippedAtLoadAndTheOtherPricesAreKept() throws UnresolvableProtobufException {
        MarketPrice usdPrice = marketPrice(PriceQuote.fromFiatPrice(60000, "USD"));
        MarketPrice eurPrice = marketPrice(PriceQuote.fromFiatPrice(-1000, "EUR"));
        MarketPrice chfPrice = marketPrice(PriceQuote.fromFiatPrice(0, "CHF"));
        MarketPriceStore persisted = new MarketPriceStore();
        persisted.getMarketPriceByCurrencyMap().put(usdPrice.getMarket(), usdPrice);
        persisted.getMarketPriceByCurrencyMap().put(eurPrice.getMarket(), eurPrice);
        persisted.getMarketPriceByCurrencyMap().put(chfPrice.getMarket(), chfPrice);

        MarketPriceStore loaded = MarketPriceStore.fromProto(persisted.toProto(false));

        assertThat(loaded.getMarketPriceByCurrencyMap()).containsOnlyKeys(MarketRepository.getUSDBitcoinMarket());
    }

    private static MarketPrice marketPrice(PriceQuote priceQuote) {
        return new MarketPrice(priceQuote,
                System.currentTimeMillis(),
                new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE));
    }
}
