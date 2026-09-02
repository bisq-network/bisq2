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
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Coordinates price state for the create-offer draft.
 * <p>
 * The create-offer specification allows two pricing modes: a fixed price quote or a floating
 * price defined as a percentage relative to the current market price. This use case keeps the
 * percentage and quote representations in sync, clamps floating prices to the allowed
 * {@code -10%} to {@code +50%} range, and publishes quote updates so downstream amount and limit
 * calculations can react to price changes.
 * <p>
 * When the draft is materialized, fixed prices are represented as {@link FixPriceSpec}, a zero
 * floating offset becomes {@link MarketPriceSpec}, and non-zero offsets become
 * {@link FloatPriceSpec}.
 */
@Slf4j
public class PriceSelection extends LifecycleScope {

    @Delegate
    private final CreateOfferPriceModel model;
    private final MarketPriceService marketPriceService;
    private final PriceLimits priceLimits;
    private final MarketSelection marketSelection;
    private final CreateOfferDraftCookieStore cookieStore;
    private final Set<Consumer<PriceQuote>> listeners = new CopyOnWriteArraySet<>();

    public PriceSelection(MarketPriceService marketPriceService,
                          MarketSelection marketSelection,
                          CreateOfferDraftCookieStore cookieStore) {
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
        this.marketSelection = checkNotNull(marketSelection, "marketUseCase must not be null");
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");

        priceLimits = new PriceLimits(marketPriceService, marketSelection);
        this.model = new CreateOfferPriceModel();
    }

    @Override
    public void initialize() {
        priceLimits.initialize();

        Market market = marketSelection.getMarket();
        if (market != null) {
            updateUseFixPrice(market, false);

            double pricePercentage = getDefaultPricePercentageForMarket(market);
            updatePriceQuote(pricePercentage, false);
        }

        // Sync pricePercentage with priceQuote from defaultPriceQuoteForMarket
        PriceQuote priceQuote = getPriceQuote();
        if (priceQuote != null) {
            updatePricePercentage(priceQuote, false);
        }

        addDisposable(marketSelection.addMarketListener(this::updateAll));
    }

    @Override
    public void dispose() {
        super.dispose();
        priceLimits.dispose();
    }


    /* --------------------------------------------------------------------- */
    // Update
    /* --------------------------------------------------------------------- */

    private void updateAll(Market market) {
        if (market == null) {
            return;
        }

        // Market changes have their own state-transition path. Publishing the intermediate
        // price change here would try to recompute the previous market's amounts against the
        // new market before the draft state engine resets them.
        updateUseFixPrice(market, false);

        double pricePercentage = getDefaultPricePercentageForMarket(market);
        updatePriceQuote(pricePercentage, false);

        // Sync pricePercentage with priceQuote from defaultPriceQuoteForMarket
        updatePricePercentage(getPriceQuote(), false);
    }

    private void updateUseFixPrice(Market market, boolean notifyListeners) {
        boolean useFixPrice = cookieStore.getUseFixPrice(market);
        applyUseFixPrice(useFixPrice, notifyListeners);
    }

    private void updatePriceQuote(double pricePercentage, boolean notifyListeners) {
        PriceQuote priceQuote = percentageToPriceQuote(pricePercentage);
        applyPriceQuote(priceQuote, notifyListeners);
    }

    private void updatePricePercentage(PriceQuote priceQuote, boolean notifyListeners) {
        double pricePercentage = priceQuoteToPercentage(priceQuote);
        applyPricePercentage(pricePercentage, notifyListeners);
    }



    /* --------------------------------------------------------------------- */
    // User input
    /* --------------------------------------------------------------------- */

    public void onSetUseFixPrice(boolean useFixPrice) {
        applyUseFixPrice(useFixPrice, true);
    }

    private void applyUseFixPrice(boolean useFixPrice, boolean notifyListeners) {
        if (useFixPrice != model.getUseFixPrice()) {
            model.setUseFixPrice(useFixPrice);

            Market market = checkNotNull(marketSelection.getMarket(), "market must not be null");
            cookieStore.persistUseFixPrice(market, useFixPrice);
        }
    }

    public void onSetPricePercentage(double pricePercentage) {
        applyPricePercentage(pricePercentage, true);

        PriceQuote priceQuote = percentageToPriceQuote(getPricePercentage());
        applyPriceQuote(priceQuote, true);
    }

    private void applyPricePercentage(double pricePercentage, boolean notifyListeners) {
        if (Double.compare(pricePercentage, model.getPricePercentage()) != 0) {
            pricePercentage = priceLimits.clamp(pricePercentage);
            model.setPricePercentage(pricePercentage);

            Market market = checkNotNull(marketSelection.getMarket(), "market must not be null");
            cookieStore.persistPricePercentage(market, pricePercentage);
        }
    }

    public void onSetPriceQuote(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "priceQuote must not be null");
        applyPriceQuote(priceQuote, true);

        double pricePercentage = priceQuoteToPercentage(getPriceQuote());
        applyPricePercentage(pricePercentage, false);
    }

    private void applyPriceQuote(PriceQuote priceQuote, boolean notifyListeners) {
        if (!priceQuote.equals(model.getPriceQuote())) {
            PriceQuote clampPriceQuote = priceLimits.clamp(priceQuote);
            model.onSetPriceQuote(clampPriceQuote);
            if (notifyListeners) {
                listeners.forEach(listener -> listener.accept(clampPriceQuote));
            }
        }
    }

    public PriceSpec createAndGetPriceSpec() {
        if (getUseFixPrice()) {
            return new FixPriceSpec(checkNotNull(getPriceQuote(), "priceQuote must not be null"));
        }
        double pricePercentage = getPricePercentage();
        if (pricePercentage == 0d) {
            return new MarketPriceSpec();
        }
        return new FloatPriceSpec(pricePercentage);
    }


    public Pin addPriceQuoteListener(Consumer<PriceQuote> listener) {
        checkNotNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private double getDefaultPricePercentageForMarket(Market market) {
        double pricePercentage = cookieStore.getPricePercentage(market);
        return priceLimits.clamp(pricePercentage);
    }

    private PriceQuote percentageToPriceQuote(double pricePercentage) {
        Market market = checkNotNull(marketSelection.getMarket(), "market must not be null");
        PriceQuote marketPriceQuote = marketPriceService.getMarketPriceQuoteOrThrow(market);
        return PriceUtil.fromMarketPriceMarkup(marketPriceQuote, pricePercentage);
    }

    private double priceQuoteToPercentage(PriceQuote priceQuote) {
        Market market = checkNotNull(marketSelection.getMarket(), "market must not be null");
        PriceQuote marketPriceQuote = marketPriceService.getMarketPriceQuoteOrThrow(market);
        return PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote);
    }

    private static PriceQuote percentageToPriceQuote(PriceQuote marketPriceQuote, double pricePercentage) {
        return PriceUtil.fromMarketPriceMarkup(marketPriceQuote, pricePercentage);
    }

    private static double priceQuoteToPercentage(PriceQuote marketPriceQuote, PriceQuote priceQuote) {
        return PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote);
    }

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
