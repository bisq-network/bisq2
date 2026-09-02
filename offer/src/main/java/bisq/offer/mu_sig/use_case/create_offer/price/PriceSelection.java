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

package bisq.offer.mu_sig.use_case.create_offer.price;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.common.util.MathUtils;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.limits.PriceLimits;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Coordinates price state for the create-offer draft.
 * <p>
 * The authoritative side depends on the pricing mode: with a floating price the percentage is
 * authoritative and the quote is derived from it against the current market price; with a fixed
 * price the quote is authoritative and the percentage only mirrors the current market deviation
 * for display. Derived values are never fed back as input to the reverse calculation, so a fixed
 * quote survives market updates verbatim and a floating percentage cannot drift through
 * quote/percentage rounding.
 * <p>
 * This use case owns the market-price reaction: on every market-price update it derives the
 * allowed price range and, for a floating price, the refreshed quote from that same update, so
 * the clamp can never run against limits from an older price. Quote listeners fire only when the
 * final clamped quote actually changed; market-context listeners fire instead when a market-price
 * update did not change the quote, so amount-limit providers can refresh conversions that depend
 * on other rates (for example the BTC/USD leg) even while the offer quote is stable.
 * <p>
 * All state transitions synchronize on the draft lock shared across the create-offer draft:
 * market-price updates arrive on the market data thread while user input arrives on the UI
 * thread, and downstream amount state is recomputed synchronously inside the transitions.
 * <p>
 * When the draft is materialized, fixed prices are represented as {@link FixPriceSpec}, a zero
 * floating offset becomes {@link MarketPriceSpec}, and non-zero offsets become
 * {@link FloatPriceSpec}.
 */
@Slf4j
public class PriceSelection extends LifecycleScope {
    private final CreateOfferPriceModel model;
    private final MarketPriceService marketPriceService;
    private final MarketSelection marketSelection;
    private final CreateOfferDraftCookieStore cookieStore;
    private final Object draftLock;
    private final Set<Consumer<PriceQuote>> quoteListeners = new CopyOnWriteArraySet<>();
    private final Set<Runnable> marketContextListeners = new CopyOnWriteArraySet<>();
    // The market quote observed by the last transition, guarded by the draft lock. The draft
    // snapshot captures this value so it can never pair a resolved quote with a newer raw map
    // entry whose transition is still waiting for the lock.
    private Optional<PriceQuote> observedMarketPriceQuote = Optional.empty();

    public PriceSelection(MarketPriceService marketPriceService,
                          MarketSelection marketSelection,
                          CreateOfferDraftCookieStore cookieStore,
                          Object draftLock) {
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
        this.marketSelection = checkNotNull(marketSelection, "marketSelection must not be null");
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        this.draftLock = checkNotNull(draftLock, "draftLock must not be null");
        this.model = new CreateOfferPriceModel();
    }

    @Override
    public void initialize() {
        synchronized (draftLock) {
            Market market = marketSelection.getMarket();
            if (market != null) {
                restoreForMarket(market);
            }
            addDisposable(marketSelection.addMarketListener(this::handleMarketChange));
            addDisposable(marketPriceService.getMarketPriceByCurrencyMap().addObserver(this::handleMarketPriceUpdate));
        }
    }


    /* --------------------------------------------------------------------- */
    // Market and market-price transitions
    /* --------------------------------------------------------------------- */

    private void handleMarketChange(Market market) {
        // Invoked from MarketSelection.onSetMarket, which already holds the draft lock. Market
        // changes have their own state-transition path: publishing the intermediate price change
        // here would try to recompute the previous market's amounts against the new market
        // before the draft state engine resets them, so quote listeners are deliberately not
        // notified.
        if (market == null) {
            return;
        }
        restoreForMarket(market);
    }

    private void restoreForMarket(Market market) {
        boolean useFixPrice = cookieStore.getUseFixPrice(market);
        model.setUseFixPrice(useFixPrice);

        double persisted = cookieStore.getPricePercentage(market);
        double pricePercentage = Double.isFinite(persisted) ? PriceLimits.clamp(persisted) : 0d;
        if (Double.compare(pricePercentage, persisted) != 0) {
            // Normalize an invalid persisted value once.
            cookieStore.persistPricePercentage(market, pricePercentage);
        }
        model.setPricePercentage(pricePercentage);

        Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
        observedMarketPriceQuote = marketQuote;
        if (marketQuote.isPresent()) {
            model.setPriceQuote(PriceLimits.clamp(
                    PriceUtil.fromMarketPriceMarkup(marketQuote.get(), model.getPricePercentage()),
                    PriceLimits.rangeFor(marketQuote.get())));
        } else {
            // Never retain a quote from another market; the next valid update seeds this one.
            model.setPriceQuote(null);
        }
    }

