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

package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.PaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal state-transition engine for {@link CreateOfferDraft}.
 * <p>
 * Design: this package-local component applies market/direction/price/input-mode transitions in a
 * deterministic order, recomputes derived constraints, and keeps draft amount fields/clamp state
 * consistent. The workflow remains a user-facing facade and persistence coordinator.
 */
class CreateOfferDraftStateEngine {
    private final CreateOfferDraft offerDraft;
    private final CreateOfferDraftMarketData marketData;
    private final TradeAmountConstraintsService tradeAmountConstraintsService;
    private final AmountMappingService amountMappingService;
    private final Supplier<PaymentRail> selectedPaymentRailSupplier;
    private final Runnable updatePaymentMethodsHandler;
    private final Fiat defaultTradeAmountInUsd;

    /* --------------------------------------------------------------------- */
    // Construction
    /* --------------------------------------------------------------------- */

    CreateOfferDraftStateEngine(CreateOfferDraft offerDraft,
                                CreateOfferDraftMarketData marketData,
                                TradeAmountConstraintsService tradeAmountConstraintsService,
                                AmountMappingService amountMappingService,
                                Supplier<PaymentRail> selectedPaymentRailSupplier,
                                Runnable updatePaymentMethodsHandler,
                                Fiat defaultTradeAmountInUsd) {
        this.offerDraft = checkNotNull(offerDraft, "offerDraft must not be null");
        this.marketData = checkNotNull(marketData, "marketData must not be null");
        this.tradeAmountConstraintsService = checkNotNull(tradeAmountConstraintsService, "tradeAmountConstraintsService must not be null");
        this.amountMappingService = checkNotNull(amountMappingService, "amountMappingService must not be null");
        this.selectedPaymentRailSupplier = checkNotNull(selectedPaymentRailSupplier, "selectedPaymentRailSupplier must not be null");
        this.updatePaymentMethodsHandler = checkNotNull(updatePaymentMethodsHandler, "updatePaymentMethodsHandler must not be null");
        this.defaultTradeAmountInUsd = checkNotNull(defaultTradeAmountInUsd, "defaultTradeAmountInUsd must not be null");
    }

    /* --------------------------------------------------------------------- */
    // State transitions
    /* --------------------------------------------------------------------- */

    void initialize(Market market,
                    Direction direction,
                    boolean useBaseCurrencyForAmountInput,
                    boolean useRangeAmount) {
        checkNotNull(market, "market must not be null");
        checkNotNull(direction, "direction must not be null");

        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);

        offerDraft.setMarket(market);
        offerDraft.setDirection(direction);
        offerDraft.setUseBaseCurrencyForAmountInput(useBaseCurrencyForAmountInput);
        offerDraft.setUseRangeAmount(useRangeAmount);
        offerDraft.setPriceQuote(marketPriceQuote);

        TradeAmountConstraints tradeAmountConstraints = tradeAmountConstraintsService.compute(market,
                direction,
                marketPriceQuote,
                marketPriceQuote,
                getSelectedPaymentRail());
        applyTradeAmountConstraints(tradeAmountConstraints);

