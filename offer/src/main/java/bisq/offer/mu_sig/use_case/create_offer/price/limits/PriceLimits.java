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

package bisq.offer.mu_sig.use_case.create_offer.price.limits;

import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.PriceQuoteRange;
import bisq.common.util.MathUtils;
import bisq.offer.price.PriceUtil;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stateless price-limit policy for the allowed floating range relative to a market price. Every
 * method derives its result from the explicitly supplied market price quote, so a limit can never
 * be stale relative to the price it is applied together with.
 */
public class PriceLimits {
    public static final double MIN_PERCENTAGE_FROM_MARKET_PRICE = -0.1;
    public static final double MAX_PERCENTAGE_FROM_MARKET_PRICE = 0.5;

    private PriceLimits() {
    }

    public static double clamp(double pricePercentage) {
        checkArgument(Double.isFinite(pricePercentage), "pricePercentage must be finite");
        return MathUtils.bounded(MIN_PERCENTAGE_FROM_MARKET_PRICE, MAX_PERCENTAGE_FROM_MARKET_PRICE, pricePercentage);
    }

    public static PriceQuoteRange rangeFor(PriceQuote marketPriceQuote) {
        checkNotNull(marketPriceQuote, "marketPriceQuote must not be null");
        return new PriceQuoteRange(
                PriceUtil.fromMarketPriceMarkup(marketPriceQuote, MIN_PERCENTAGE_FROM_MARKET_PRICE),
                PriceUtil.fromMarketPriceMarkup(marketPriceQuote, MAX_PERCENTAGE_FROM_MARKET_PRICE));
    }

    public static PriceQuote clamp(PriceQuote priceQuote, PriceQuoteRange range) {
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(range, "range must not be null");
        return priceQuote.clamp(range);
    }
}
