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

package bisq.offer.mu_sig.use_case.create_offer.amount;

import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.MathUtils;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecFactory;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.AmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class AmountSelection extends LifecycleScope {
    public static final Fiat DEFAULT_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(500, "USD");

    @Delegate
    private final CreateOfferAmountModel model;

    private final MarketPriceService marketPriceService;
    private final MarketSelection marketSelection;
    private final PriceSelection priceSelection;
    private final CreateOfferDraftCookieStore cookieStore;
    @Getter
    private final AmountLimitsProvider amountLimits;

    public AmountSelection(MarketPriceService marketPriceService,
                           MarketSelection marketSelection,
                           DirectionSelection directionSelection,
                           PaymentMethodSelection paymentMethodService,
                           PriceSelection priceSelection,
                           CreateOfferDraftCookieStore cookieStore) {
        this.marketPriceService = checkNotNull(marketPriceService, "marketPriceService must not be null");
        this.marketSelection = checkNotNull(marketSelection, "marketUseCase must not be null");
        this.priceSelection = checkNotNull(priceSelection, "priceUseCase must not be null");
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");

        this.model = new CreateOfferAmountModel();

        amountLimits = new AmountLimitsProvider(marketPriceService,
                marketSelection,
                directionSelection,
                paymentMethodService,
                priceSelection);
    }

    @Override
    public void initialize() {
        amountLimits.initialize();

        boolean useRangeAmount = cookieStore.getUseRangeAmount();
        model.setUseRangeAmount(useRangeAmount);

        Market market = marketSelection.getMarket();
        if (market != null) {
            boolean useBaseCurrencyForAmountInput = cookieStore.getUseBaseCurrencyForAmountInput(market);
            model.setUseBaseCurrencyForAmountInput(useBaseCurrencyForAmountInput);

            // Not clamped yet as we do not have established the trade amount limits
            TradeAmount defaultTradeAmount = getDefaultTradeAmountForMarket(market);
            applyDefaultAmountAndSliderValue(defaultTradeAmount);
        }

        addDisposable(amountLimits.initializedObservable().addObserver(initialized -> {
            if (initialized) {
                addDisposable(amountLimits.effectiveTradeAmountLimitsObservable().addObserver(this::handleEffectiveTradeAmountLimitsChange));
                addDisposable(amountLimits.potentialTradeAmountLimitsObservable().addObserver(this::handlePotentialTradeAmountLimitsChange));
                addDisposable(amountLimits.userSpecificAmountLimitObservable().addObserver(this::handleUserSpecificAmountLimitChange));

                addDisposable(marketSelection.addMarketListener(this::handleMarketChange));
                addDisposable(priceSelection.addPriceQuoteListener(this::handlePriceQuoteChange));
                model.setInitialized(true);
            }
        }));
    }

    @Override
    public void dispose() {
        super.dispose();
        amountLimits.dispose();
    }


    /* --------------------------------------------------------------------- */
    // Handle changes from dependencies
    /* --------------------------------------------------------------------- */

    private void handleMarketChange(Market market) {
        if (market != null) {
            TradeAmount defaultTradeAmount = getDefaultTradeAmountForMarket(market);
            applyDefaultAmountAndSliderValue(defaultTradeAmount);
        }
    }

    private void handlePriceQuoteChange(PriceQuote priceQuote) {
        if (priceQuote != null) {
            applyPriceQuoteToFixTradeAmount(priceQuote);
            applyPriceQuoteToMinTradeAmount(priceQuote);
            applyPriceQuoteToMaxTradeAmount(priceQuote);
        }
    }

    private void applyPriceQuoteToFixTradeAmount(PriceQuote priceQuote) {
        TradeAmount fixTradeAmount = getFixTradeAmount();
        TradeAmount newFixTradeAmount = applyPriceQuoteToPassiveSide(fixTradeAmount, priceQuote);
        model.setFixTradeAmount(newFixTradeAmount);
    }

    private void applyPriceQuoteToMinTradeAmount(PriceQuote priceQuote) {
        TradeAmount minTradeAmount = getMinTradeAmount();
        TradeAmount newMinTradeAmount = applyPriceQuoteToPassiveSide(minTradeAmount, priceQuote);
        model.setMinTradeAmount(newMinTradeAmount);
    }

    private void applyPriceQuoteToMaxTradeAmount(PriceQuote priceQuote) {
        TradeAmount maxTradeAmount = getMaxTradeAmount();
        TradeAmount newMaxTradeAmount = applyPriceQuoteToPassiveSide(maxTradeAmount, priceQuote);
        model.setMaxTradeAmount(newMaxTradeAmount);
    }


    private void handlePotentialTradeAmountLimitsChange(TradeAmountRange potentialTradeAmountLimits) {
        if (potentialTradeAmountLimits != null) {
            applyPotentialTradeAmountLimits(potentialTradeAmountLimits);
        }
    }

    private void applyPotentialTradeAmountLimits(TradeAmountRange potentialTradeAmountLimits) {
        MonetaryRange inputAmountRange = toInputSideMonetaryRange(potentialTradeAmountLimits);
        model.setInputAmountRange(inputAmountRange);
        // All slider values are fractions of the input amount range. When the range changes but the
        // corresponding amount values stay equal, their observers do not fire, so we recompute them here.
        // The user-specific limit must be emitted first: UI slider controllers clamp incoming amount
        // slider values against the last emitted limit and feed the result back into this class.
        handleUserSpecificAmountLimitChange(getUserSpecificTradeAmountLimit());
        if (model.getFixTradeAmount() != null) {
            applyFixAmountAndSliderValue(model.getFixTradeAmount());
        }
        if (model.getMinTradeAmount() != null) {
            applyMinAmountAndSliderValue(model.getMinTradeAmount());
        }
        if (model.getMaxTradeAmount() != null) {
            applyMaxAmountAndSliderValue(model.getMaxTradeAmount());
        }
    }


    private void handleUserSpecificAmountLimitChange(Optional<TradeAmount> userSpecificAmountLimit) {
        if (userSpecificAmountLimit != null) {
            applyUserSpecificAmountLimitChange(userSpecificAmountLimit);
        }
    }

    private void applyUserSpecificAmountLimitChange(Optional<TradeAmount> userSpecificAmountLimit) {
        Optional<Double> sliderValue = userSpecificAmountLimit
                .map(this::toInputAmount)
                .map(this::toSliderValueFromAmount);
        model.setUserSpecificTradeAmountLimitAsSliderValue(sliderValue);

    }

    private void handleEffectiveTradeAmountLimitsChange(TradeAmountRange effectiveTradeAmountLimits) {
        if (effectiveTradeAmountLimits != null) {
            applyFixAmountAndSliderValue(model.getFixTradeAmount());
            applyMinAmountAndSliderValue(model.getMinTradeAmount());
            applyMaxAmountAndSliderValue(model.getMaxTradeAmount());
        }
    }


    /* --------------------------------------------------------------------- */
    // User interaction
    /* --------------------------------------------------------------------- */

    public void onSetUseBaseCurrencyForAmountInput(boolean value) {
        model.setUseBaseCurrencyForAmountInput(value);
        Market market = marketSelection.getMarket();
        if (market != null) {
            cookieStore.persistUseBaseCurrencyForAmountInput(market, value);
        }
    }

    public void onSetUseRangeAmount(boolean value) {
        model.setUseRangeAmount(value);
        cookieStore.persistUseRangeAmount(value);
    }


    /* --------------------------------------------------------------------- */
    // Amount as input
    /* --------------------------------------------------------------------- */

    public void onSetFixTradeAmountFromInputAmount(Monetary inputAmount) {
        checkNotNull(inputAmount, "inputAmount must not be null");
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && market != null && priceQuote != null) {
            TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
            applyFixAmountAndSliderValue(tradeAmount);
        }
    }

    public void onSetMinTradeAmountFromInputAmount(Monetary inputAmount) {
        checkNotNull(inputAmount, "inputAmount must not be null");
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && market != null && priceQuote != null) {
            TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
            applyMinAmountAndSliderValue(tradeAmount);
        }
    }

    public void onSetMaxTradeAmountFromInputAmount(Monetary inputAmount) {
        checkNotNull(inputAmount, "inputAmount must not be null");
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && market != null && priceQuote != null) {
            TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
            applyMaxAmountAndSliderValue(tradeAmount);
        }
    }



    /* --------------------------------------------------------------------- */
    // Slider value as input
    /* --------------------------------------------------------------------- */

    public void onSetFixTradeAmountFromSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be between 0 and 1");
        TradeAmount fixTradeAmount = model.getFixTradeAmount();
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && fixTradeAmount != null && market != null && priceQuote != null) {
            TradeAmount newTradeAmount = toTradeAmountFromSliderValue(market, priceQuote, fixTradeAmount, sliderValue);
            model.setFixTradeAmount(newTradeAmount);

            double newSliderValue = toSliderValueFromTradeAmount(newTradeAmount); // We might have got clamped
            model.setFixAmountSliderValue(newSliderValue);
        }
    }

    public void onSetMinTradeAmountFromSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be between 0 and 1");

        TradeAmount minTradeAmount = model.getMinTradeAmount();
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && minTradeAmount != null && market != null && priceQuote != null) {
            TradeAmount newTradeAmount = toTradeAmountFromSliderValue(market, priceQuote, minTradeAmount, sliderValue);
            model.setMinTradeAmount(newTradeAmount);

            double newSliderValue = toSliderValueFromTradeAmount(newTradeAmount); // We might have got clamped
            model.setMinAmountSliderValue(newSliderValue);
        }
    }

    public void onSetMaxTradeAmountFromSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be between 0 and 1");

        TradeAmount maxTradeAmount = model.getMaxTradeAmount();
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        if (amountLimits.isInitialized() && maxTradeAmount != null && market != null && priceQuote != null) {
            TradeAmount newTradeAmount = toTradeAmountFromSliderValue(market, priceQuote, maxTradeAmount, sliderValue);
            model.setMaxTradeAmount(newTradeAmount);

            double newSliderValue = toSliderValueFromTradeAmount(newTradeAmount); // We might have got clamped
            model.setMaxAmountSliderValue(newSliderValue);
        }
    }


    /* --------------------------------------------------------------------- */
    // API for input amount
    /* --------------------------------------------------------------------- */

    public Monetary getFixInputAmount() {
        return toInputAmount(getFixTradeAmount());
    }

    public Monetary getMinInputAmount() {
        return toInputAmount(getMinTradeAmount());
    }

    public Monetary getMaxInputAmount() {
        return toInputAmount(getMaxTradeAmount());
    }

    public Monetary toInputAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        return toInputAmount(tradeAmount, getUseBaseCurrencyForAmountInput());
    }


    /* --------------------------------------------------------------------- */
    // API for passive amount
    /* --------------------------------------------------------------------- */

    public Monetary getFixPassiveAmount() {
        return toPassiveAmount(getFixTradeAmount());
    }

    public Monetary getMinPassiveAmount() {
        return toPassiveAmount(getMinTradeAmount());
    }

    public Monetary getMaxPassiveAmount() {
        return toPassiveAmount(getMaxTradeAmount());
    }


    /* --------------------------------------------------------------------- */
    // AmountSpec
    /* --------------------------------------------------------------------- */

    public AmountSpec createAndGetAmountSpec(Market market) {
        checkNotNull(market, "market must not be null");
        boolean isBtcFiatMarket = market.isBtcFiatMarket();
        boolean useRangeAmount = getUseRangeAmount();
        return AmountSpecFactory.createAmountSpec(isBtcFiatMarket,
                useRangeAmount,
                getMinTradeAmount(),
                getMaxTradeAmount(),
                getFixTradeAmount());
    }


    /* --------------------------------------------------------------------- */
    // Delegates
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<Optional<TradeAmount>> userSpecificTradeAmountLimitObservable() {
        return getAmountLimits().userSpecificAmountLimitObservable();
    }

    public Optional<TradeAmount> getUserSpecificTradeAmountLimit() {
        return userSpecificTradeAmountLimitObservable().get();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    /* --------------------------------------------------------------------- */
    // Apply default amount and slider value
    /* --------------------------------------------------------------------- */

    private TradeAmount getDefaultTradeAmountForMarket(Market market) {
        return getTradeAmountFromUsd(marketPriceService, market, DEFAULT_TRADE_AMOUNT_IN_USD);
    }

    private static TradeAmount getTradeAmountFromUsd(MarketPriceService marketPriceService,
                                                     Market market,
                                                     Fiat usdAmount) {
        return MarketBasedAmountConversion.tradeAmountFromUsdAmount(marketPriceService, market, usdAmount);
    }

    private void applyDefaultAmountAndSliderValue(TradeAmount defaultTradeAmount) {
        model.setFixTradeAmount(defaultTradeAmount);
        model.setMinTradeAmount(defaultTradeAmount);
        model.setMaxTradeAmount(defaultTradeAmount);
        double defaultSliderValue = toSliderValueFromTradeAmount(defaultTradeAmount);
        model.setFixAmountSliderValue(defaultSliderValue);
        model.setMinAmountSliderValue(defaultSliderValue);
        model.setMaxAmountSliderValue(defaultSliderValue);
    }


    /* --------------------------------------------------------------------- */
    // Apply amount and slider value
    /* --------------------------------------------------------------------- */

    private void applyFixAmountAndSliderValue(TradeAmount fixTradeAmount) {
        fixTradeAmount = amountLimits.clamp(fixTradeAmount);
        model.setFixTradeAmount(fixTradeAmount);

        double sliderValue = toSliderValueFromTradeAmount(fixTradeAmount);
        model.setFixAmountSliderValue(sliderValue);
    }

    private void applyMinAmountAndSliderValue(TradeAmount minTradeAmount) {
        minTradeAmount = amountLimits.clamp(minTradeAmount);
        model.setMinTradeAmount(minTradeAmount);

        double sliderValue = toSliderValueFromTradeAmount(minTradeAmount);
        model.setMinAmountSliderValue(sliderValue);
    }

    private void applyMaxAmountAndSliderValue(TradeAmount maxTradeAmount) {
        maxTradeAmount = amountLimits.clamp(maxTradeAmount);
        model.setMaxTradeAmount(maxTradeAmount);

        double sliderValue = toSliderValueFromTradeAmount(maxTradeAmount);
        model.setMaxAmountSliderValue(sliderValue);
    }


    /* --------------------------------------------------------------------- */
    // Slider Utils
    /* --------------------------------------------------------------------- */

    private TradeAmount toTradeAmountFromSliderValue(Market market,
                                                     PriceQuote priceQuote,
                                                     TradeAmount tradeAmount,
                                                     double sliderValue) {
        Monetary inputAmount = toInputAmount(tradeAmount);
        // We do not use the effective trade amount limits as the slider represents the full potential range
        TradeAmountRange potentialTradeAmountLimits = amountLimits.getPotentialTradeAmountLimits();
        MonetaryRange inputAmountRange = toInputSideMonetaryRange(potentialTradeAmountLimits);
        TradeAmount fromSliderValue = toTradeAmountFromSliderValue(market,
                priceQuote,
                inputAmountRange,
                inputAmount,
                sliderValue);
        return amountLimits.clamp(fromSliderValue);
    }

    // TODO add test
    static TradeAmount toTradeAmountFromSliderValue(Market market,
                                                    PriceQuote priceQuote,
                                                    MonetaryRange amountRange,
                                                    Monetary amount,
                                                    double sliderValue) {
        long sliderAmountValue = getAmountValueFromSliderValue(amountRange, sliderValue);
        Monetary sliderAmount = Monetary.from(amount, sliderAmountValue);

        return TradeAmountConversion.toTradeAmount(market, priceQuote, sliderAmount);
    }


    // todo add tests
    public static long getAmountValueFromSliderValue(MonetaryRange amountRange, double sliderValue) {
        long min = amountRange.getMin().getValue();
        long max = amountRange.getMax().getValue();
        long range = max - min;
        return min + Math.round(sliderValue * range);
    }

    private double toSliderValueFromTradeAmount(TradeAmount tradeAmount) {
        MonetaryRange inputAmountRange = model.getInputAmountRange();
        if (inputAmountRange != null) {
            Monetary inputAmount = toInputAmount(tradeAmount);
            return toSliderValueFromAmount(inputAmount, inputAmountRange);
        } else {
            return 0;
        }
    }

    private double toSliderValueFromAmount(Monetary amount) {
        MonetaryRange inputAmountRange = model.getInputAmountRange();
        if (inputAmountRange != null) {
            return toSliderValueFromAmount(amount, inputAmountRange);
        } else {
            return 0;
        }
    }

    // todo add tests
    public static double toSliderValueFromAmount(Monetary amount, MonetaryRange amountRange) {
        long min = amountRange.getMin().getValue();
        long max = amountRange.getMax().getValue();
        double range = max - min;
        double sliderValue = range != 0 ? (amount.getValue() - min) / range : 0;
        return MathUtils.bounded(0, 1, sliderValue);
    }


    /* --------------------------------------------------------------------- */
    // Utils for selecting input side
    /* --------------------------------------------------------------------- */

    private MonetaryRange toInputSideMonetaryRange(TradeAmountRange amountLimits) {
        return new MonetaryRange(toInputAmount(amountLimits.getMin()), toInputAmount(amountLimits.getMax()));
    }


    private static Monetary toInputAmount(TradeAmount tradeAmount, boolean useBaseCurrencyForAmountInput) {
        if (useBaseCurrencyForAmountInput) {
            return tradeAmount.getBaseSideAmount();
        } else {
            return tradeAmount.getQuoteSideAmount();
        }
    }


    /* --------------------------------------------------------------------- */
    // Utils for selecting passive (non-input) side
    /* --------------------------------------------------------------------- */

    private Monetary toPassiveAmount(TradeAmount tradeAmount) {
        return toPassiveAmount(tradeAmount, getUseBaseCurrencyForAmountInput());
    }

    private static Monetary toPassiveAmount(TradeAmount tradeAmount, boolean useBaseCurrencyForAmountInput) {
        if (useBaseCurrencyForAmountInput) {
            return tradeAmount.getQuoteSideAmount();
        } else {
            return tradeAmount.getBaseSideAmount();
        }
    }

    private TradeAmount applyPriceQuoteToPassiveSide(TradeAmount tradeAmount, PriceQuote priceQuote) {
        return applyPriceQuoteToPassiveSide(tradeAmount, priceQuote, getUseBaseCurrencyForAmountInput());
    }

    // TODO add tests
    public static TradeAmount applyPriceQuoteToPassiveSide(TradeAmount tradeAmount,
                                                    PriceQuote priceQuote,
                                                    boolean useBaseCurrencyForAmountInput) {
        Monetary baseSideMonetary, quoteSideMonetary;
        if (useBaseCurrencyForAmountInput) {
            baseSideMonetary = tradeAmount.getBaseSideAmount();
            quoteSideMonetary = priceQuote.toQuoteSideMonetary(baseSideMonetary);
        } else {
            quoteSideMonetary = tradeAmount.getQuoteSideAmount();
            baseSideMonetary = priceQuote.toBaseSideMonetary(quoteSideMonetary);
        }
        return new TradeAmount(baseSideMonetary, quoteSideMonetary);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */


    private boolean dependenciesValid() {
        Market market = marketSelection.getMarket();
        PriceQuote priceQuote = priceSelection.getPriceQuote();
        TradeAmountRange effectiveAmountLimits = amountLimits.getEffectiveAmountLimits();

        return dependenciesValid(market, priceQuote, effectiveAmountLimits);
    }

    // TODO add tests
    static boolean dependenciesValid(Market market,
                                     PriceQuote priceQuote,
                                     TradeAmountRange effectiveAmountLimits) {
        if (market == null ||
                priceQuote == null ||
                !market.equals(priceQuote.getMarket())) {
            return false;
        }

        if (effectiveAmountLimits == null) {
            return true;
        }

        TradeAmount min = effectiveAmountLimits.getMin();
        TradeAmount max = effectiveAmountLimits.getMax();
        return min != null &&
                max != null &&
                matchingMarket(market, min) &&
                matchingMarket(market, max);
    }

    // TODO add tests
    static boolean matchingMarket(Market market, TradeAmount tradeAmount) {
        return market.getBaseCurrencyCode().equals(tradeAmount.getBaseSideAmount().getCode()) &&
                market.getQuoteCurrencyCode().equals(tradeAmount.getQuoteSideAmount().getCode());
    }
}