        TradeAmount defaultTradeAmount = marketData.getTradeAmountFromUsd(market, defaultTradeAmountInUsd);
        TradeAmount clampedDefaultTradeAmount = clampTradeAmount(defaultTradeAmount, true);
        offerDraft.setFixTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMinTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMaxTradeAmount(clampedDefaultTradeAmount);

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
        updatePaymentMethodsHandler.run();
    }

    void applyMarketChanged(Market market) {
        checkNotNull(market, "market must not be null");
        offerDraft.setMarket(market);
        if (!isDerivedStateInitialized() || offerDraft.getDirection() == null) {
            return;
        }

        Direction direction = offerDraft.getDirection();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);
        offerDraft.setPriceQuote(marketPriceQuote);
        TradeAmountConstraints tradeAmountConstraints = tradeAmountConstraintsService.compute(market,
                direction,
                marketPriceQuote,
                marketPriceQuote,
                getSelectedPaymentRail());
        applyTradeAmountConstraints(tradeAmountConstraints);

        TradeAmount defaultTradeAmount = marketData.getTradeAmountFromUsd(market, defaultTradeAmountInUsd);
        TradeAmount clampedDefaultTradeAmount = clampTradeAmount(defaultTradeAmount, true);
        offerDraft.setFixTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMinTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMaxTradeAmount(clampedDefaultTradeAmount);

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
        updatePaymentMethodsHandler.run();
    }

    boolean applyDirectionChanged(Direction direction) {
        checkNotNull(direction, "direction must not be null");
        offerDraft.setDirection(direction);
        if (!hasPricingContext()) {
            return false;
        }

        Market market = offerDraft.getMarket();
        PriceQuote offerPriceQuote = offerDraft.getPriceQuote();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);
        TradeAmountConstraints tradeAmountConstraints = tradeAmountConstraintsService.compute(market,
                direction,
                offerPriceQuote,
                marketPriceQuote,
                getSelectedPaymentRail());
        applyTradeAmountConstraints(tradeAmountConstraints);

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
        return true;
    }

    void applyPriceQuoteChanged(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "priceQuote must not be null");
        offerDraft.setPriceQuote(priceQuote);
        if (!hasPricingContext()) {
            return;
        }

        Market market = offerDraft.getMarket();
        Direction direction = offerDraft.getDirection();
        TradeAmount fixTradeAmount = offerDraft.getFixTradeAmount();
        TradeAmount minTradeAmount = offerDraft.getMinTradeAmount();
        TradeAmount maxTradeAmount = offerDraft.getMaxTradeAmount();
        TradeAmountRange oldClampLimits = getClampLimits(true);
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);

        TradeAmountConstraints tradeAmountConstraints = tradeAmountConstraintsService.compute(market,
                direction,
                priceQuote,
                marketPriceQuote,
                getSelectedPaymentRail());
        applyTradeAmountConstraints(tradeAmountConstraints);

        TradeAmountRange newClampLimits = getClampLimits(true);
        if (fixTradeAmount != null) {
            offerDraft.setFixTradeAmount(toUpdatedPassiveAmount(market, priceQuote, fixTradeAmount, oldClampLimits, newClampLimits));
        }
        if (minTradeAmount != null) {
            offerDraft.setMinTradeAmount(toUpdatedPassiveAmount(market, priceQuote, minTradeAmount, oldClampLimits, newClampLimits));
        }
        if (maxTradeAmount != null) {
            offerDraft.setMaxTradeAmount(toUpdatedPassiveAmount(market, priceQuote, maxTradeAmount, oldClampLimits, newClampLimits));
        }

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
    }

    boolean applyUseBaseCurrencyForAmountInputChanged(boolean useBaseCurrencyForAmountInput) {
        offerDraft.setUseBaseCurrencyForAmountInput(useBaseCurrencyForAmountInput);
        Direction direction = offerDraft.getDirection();
        if (!isDerivedStateInitialized() || direction == null) {
            return false;
        }

        updateInputAmountLimits(offerDraft.getTradeAmountLimits());
        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
        return true;
    }

    boolean applyUseRangeAmountChanged(boolean useRangeAmount) {
        offerDraft.setUseRangeAmount(useRangeAmount);
        if (!isDerivedStateInitialized()) {
            return false;
        }

        updateAmountSliderValues();
        return true;
    }

    /* --------------------------------------------------------------------- */
    // Amount writes
    /* --------------------------------------------------------------------- */

    void setFixTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setFixTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateFixAmountSliderValue();
        }
    }

    void setMinTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setMinTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateMinAmountSliderValue();
        }
    }

    void setMaxTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setMaxTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateMaxAmountSliderValue();
        }
    }

    void recalculateTradeAmountConstraintsForSelectedPaymentRail() {
        if (!hasPricingContext()) {
            return;
        }

        Market market = offerDraft.getMarket();
        Direction direction = offerDraft.getDirection();
        PriceQuote offerPriceQuote = offerDraft.getPriceQuote();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);

        TradeAmountConstraints tradeAmountConstraints = tradeAmountConstraintsService.compute(market,
                direction,
                offerPriceQuote,
                marketPriceQuote,
                getSelectedPaymentRail());
        applyTradeAmountConstraints(tradeAmountConstraints);

        if (offerDraft.getFixTradeAmount() != null) {
            offerDraft.setFixTradeAmount(clampTradeAmount(offerDraft.getFixTradeAmount(), true));
        }
        if (offerDraft.getMinTradeAmount() != null) {
            offerDraft.setMinTradeAmount(clampTradeAmount(offerDraft.getMinTradeAmount(), true));
        }
        if (offerDraft.getMaxTradeAmount() != null) {
            offerDraft.setMaxTradeAmount(clampTradeAmount(offerDraft.getMaxTradeAmount(), true));
        }

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, offerDraft.getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
    }

    /* --------------------------------------------------------------------- */
    // Package scope helpers used by workflow facade
    /* --------------------------------------------------------------------- */

    TradeAmount toClampedTradeAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        Market market = checkNotNull(offerDraft.getMarket(), "market must not be null");
        PriceQuote priceQuote = checkNotNull(offerDraft.getPriceQuote(), "priceQuote must not be null");
        TradeAmountRange limits = getClampLimits(true);
        return amountMappingService.toTradeAmountFromInputAmount(market, priceQuote, amount, limits);
    }

    TradeAmount toTradeAmountFromSliderValue(TradeAmount tradeAmount, double sliderValue) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        Market market = checkNotNull(offerDraft.getMarket(), "market must not be null");
        PriceQuote priceQuote = checkNotNull(offerDraft.getPriceQuote(), "priceQuote must not be null");
        TradeAmountRange limits = getClampLimits(true);
        MonetaryRange inputAmountLimits = checkNotNull(offerDraft.getInputAmountLimits(), "inputAmountLimits must not be null");
        return amountMappingService.toTradeAmountFromSliderValue(market,
                priceQuote,
                tradeAmount,
                limits,
                inputAmountLimits,
                offerDraft.getUseBaseCurrencyForAmountInput(),
                sliderValue);
    }

    TradeAmountRange getClampLimits(boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange tradeAmountLimits = offerDraft.getTradeAmountLimits();
        Optional<TradeAmount> userSpecificTradeAmountLimit = offerDraft.getUserSpecificTradeAmountLimit();
        return TradeAmountLimits.getClampLimits(tradeAmountLimits,
                userSpecificTradeAmountLimit,
                includeUserSpecificTradeAmountLimit);
    }

    boolean isDerivedStateInitialized() {
        return offerDraft.getTradeAmountLimits() != null && offerDraft.getInputAmountLimits() != null;
    }

    /* --------------------------------------------------------------------- */
    // Internal recalculation helpers
    /* --------------------------------------------------------------------- */

    private boolean hasPricingContext() {
        return offerDraft.getMarket() != null
                && offerDraft.getDirection() != null
                && offerDraft.getPriceQuote() != null
                && isDerivedStateInitialized();
    }

    private void applyTradeAmountConstraints(TradeAmountConstraints tradeAmountConstraints) {
        offerDraft.setTradeAmountLimits(tradeAmountConstraints.tradeAmountLimits());
        offerDraft.setUserSpecificTradeAmountLimit(tradeAmountConstraints.userSpecificTradeAmountLimit());
        updateInputAmountLimits(tradeAmountConstraints.tradeAmountLimits());
    }

    private void updateInputAmountLimits(TradeAmountRange tradeAmountLimits) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        MonetaryRange inputAmountLimits = amountMappingService.toInputAmountLimits(tradeAmountLimits,
                offerDraft.getUseBaseCurrencyForAmountInput());
        offerDraft.setInputAmountLimits(inputAmountLimits);
    }

    private void updateUserSpecificTradeAmountLimitAsSliderValue(Direction direction,
                                                                 Optional<TradeAmount> userSpecificTradeAmountLimit) {
        if (direction.isBuy() && userSpecificTradeAmountLimit.isPresent() && offerDraft.getInputAmountLimits() != null) {
            double sliderValue = toSliderValue(userSpecificTradeAmountLimit.get());
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.of(sliderValue));
        } else {
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.empty());
        }
    }

    private TradeAmount toUpdatedPassiveAmount(Market market,
                                               PriceQuote priceQuote,
                                               TradeAmount tradeAmount,
                                               TradeAmountRange oldClampLimits,
                                               TradeAmountRange newClampLimits) {
        return amountMappingService.toUpdatedPassiveAmount(market,
                priceQuote,
                tradeAmount,
                oldClampLimits,
                newClampLimits,
                offerDraft.getUseBaseCurrencyForAmountInput());
    }

    private double toSliderValue(TradeAmount tradeAmount) {
        TradeAmountRange limits = getClampLimits(true);
        MonetaryRange inputAmountLimits = checkNotNull(offerDraft.getInputAmountLimits(), "inputAmountLimits must not be null");
        return amountMappingService.toSliderValue(tradeAmount,
                limits,
                inputAmountLimits,
                offerDraft.getUseBaseCurrencyForAmountInput());
    }

    /* --------------------------------------------------------------------- */
    // Internal slider helpers
    /* --------------------------------------------------------------------- */

    private void updateAmountSliderValues() {
        if (offerDraft.getFixTradeAmount() != null) {
            updateFixAmountSliderValue();
        }
        if (offerDraft.getMinTradeAmount() != null) {
            updateMinAmountSliderValue();
        }
        if (offerDraft.getMaxTradeAmount() != null) {
            updateMaxAmountSliderValue();
        }
    }

    private void updateFixAmountSliderValue() {
        setFixAmountSliderValue(toSliderValue(offerDraft.getFixTradeAmount()));
    }

    private void updateMinAmountSliderValue() {
        setMinAmountSliderValue(toSliderValue(offerDraft.getMinTradeAmount()));
    }

    private void updateMaxAmountSliderValue() {
        setMaxAmountSliderValue(toSliderValue(offerDraft.getMaxTradeAmount()));
    }

    private void setUserSpecificTradeAmountLimitAsSliderValue(Optional<Double> value) {
        value.ifPresent(v -> checkArgument(v >= 0 && v <= 1, "value must be in range of 0 and 1"));
        offerDraft.setUserSpecificTradeAmountLimitAsSliderValue(value);
    }

    private void setFixAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setFixAmountSliderValue(sliderValue);
    }

    private void setMinAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setMinAmountSliderValue(sliderValue);
    }

    private void setMaxAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setMaxAmountSliderValue(sliderValue);
    }

    private TradeAmount clampTradeAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return TradeAmountLimits.clampTradeAmount(limits, tradeAmount);
    }

    /* --------------------------------------------------------------------- */
    // Internal callbacks
    /* --------------------------------------------------------------------- */

    private PaymentRail getSelectedPaymentRail() {
        return selectedPaymentRailSupplier.get();
    }
}
