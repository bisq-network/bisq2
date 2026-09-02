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
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.Direction;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserSpecificAmountLimitsProvider extends LifecycleScope {
    private static final long USER_SPECIFIC_LIMIT_IN_USD = 4000;

    private final MarketPriceService marketPriceService;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final PriceSelection priceSelection;
    private final Observable<Optional<TradeAmount>> tradeAmountLimit = new Observable<>(Optional.empty());

    UserSpecificAmountLimitsProvider(MarketPriceService marketPriceService,
                                     MarketSelection marketSelection,
                                     DirectionSelection directionSelection,
                                     PriceSelection priceSelection) {
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
        this.marketSelection = marketSelection;
        this.directionSelection = directionSelection;
        this.priceSelection = priceSelection;
    }

    // The market the current output was computed for; retention across markets is refused.
    private Market outputMarket;
    private final RatesCache ratesCache = new RatesCache();

    private Optional<TradeAmountLimitUtils.Rates> resolveRates(Market market) {
        return ratesCache.resolve(marketPriceService, market);
    }

    @Override
    public void initialize() {
        addDisposable(marketSelection.addMarketListener(market ->
                update(market,
                        directionSelection.getDisplayDirection(),
                        priceSelection.getPriceQuote())));
        addDisposable(directionSelection.addDisplayDirectionListener(direction ->
                update(marketSelection.getMarket(),
                        direction,
                        priceSelection.getPriceQuote())));
        addDisposable(priceSelection.addPriceQuoteListener(priceQuote ->
                update(marketSelection.getMarket(),
                        directionSelection.getDisplayDirection(),
                        priceQuote)));

        // The market context can change (e.g. the BTC/USD leg) without the offer quote changing.
        addDisposable(priceSelection.addMarketContextListener(() ->
                update(marketSelection.getMarket(),
                        directionSelection.getDisplayDirection(),
                        priceSelection.getPriceQuote())));

        // The listeners above do not replay their current value; compute from the current state now.
        update(marketSelection.getMarket(),
                directionSelection.getDisplayDirection(),
                priceSelection.getPriceQuote());
    }

    //todo
    public static Fiat getUserSpecificLimitInUsd() {
        return Fiat.fromFaceValue(USER_SPECIFIC_LIMIT_IN_USD, "USD");
    }


    /* --------------------------------------------------------------------- */
    // Update
    /* --------------------------------------------------------------------- */

    private void update(Market market,
                        Direction displayDirection,
                        PriceQuote priceQuote) {
        clearOutputOnMarketChange(market);
        if (!dependenciesValid(market, displayDirection, priceQuote)) {
            return;
        }
        // The rates cache refreshes on every valid update so a later applicability flip can
        // still compute the cap while the live rate is temporarily missing.
        Optional<TradeAmountLimitUtils.Rates> resolvedRates = resolveRates(market);
        // Applicability is decided independently of rate availability: when the cap does not
        // apply it must be cleared even while a rate is unavailable, otherwise a stale cap from
        // an earlier buy-side state would survive a direction or market change.
        if (!(market.isBtcFiatMarket() && displayDirection.isBuy())) {
            tradeAmountLimit.set(Optional.empty());
            return;
        }
        // A missing rate retains the previous converted output instead of throwing inside a map
        // observer.
        resolvedRates.ifPresent(rates -> {
            Fiat userSpecificLimitInUsd = getUserSpecificLimitInUsd();
            TradeAmount limit = TradeAmountLimitUtils.toTradeAmountLimit(rates, market, priceQuote, userSpecificLimitInUsd);
            tradeAmountLimit.set(Optional.of(limit));
            outputMarket = market;
        });
    }

    private void clearOutputOnMarketChange(Market market) {
        // Fail-soft retention only spans rate gaps within one market; another market's
        // converted output must not survive a market change.
        if (market != null && outputMarket != null && !market.equals(outputMarket)) {
            tradeAmountLimit.set(Optional.empty());
            outputMarket = null;
        }
    }


    private static boolean dependenciesValid(Market market, Direction displayDirection, PriceQuote priceQuote) {
        return market != null &&
                displayDirection != null &&
                priceQuote != null &&
                market.equals(priceQuote.getMarket());
    }

    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    ReadOnlyObservable<Optional<TradeAmount>> tradeAmountLimitObservable() {
        return tradeAmountLimit;
    }

    Optional<TradeAmount> getTradeAmountLimit() {
        return tradeAmountLimit.get();
    }
}
