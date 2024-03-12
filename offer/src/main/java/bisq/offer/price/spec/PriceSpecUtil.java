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

package bisq.offer.price.spec;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.MathUtils;

import java.util.Optional;

public class PriceSpecUtil {
    public static Optional<FixPriceSpec> findFixPriceSpec(PriceSpec priceSpec) {
        return priceSpec instanceof FixPriceSpec
                ? Optional.of((FixPriceSpec) priceSpec)
                : Optional.empty();
    }

    public static Optional<FloatPriceSpec> findFloatPriceSpec(PriceSpec priceSpec) {
        return priceSpec instanceof FloatPriceSpec
                ? Optional.of((FloatPriceSpec) priceSpec)
                : Optional.empty();
    }

    public static Optional<MarketPriceSpec> findMarketPriceSpec(PriceSpec priceSpec) {
        return priceSpec instanceof MarketPriceSpec
                ? Optional.of((MarketPriceSpec) priceSpec)
                : Optional.empty();
    }

    public static Optional<Double> createFloatPriceAsPercentage(MarketPriceService marketPriceService, PriceQuote priceQuote) {
        return marketPriceService.findMarketPrice(priceQuote.getMarket())
                .map(MarketPrice::getPriceQuote).stream()
                .map(marketPrice -> {
                    double exact = (double) priceQuote.getValue() / (double) marketPrice.getValue() - 1;
                    return MathUtils.roundDouble(exact, 4);
                })
                .findAny();
    }

    public static Optional<FloatPriceSpec> createFloatPriceSpec(MarketPriceService marketPriceService, PriceQuote priceQuote) {
        return createFloatPriceAsPercentage(marketPriceService, priceQuote)
                .map(FloatPriceSpec::new);
    }
}
