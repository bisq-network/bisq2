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

import java.util.Optional;

/**
 * The last valid conversion rates for one market. When a rate vanishes mid-session the caller
 * still recomputes its output from the CURRENT market, quote and policy context with these
 * rates, so no stale converted output can survive a context change. Retention never spans a
 * market change: rates cached for another market are not returned.
 */
final class RatesCache {
    private TradeAmountLimitUtils.Rates rates;
    private Market market;

    Optional<TradeAmountLimitUtils.Rates> resolve(MarketPriceService marketPriceService, Market market) {
        Optional<TradeAmountLimitUtils.Rates> fresh = TradeAmountLimitUtils.findRates(marketPriceService, market);
        if (fresh.isPresent()) {
            this.rates = fresh.get();
            this.market = market;
            return fresh;
        }
        if (rates != null && !market.equals(this.market)) {
            // The first request for another market invalidates the cache even without fresh
            // rates; otherwise switching away and back would serve pre-switch rates and the
            // retention would span the market change after all.
            rates = null;
            this.market = null;
        }
        return Optional.ofNullable(rates);
    }
}