    private void handleMarketPriceUpdate() {
        synchronized (draftLock) {
            Market market = marketSelection.getMarket();
            if (market == null) {
                return;
            }
            Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
            if (marketQuote.isEmpty()) {
                // Retain the last coherent state; a later valid update reconciles it.
                return;
            }
            observedMarketPriceQuote = marketQuote;
            boolean quoteChanged = false;
            if (model.getUseFixPrice()) {
                PriceQuote fixedQuote = model.getPriceQuote();
                if (fixedQuote == null) {
                    // The market's first price arrives after restoration: seed the fixed quote
                    // from the persisted restoration percentage. The seeded quote is quantized,
                    // so the displayed deviation is recomputed from it - the cookie percentage
                    // may no longer describe the quote it produced.
                    quoteChanged = publishQuote(PriceLimits.clamp(
                            PriceUtil.fromMarketPriceMarkup(marketQuote.get(), model.getPricePercentage()),
                            PriceLimits.rangeFor(marketQuote.get())));
                    model.setPricePercentage(
                            PriceUtil.getPercentageToMarketPrice(marketQuote.get(), model.getPriceQuote()));
                } else {
                    // The fixed quote is authoritative and stays untouched; only the displayed
                    // deviation follows the market. Never persisted.
                    model.setPricePercentage(PriceUtil.getPercentageToMarketPrice(marketQuote.get(), fixedQuote));
                }
            } else {
                PriceQuote candidate = PriceUtil.fromMarketPriceMarkup(marketQuote.get(), model.getPricePercentage());
                quoteChanged = publishQuote(PriceLimits.clamp(candidate, PriceLimits.rangeFor(marketQuote.get())));
            }
            if (!quoteChanged) {
                // The offer quote is unchanged, but conversions depending on other rates (e.g.
                // the BTC/USD leg of the amount limits) may not be: let providers refresh.
                marketContextListeners.forEach(Runnable::run);
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // User input
    /* --------------------------------------------------------------------- */

    public void onSetPricePercentage(double pricePercentage) {
        checkArgument(Double.isFinite(pricePercentage), "pricePercentage must be finite");
        synchronized (draftLock) {
            if (model.getUseFixPrice()) {
                // While the fixed price is authoritative the percentage is only its display
                // deviation; accepting it as input would rewrite the fixed quote.
                return;
            }
            Market market = marketSelection.getMarket();
            if (market == null) {
                return;
            }
            Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
            if (marketQuote.isEmpty()) {
                log.warn("Ignoring percentage input; no market price available for {}", market.getMarketCodes());
                return;
            }
            observedMarketPriceQuote = marketQuote;
            // Rounded to the same 4-decimal precision as PriceUtil.getPercentageToMarketPrice so
            // slider arithmetic noise (e.g. 0.19999999999999998) never reaches the spec or cookie.
            double clamped = MathUtils.roundDouble(PriceLimits.clamp(pricePercentage), 4);
            model.setPricePercentage(clamped);
            cookieStore.persistPricePercentage(market, clamped);
            PriceQuote candidate = PriceUtil.fromMarketPriceMarkup(marketQuote.get(), clamped);
            publishQuote(PriceLimits.clamp(candidate, PriceLimits.rangeFor(marketQuote.get())));
        }
    }

    public void onSetFixedPriceQuote(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "priceQuote must not be null");
        synchronized (draftLock) {
            if (!model.getUseFixPrice()) {
                // Quote input is only meaningful while the fixed price is authoritative.
                return;
            }
            if (priceQuote.equals(model.getPriceQuote())) {
                // A re-commit of the unchanged value (e.g. on focus loss) is not an edit;
                // clamping it against a range the market has moved since would rewrite the
                // fixed offer on a no-op blur.
                return;
            }
            Market market = marketSelection.getMarket();
            if (market == null) {
                return;
            }
            Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
            if (marketQuote.isEmpty()) {
                log.warn("Ignoring price input; no market price available for {}", market.getMarketCodes());
                return;
            }
            observedMarketPriceQuote = marketQuote;
            PriceQuote clamped = PriceLimits.clamp(priceQuote, PriceLimits.rangeFor(marketQuote.get()));
            publishQuote(clamped);
            double deviation = PriceUtil.getPercentageToMarketPrice(marketQuote.get(), clamped);
            model.setPricePercentage(deviation);
            // The bounded equivalent percentage is the per-market restoration value; there is no
            // fixed-quote cookie.
            cookieStore.persistPricePercentage(market, PriceLimits.clamp(deviation));
        }
    }

    public void onSetUseFixPrice(boolean useFixPrice) {
        synchronized (draftLock) {
            if (useFixPrice == model.getUseFixPrice()) {
                return;
            }
            Market market = marketSelection.getMarket();
            if (market == null) {
                return;
            }
            if (useFixPrice) {
                cookieStore.persistUseFixPrice(market, true);
                // Floating -> fixed: the currently resolved quote becomes the fixed price
                // verbatim; the percentage becomes its display deviation.
                model.setUseFixPrice(true);
                Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
                PriceQuote quote = model.getPriceQuote();
                if (marketQuote.isPresent() && quote != null) {
                    observedMarketPriceQuote = marketQuote;
                    model.setPricePercentage(PriceUtil.getPercentageToMarketPrice(marketQuote.get(), quote));
                }
            } else {
                // Fixed -> floating adopts the clamped deviation as the authoritative percentage
                // and derives its quote. Without a usable market context every dependent value
                // would go inconsistent, so the transition is refused until a price is available.
                Optional<PriceQuote> marketQuote = findPositiveMarketPriceQuote(market);
                if (marketQuote.isEmpty()) {
                    return;
                }
                cookieStore.persistUseFixPrice(market, false);
                double current = model.getPricePercentage();
                double adopted = PriceLimits.clamp(Double.isFinite(current) ? current : 0d);
                model.setUseFixPrice(false);
                model.setPricePercentage(adopted);
                cookieStore.persistPricePercentage(market, adopted);
                observedMarketPriceQuote = marketQuote;
                publishQuote(PriceLimits.clamp(
                        PriceUtil.fromMarketPriceMarkup(marketQuote.get(), adopted),
                        PriceLimits.rangeFor(marketQuote.get())));
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // Publication
    /* --------------------------------------------------------------------- */

    private boolean publishQuote(PriceQuote finalQuote) {
        if (finalQuote.equals(model.getPriceQuote())) {
            return false;
        }
        model.setPriceQuote(finalQuote);
        quoteListeners.forEach(listener -> listener.accept(finalQuote));
        return true;
    }

    public PriceSpec createAndGetPriceSpec() {
        synchronized (draftLock) {
            if (getUseFixPrice()) {
                return new FixPriceSpec(checkNotNull(getPriceQuote(), "priceQuote must not be null"));
            }
            double pricePercentage = getPricePercentage();
            if (pricePercentage == 0d) {
                return new MarketPriceSpec();
            }
            return new FloatPriceSpec(pricePercentage);
        }
    }

    public Pin addPriceQuoteListener(Consumer<PriceQuote> listener) {
        checkNotNull(listener, "listener must not be null");
        quoteListeners.add(listener);
        return () -> quoteListeners.remove(listener);
    }

    public Pin addMarketContextListener(Runnable listener) {
        checkNotNull(listener, "listener must not be null");
        marketContextListeners.add(listener);
        return () -> marketContextListeners.remove(listener);
    }

    public Optional<PriceQuote> getObservedMarketPriceQuote() {
        synchronized (draftLock) {
            return observedMarketPriceQuote;
        }
    }

    private Optional<PriceQuote> findPositiveMarketPriceQuote(Market market) {
        return marketPriceService.findMarketPriceQuote(market)
                .filter(quote -> quote.getValue() > 0);
    }


    /* --------------------------------------------------------------------- */
    // Read model
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<PriceQuote> priceQuoteObservable() {
        return model.priceQuoteObservable();
    }

    public PriceQuote getPriceQuote() {
        return model.getPriceQuote();
    }

    public ReadOnlyObservable<Boolean> useFixPriceObservable() {
        return model.useFixPriceObservable();
    }

    public boolean getUseFixPrice() {
        return model.getUseFixPrice();
    }

    public ReadOnlyObservable<Double> pricePercentageObservable() {
        return model.pricePercentageObservable();
    }

    public double getPricePercentage() {
        return model.getPricePercentage();
    }
}
