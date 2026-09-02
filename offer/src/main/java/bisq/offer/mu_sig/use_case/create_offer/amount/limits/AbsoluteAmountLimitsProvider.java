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
import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;

public class AbsoluteAmountLimitsProvider extends LifecycleScope {
    public static final Fiat MIN_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(10, "USD");
    public static final Fiat MAX_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(10000, "USD");

    private final Observable<TradeAmountRange> tradeAmountLimits = new Observable<>();
    private final MarketPriceService marketPriceService;
    private final MarketSelection marketSelection;
    private final PriceSelection priceSelection;

    AbsoluteAmountLimitsProvider(MarketPriceService marketPriceService,
                                 MarketSelection marketSelection,
                                 PriceSelection priceSelection) {
        this.marketSelection = marketSelection;
        this.priceSelection = priceSelection;
        this.marketPriceService = marketPriceService;
    }

    @Override
    public void initialize() {
        addDisposable(marketSelection.addMarketListener(market -> update(market, priceSelection.getPriceQuote())));
        addDisposable(priceSelection.addPriceQuoteListener(priceQuote -> update(marketSelection.getMarket(), priceQuote)));
    }


    /* --------------------------------------------------------------------- */
    // Update
    /* --------------------------------------------------------------------- */

    private void update(Market market, PriceQuote priceQuote) {
        if (dependenciesValid(market, priceQuote)) {
            TradeAmount minTradeAmount = TradeAmountLimitUtils.toTradeAmountLimit(marketPriceService,
                    market,
                    priceQuote,
                    MIN_TRADE_AMOUNT_IN_USD);
            TradeAmount maxTradeAmount = TradeAmountLimitUtils.toTradeAmountLimit(marketPriceService,
                    market,
                    priceQuote,
                    MAX_TRADE_AMOUNT_IN_USD);
            tradeAmountLimits.set(new TradeAmountRange(minTradeAmount, maxTradeAmount));
        }
    }

    private static boolean dependenciesValid(Market market, PriceQuote priceQuote) {
        return market != null &&
                priceQuote != null &&
                market.equals(priceQuote.getMarket());
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    ReadOnlyObservable<TradeAmountRange> tradeAmountLimitsObservable() {
        return tradeAmountLimits;
    }

    TradeAmountRange getTradeAmountLimits() {
        return tradeAmountLimits.get();
    }
}
