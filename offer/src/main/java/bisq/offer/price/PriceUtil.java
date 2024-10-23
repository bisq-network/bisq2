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

package bisq.offer.price;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.MathUtils;
import bisq.offer.Offer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class PriceUtil {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Market price
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A quote created from a market price quote and a percentage
     *
     * @param marketPrice Current market price
     * @param percentage  Offset from market price in percent normalize to 1 (=100%).
     * @return The quote representing the offset from market price
     */
    public static PriceQuote fromMarketPriceMarkup(PriceQuote marketPrice, double percentage) {
        checkArgument(percentage >= -1, "Percentage must not be lower than -100%");
        double price = marketPrice.asDouble() * (1 + percentage);
        return PriceQuote.fromPrice(price, marketPrice.getBaseSideMonetary().getCode(), marketPrice.getQuoteSideMonetary().getCode());
    }

    /**
     * @param marketPrice The quote representing the market price
     * @param priceQuote  The quote we want to compare to the market price
     * @return The percentage offset from the market price. Positive value means that quote is above market price.
     * Result is rounded to precision 4 (2 decimal places at percentage representation)
     */
    public static double getPercentageToMarketPrice(PriceQuote marketPrice, PriceQuote priceQuote) {
        checkArgument(marketPrice.getValue() > 0, "marketQuote must be positive");
        return MathUtils.roundDouble(priceQuote.getValue() / (double) marketPrice.getValue() - 1, 4);
    }

    public static Optional<Double> findPercentFromMarketPrice(MarketPriceService marketPriceService,
                                                              Offer<?, ?> offer) {
        return findPercentFromMarketPrice(marketPriceService, offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Double> findPercentFromMarketPrice(MarketPriceService marketPriceService,
                                                              PriceSpec priceSpec,
                                                              Market market) {
        Optional<Double> percentage;
        if (priceSpec instanceof FixPriceSpec) {
            PriceQuote fixPrice = getFixPriceQuote((FixPriceSpec) priceSpec);
            percentage = findMarketPriceQuote(marketPriceService, market).map(marketPrice ->
                    getPercentageToMarketPrice(marketPrice, fixPrice));
        } else if (priceSpec instanceof MarketPriceSpec) {
            percentage = Optional.of(0d);
        } else if (priceSpec instanceof FloatPriceSpec) {
            percentage = Optional.of(((FloatPriceSpec) priceSpec).getPercentage());
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
        return percentage;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Quote
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<PriceQuote> findQuote(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return findQuote(marketPriceService, offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<PriceQuote> findQuote(MarketPriceService marketPriceService,
                                                 PriceSpec priceSpec,
                                                 Market market) {
        if (priceSpec instanceof FixPriceSpec) {
            return Optional.of(getFixPriceQuote((FixPriceSpec) priceSpec));
        } else if (priceSpec instanceof MarketPriceSpec) {
            return findMarketPriceQuote(marketPriceService, market);
        } else if (priceSpec instanceof FloatPriceSpec) {
            return findFloatPriceQuote(marketPriceService, (FloatPriceSpec) priceSpec, market);
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public static PriceQuote getFixPriceQuote(FixPriceSpec fixPriceSpec) {
        return fixPriceSpec.getPriceQuote();
    }

    public static Optional<PriceQuote> findFloatPriceQuote(MarketPriceService marketPriceService,
                                                           FloatPriceSpec floatPriceSpec,
                                                           Market market) {
        return findMarketPriceQuote(marketPriceService, market)
                .map(marketQuote -> fromMarketPriceMarkup(marketQuote, floatPriceSpec.getPercentage()));
    }

    public static Optional<PriceQuote> findMarketPriceQuote(MarketPriceService marketPriceService, Market market) {
        return marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote).stream().findAny();
    }
}