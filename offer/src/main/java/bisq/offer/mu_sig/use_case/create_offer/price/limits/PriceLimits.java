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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.PriceQuoteRange;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.MathUtils;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.price.PriceUtil;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class PriceLimits extends LifecycleScope {
    public static final double MIN_PERCENTAGE_FROM_MARKET_PRICE = -0.1;
    public static final double MAX_PERCENTAGE_FROM_MARKET_PRICE = 0.5;

    protected final Observable<PriceQuoteRange> tradeAmountLimits = new Observable<>();

    private final MarketPriceService marketPriceService;
    private final MarketSelection marketSelection;

    public PriceLimits(MarketPriceService marketPriceService, MarketSelection marketSelection) {
        checkNotNull(marketPriceService, "marketPriceService must not be null");
        checkNotNull(marketSelection, "marketService must not be null");
        this.marketSelection = marketSelection;
        this.marketPriceService = marketPriceService;
    }

    @Override
    public void initialize() {
        addDisposable(marketSelection.marketObservable().addObserver(market -> {
            if (market != null) {
                PriceQuote minTradeAmount = percentageToPriceQuote(marketPriceService,
                        market,
                        MIN_PERCENTAGE_FROM_MARKET_PRICE);
                PriceQuote maxTradeAmount = percentageToPriceQuote(marketPriceService,
                        market,
                        MAX_PERCENTAGE_FROM_MARKET_PRICE);
                tradeAmountLimits.set(new PriceQuoteRange(minTradeAmount, maxTradeAmount));
            }
        }));
    }


    public PriceQuote clamp(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "priceQuote must not be null");
        return priceQuote.clamp(getAmountLimits());
    }

    public double clamp(double pricePercentage) {
        checkArgument(Double.isFinite(pricePercentage), "pricePercentage must be finite");
        return MathUtils.bounded(MIN_PERCENTAGE_FROM_MARKET_PRICE, MAX_PERCENTAGE_FROM_MARKET_PRICE, pricePercentage);
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<PriceQuoteRange> amountLimitsObservable() {
        return tradeAmountLimits;
    }

    public PriceQuoteRange getAmountLimits() {
        return tradeAmountLimits.get();
    }

    private static PriceQuote percentageToPriceQuote(MarketPriceService marketPriceService,
                                                     Market market,
                                                     double pricePercentage) {
        PriceQuote marketPriceQuote = marketPriceService.getMarketPriceQuoteOrThrow(market);
        return PriceUtil.fromMarketPriceMarkup(marketPriceQuote, pricePercentage);
    }
}
