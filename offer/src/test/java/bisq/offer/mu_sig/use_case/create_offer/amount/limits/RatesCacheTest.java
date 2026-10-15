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

package bisq.offer.mu_sig.use_case.create_offer.amount.limits;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RatesCacheTest {
    @Test
    void retentionDoesNotSpanAMarketChange() {
        Market usdMarket = MarketRepository.getUSDBitcoinMarket();
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(usdMarket))
                .thenReturn(Optional.of(PriceQuote.fromFiatPrice(100_000, "USD")));
        RatesCache ratesCache = new RatesCache();
        assertTrue(ratesCache.resolve(marketPriceService, usdMarket).isPresent());

        // Every rate vanishes, the market switches away and back: the pre-switch rates must
        // not be served for the original market - retention only spans rate gaps within one
        // continuous market session.
        when(marketPriceService.findMarketPriceQuote(any())).thenReturn(Optional.empty());
        assertTrue(ratesCache.resolve(marketPriceService, eurMarket).isEmpty());
        assertTrue(ratesCache.resolve(marketPriceService, usdMarket).isEmpty(),
                "stale rates from before the market change must not resurface");
    }

    @Test
    void retentionSpansARateGapWithinTheSameMarket() {
        Market usdMarket = MarketRepository.getUSDBitcoinMarket();
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(usdMarket))
                .thenReturn(Optional.of(PriceQuote.fromFiatPrice(100_000, "USD")));
        RatesCache ratesCache = new RatesCache();
        assertTrue(ratesCache.resolve(marketPriceService, usdMarket).isPresent());

        when(marketPriceService.findMarketPriceQuote(any())).thenReturn(Optional.empty());
        assertTrue(ratesCache.resolve(marketPriceService, usdMarket).isPresent(),
                "a rate gap within the same market keeps serving the cached rates");
    }
}
