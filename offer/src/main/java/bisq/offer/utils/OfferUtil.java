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

package bisq.offer.utils;

import bisq.common.monetary.Quote;
import bisq.common.util.MathUtils;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OfferUtil {
    public static Optional<FloatPriceSpec> findFloatPriceSpec(MarketPriceService marketPriceService, Quote quote) {
        return marketPriceService.findMarketPrice(quote.getMarket())
                .map(MarketPrice::getQuote).stream()
                .map(marketPrice -> {
                    double exact = (double) quote.getValue() / (double) marketPrice.getValue() - 1;
                    return MathUtils.roundDouble(exact, 4);
                })
                .map(FloatPriceSpec::new)
                .findAny();
    }
}
