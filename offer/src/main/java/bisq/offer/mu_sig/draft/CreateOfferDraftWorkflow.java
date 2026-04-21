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

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecFactory;
import bisq.settings.SettingsService;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class CreateOfferDraftWorkflow extends OfferDraftWorkflow<CreateOfferDraft> {
    public static final Fiat DEFAULT_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(500, "USD");

    private final CreateOfferDraftMarketData marketData;
    private final CreateOfferDraftCookieStore cookieStore;
    @Delegate
    protected CreateOfferDraft createOfferDraft;

    public CreateOfferDraftWorkflow(MarketPriceService marketPriceService, SettingsService settingsService) {
        this(new DefaultCreateOfferDraftMarketData(marketPriceService),
                new DefaultCreateOfferDraftCookieStore(settingsService));
    }

    CreateOfferDraftWorkflow(CreateOfferDraftMarketData marketData, CreateOfferDraftCookieStore cookieStore) {
        super(new CreateOfferDraft());

        this.marketData = checkNotNull(marketData, "marketData must not be null");
        this.cookieStore = checkNotNull(cookieStore, "cookieStore must not be null");
        createOfferDraft = offerDraft;
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        addObservers();
        initializeDraft();
    }


    /* --------------------------------------------------------------------- */
    // Observers
    /* --------------------------------------------------------------------- */

    @Override
    protected void addObservers() {
        // no-op by design:
        // internal state transitions are action-based and deterministic.
    }


    /* --------------------------------------------------------------------- */
    // Write methods
    /* --------------------------------------------------------------------- */

    public void setFixTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = toClampedTradeAmount(amount);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = toClampedTradeAmount(amount);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromInputAmount(Monetary amount) {
        TradeAmount tradeAmount = toClampedTradeAmount(amount);
        setMaxTradeAmount(tradeAmount);
    }

    public void setFixTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount fixTradeAmount = checkNotNull(getFixTradeAmount(), "fixTradeAmount must not be null");
        TradeAmount tradeAmount = toTradeAmountFromSliderValue(fixTradeAmount, sliderValue);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount minTradeAmount = checkNotNull(getMinTradeAmount(), "minTradeAmount must not be null");
        TradeAmount tradeAmount = toTradeAmountFromSliderValue(minTradeAmount, sliderValue);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount maxTradeAmount = checkNotNull(getMaxTradeAmount(), "maxTradeAmount must not be null");
        TradeAmount tradeAmount = toTradeAmountFromSliderValue(maxTradeAmount, sliderValue);
        setMaxTradeAmount(tradeAmount);
    }


    /* --------------------------------------------------------------------- */
    // Input/Passive mapping
    /* --------------------------------------------------------------------- */

    public Monetary toInputAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return AmountUtils.toInputAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }

    public Monetary toPassiveAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return AmountUtils.toPassiveAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }


    /* --------------------------------------------------------------------- */
    // Update methods
    /* --------------------------------------------------------------------- */

    private void updateInputAmountLimits(TradeAmountRange tradeAmountLimits) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        MonetaryRange inputAmountLimits = toInputAmountLimits(tradeAmountLimits);
        offerDraft.setInputAmountLimits(inputAmountLimits);
    }

    // Only buyers have a user-specific trade amount limit
    private void updateUserSpecificTradeAmountLimitAsSliderValue(Direction direction,
                                                                 Optional<TradeAmount> userSpecificTradeAmountLimit) {
        if (direction.isBuy() && userSpecificTradeAmountLimit.isPresent() && getInputAmountLimits() != null) {
            double sliderValue = toSliderValue(userSpecificTradeAmountLimit.get());
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.of(sliderValue));
        } else {
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.empty());
        }
    }

    private void updateFixAmountSliderValue() {
        TradeAmount fixTradeAmount = getFixTradeAmount();
        double sliderValue = toSliderValue(fixTradeAmount);
        setFixAmountSliderValue(sliderValue);
    }

    private void updateMinAmountSliderValue() {
        TradeAmount minTradeAmount = getMinTradeAmount();
        double sliderValue = toSliderValue(minTradeAmount);
        setMinAmountSliderValue(sliderValue);
    }

    private void updateMaxAmountSliderValue() {
        TradeAmount maxTradeAmount = getMaxTradeAmount();
        double sliderValue = toSliderValue(maxTradeAmount);
        setMaxAmountSliderValue(sliderValue);
    }


    /* --------------------------------------------------------------------- */
    // Utils
    /* --------------------------------------------------------------------- */

    private void initializeDraft() {
        Market market = MarketRepository.getDefaultBtcFiatMarket();
        Direction direction = cookieStore.getDefaultDirection();
        boolean useBaseCurrencyForAmountInput = cookieStore.getDefaultUseBaseCurrencyForAmountInput(market);
        boolean useRangeAmount = cookieStore.getDefaultUseRangeAmount();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);

        offerDraft.setMarket(market);
        offerDraft.setDirection(direction);
        offerDraft.setUseBaseCurrencyForAmountInput(useBaseCurrencyForAmountInput);
        offerDraft.setUseRangeAmount(useRangeAmount);
        offerDraft.setPriceQuote(marketPriceQuote);

        PricingData pricingData = createPricingData(market, direction, marketPriceQuote, marketPriceQuote);
        applyPricingData(pricingData);

        TradeAmount defaultTradeAmount = marketData.getTradeAmountFromUsd(market, DEFAULT_TRADE_AMOUNT_IN_USD);
        TradeAmount clampedDefaultTradeAmount = clampTradeAmount(defaultTradeAmount, true);
        offerDraft.setFixTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMinTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMaxTradeAmount(clampedDefaultTradeAmount);

        Optional<TradeAmount> userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit();
        updateUserSpecificTradeAmountLimitAsSliderValue(direction, userSpecificTradeAmountLimit);
        updateAmountSliderValues();
    }

    private void applyMarketChanged(Market market) {
        offerDraft.setMarket(market);
        if (!isDerivedStateInitialized() || getDirection() == null) {
            return;
        }

        Direction direction = getDirection();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);
        offerDraft.setPriceQuote(marketPriceQuote);
        PricingData pricingData = createPricingData(market, direction, marketPriceQuote, marketPriceQuote);
        applyPricingData(pricingData);

        TradeAmount defaultTradeAmount = marketData.getTradeAmountFromUsd(market, DEFAULT_TRADE_AMOUNT_IN_USD);
        TradeAmount clampedDefaultTradeAmount = clampTradeAmount(defaultTradeAmount, true);
        offerDraft.setFixTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMinTradeAmount(clampedDefaultTradeAmount);
        offerDraft.setMaxTradeAmount(clampedDefaultTradeAmount);

        updateUserSpecificTradeAmountLimitAsSliderValue(direction, getUserSpecificTradeAmountLimit());
        updateAmountSliderValues();
    }

    private void applyDirectionChanged(Direction direction) {
        offerDraft.setDirection(direction);
        if (!hasPricingContext()) {
            return;
        }

        Market market = getMarket();
        PriceQuote priceQuote = getPriceQuote();
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);
        PricingData pricingData = createPricingData(market, direction, priceQuote, marketPriceQuote);
        applyPricingData(pricingData);
        Optional<TradeAmount> userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit();
        updateUserSpecificTradeAmountLimitAsSliderValue(direction, userSpecificTradeAmountLimit);
        updateAmountSliderValues();
        cookieStore.persistDirection(direction);
    }

    private void applyPriceQuoteChanged(PriceQuote priceQuote) {
        offerDraft.setPriceQuote(priceQuote);
        if (!hasPricingContext()) {
            return;
        }

        Market market = getMarket();
        Direction direction = getDirection();
        TradeAmount fixTradeAmount = getFixTradeAmount();
        TradeAmount minTradeAmount = getMinTradeAmount();
        TradeAmount maxTradeAmount = getMaxTradeAmount();
        TradeAmountRange oldClampLimits = getClampLimits(true);
        PriceQuote marketPriceQuote = marketData.getMarketPriceQuote(market);

        PricingData pricingData = createPricingData(market, direction, priceQuote, marketPriceQuote);
        applyPricingData(pricingData);

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

        Optional<TradeAmount> userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit();
        updateUserSpecificTradeAmountLimitAsSliderValue(direction, userSpecificTradeAmountLimit);
        updateAmountSliderValues();
    }

    private void applyUseBaseCurrencyForAmountInputChanged(Market market, boolean useBaseCurrencyForAmountInput) {
        offerDraft.setUseBaseCurrencyForAmountInput(useBaseCurrencyForAmountInput);
        Direction direction = getDirection();
        if (!isDerivedStateInitialized() || direction == null) {
            return;
        }

        updateInputAmountLimits(getTradeAmountLimits());
        Optional<TradeAmount> userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit();
        updateUserSpecificTradeAmountLimitAsSliderValue(direction, userSpecificTradeAmountLimit);
        updateAmountSliderValues();
        cookieStore.persistUseBaseCurrencyForAmountInput(market, useBaseCurrencyForAmountInput);
    }

    private TradeAmount toUpdatedPassiveAmount(Market market,
                                               PriceQuote priceQuote,
                                               TradeAmount tradeAmount,
                                               TradeAmountRange oldClampLimits,
                                               TradeAmountRange newClampLimits) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        Monetary inputAmount = AmountUtils.toInputAmount(tradeAmount, oldClampLimits, useBaseCurrencyForAmountInput);
        TradeAmount converted = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
        return TradeAmountLimits.clampTradeAmount(newClampLimits, converted);
    }

    private MonetaryRange toInputAmountLimits(TradeAmountRange tradeAmountLimits) {
        boolean useBaseCurrencyForAmountInput = getUseBaseCurrencyForAmountInput();
        TradeAmount min = tradeAmountLimits.getMin();
        TradeAmount max = tradeAmountLimits.getMax();
        Monetary minInputAmount = AmountUtils.toInputAmount(min, tradeAmountLimits, useBaseCurrencyForAmountInput);
        Monetary maxInputAmount = AmountUtils.toInputAmount(max, tradeAmountLimits, useBaseCurrencyForAmountInput);
        return new MonetaryRange(minInputAmount, maxInputAmount);
    }

    private TradeAmount toClampedTradeAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        Market market = checkNotNull(getMarket(), "market must not be null");
        PriceQuote priceQuote = checkNotNull(getPriceQuote(), "priceQuote must not be null");
        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, amount);
        return clampTradeAmount(tradeAmount, true);
    }

    private double toSliderValue(TradeAmount tradeAmount) {
        Monetary inputAmount = toInputAmount(tradeAmount, true);
        MonetaryRange inputAmountLimits = checkNotNull(getInputAmountLimits(), "inputAmountLimits must not be null");
        return AmountUtils.toSliderValue(inputAmount, inputAmountLimits);
    }

    private TradeAmount toTradeAmountFromSliderValue(TradeAmount tradeAmount, double sliderValue) {
        Market market = checkNotNull(getMarket(), "market must not be null");
        PriceQuote priceQuote = checkNotNull(getPriceQuote(), "priceQuote must not be null");
        MonetaryRange inputAmountLimits = checkNotNull(getInputAmountLimits(), "inputAmountLimits must not be null");
        Monetary inputAmount = toInputAmount(tradeAmount, true);
        TradeAmount fromSliderValue = AmountUtils.toTradeAmountFromSliderValue(market, priceQuote, inputAmountLimits, inputAmount, sliderValue);
        return clampTradeAmount(fromSliderValue, true);
    }

    private PricingData createPricingData(Market market,
                                          Direction direction,
                                          PriceQuote offerPriceQuote,
                                          PriceQuote marketPriceQuote) {
        PriceQuote btcUsdPriceQuote = marketData.getBtcUsdPriceQuote();

        TradeAmountRange tradeAmountLimits = TradeAmountLimits.toTradeAmountLimits(market,
                offerPriceQuote,
                btcUsdPriceQuote,
                marketPriceQuote,
                TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD,
                TradeAmountLimits.getMaxTradeAmountInUsd());

        Optional<TradeAmount> userSpecificTradeAmountLimit = TradeAmountLimits.toUserSpecificTradeAmountLimit(direction,
                market,
                offerPriceQuote,
                btcUsdPriceQuote,
                marketPriceQuote,
                TradeAmountLimits.getUserSpecificLimitInUsdAmount());
        return new PricingData(tradeAmountLimits, userSpecificTradeAmountLimit);
    }

    private void applyPricingData(PricingData pricingData) {
        offerDraft.setTradeAmountLimits(pricingData.tradeAmountLimits());
        offerDraft.setUserSpecificTradeAmountLimit(pricingData.userSpecificTradeAmountLimit());
        updateInputAmountLimits(pricingData.tradeAmountLimits());
    }

    private void updateAmountSliderValues() {
        if (getFixTradeAmount() != null) {
            updateFixAmountSliderValue();
        }
        if (getMinTradeAmount() != null) {
            updateMinAmountSliderValue();
        }
        if (getMaxTradeAmount() != null) {
            updateMaxAmountSliderValue();
        }
    }

    private boolean isDerivedStateInitialized() {
        return getTradeAmountLimits() != null && getInputAmountLimits() != null;
    }

    private boolean hasPricingContext() {
        return getMarket() != null && getDirection() != null && getPriceQuote() != null && isDerivedStateInitialized();
    }


    /* --------------------------------------------------------------------- */
    // Clamp to limits
    /* --------------------------------------------------------------------- */

    private TradeAmount clampTradeAmount(TradeAmount tradeAmount, boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange limits = getClampLimits(includeUserSpecificTradeAmountLimit);
        return TradeAmountLimits.clampTradeAmount(limits, tradeAmount);
    }

    private TradeAmountRange getClampLimits(boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange tradeAmountLimits = getTradeAmountLimits();
        Optional<TradeAmount> userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit();
        return TradeAmountLimits.getClampLimits(tradeAmountLimits,
                userSpecificTradeAmountLimit,
                includeUserSpecificTradeAmountLimit);
    }


    /* --------------------------------------------------------------------- */
    // Setters
    /* --------------------------------------------------------------------- */

    public void setMarket(Market market) {
        checkNotNull(market, "Market must not be null");
        applyMarketChanged(market);
    }

    public void setDirection(Direction direction) {
        checkNotNull(direction, "Direction must not be null");
        applyDirectionChanged(direction);
    }

    public void clearSelectedAccountByPaymentMethod() {
        offerDraft.clearSelectedAccountByPaymentMethod();
    }

    public void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        offerDraft.putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod);
    }


    public void setPriceQuote(PriceQuote priceQuote) {
        checkNotNull(priceQuote, "PriceQuote must not be null");
        applyPriceQuoteChanged(priceQuote);
    }

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        Market market = getMarket();
        if (market == null) {
            offerDraft.setUseBaseCurrencyForAmountInput(value);
        } else {
            applyUseBaseCurrencyForAmountInputChanged(market, value);
        }
    }

    public void setUseRangeAmount(boolean useRangeAmount) {
        offerDraft.setUseRangeAmount(useRangeAmount);
        if (isDerivedStateInitialized()) {
            cookieStore.persistUseRangeAmount(useRangeAmount);
        }
    }

    public void setFixTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setFixTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateFixAmountSliderValue();
        }
    }

    public void setMinTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setMinTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateMinAmountSliderValue();
        }
    }

    public void setMaxTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        TradeAmount valueToSet = isDerivedStateInitialized() ? clampTradeAmount(tradeAmount, true) : tradeAmount;
        offerDraft.setMaxTradeAmount(valueToSet);
        if (isDerivedStateInitialized()) {
            updateMaxAmountSliderValue();
        }
    }

    public void setTradeAmountLimits(TradeAmountRange tradeAmountRange) {
        checkNotNull(tradeAmountRange, "TradeAmountRange must not be null");
        offerDraft.setTradeAmountLimits(tradeAmountRange);
    }

    public void setUserSpecificTradeAmountLimit(Optional<TradeAmount> tradeAmount) {
        tradeAmount.ifPresent(amount -> checkNotNull(amount, "tradeAmount must not be null"));
        offerDraft.setUserSpecificTradeAmountLimit(tradeAmount);
    }

    void setUserSpecificTradeAmountLimitAsSliderValue(Optional<Double> value) {
        value.ifPresent(v -> checkArgument(v >= 0 && v <= 1, "value must be in range of 0 and 1"));
        offerDraft.setUserSpecificTradeAmountLimitAsSliderValue(value);
    }

    public void setInputAmountLimits(MonetaryRange inputAmountLimits) {
        checkNotNull(inputAmountLimits, "inputAmountLimits must not be null");
        offerDraft.setInputAmountLimits(inputAmountLimits);
    }

    void setFixAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setFixAmountSliderValue(sliderValue);
    }

    void setMinAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setMinAmountSliderValue(sliderValue);
    }

    void setMaxAmountSliderValue(double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");
        offerDraft.setMaxAmountSliderValue(sliderValue);
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    public AmountSpec getAmountSpec() {
        boolean isBtcFiatMarket = getMarket().isBtcFiatMarket();
        boolean useRangeAmount = getUseRangeAmount();
        return AmountSpecFactory.createAmountSpec(isBtcFiatMarket,
                useRangeAmount,
                getMinTradeAmount(),
                getMaxTradeAmount(),
                getFixTradeAmount());
    }

    private record PricingData(TradeAmountRange tradeAmountLimits,
                               Optional<TradeAmount> userSpecificTradeAmountLimit) {
    }
}
