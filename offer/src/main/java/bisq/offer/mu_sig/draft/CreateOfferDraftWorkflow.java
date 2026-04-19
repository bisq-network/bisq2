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
import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
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
import bisq.common.util.MathUtils;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecFactory;
import bisq.offer.amount.spec.BaseSideAmountSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class CreateOfferDraftWorkflow extends OfferDraftWorkflow<CreateOfferDraft> {
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    @Delegate
    protected CreateOfferDraft createOfferDraft;

    public CreateOfferDraftWorkflow(MarketPriceService marketPriceService, SettingsService settingsService) {
        super(new CreateOfferDraft());
        this.marketPriceService = marketPriceService;
        this.settingsService = settingsService;
        createOfferDraft = offerDraft;
    }

    @Override
    protected void initialize() {
        // Default
        Market market = MarketRepository.getDefaultBtcFiatMarket();
        setMarket(market);

        setDirection(Direction.BUY);
        Boolean useBuyDirection = settingsService.getCookie().asBoolean(CookieKey.MU_SIG_CREATE_OFFER_USE_BUY_DIRECTION)
                .orElse(false);
        setDirection(useBuyDirection ? Direction.BUY : Direction.SELL);

        if (market.isBtcFiatMarket()) {
            setUseBaseCurrencyForAmountInput(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(false));
        } else {
            setUseBaseCurrencyForAmountInput(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(true));
        }

        offerDraft.setUseRangeAmount(settingsService.getCookie().asBoolean(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED)
                .orElse(false));

        // Price
        PriceQuote priceQuote = marketPriceService.findMarketPriceQuote(market).orElseThrow();
        setPriceQuote(priceQuote);
        setPriceSpec(new MarketPriceSpec());

        // Amounts
        updateDefaultTradeAmount();
        TradeAmount defaultTradeAmount = getDefaultTradeAmount();
        setFixTradeAmount(defaultTradeAmount);
        setMinTradeAmount(defaultTradeAmount);
        setMaxTradeAmount(defaultTradeAmount);
        updateAmountSpec();
        updateTradeAmountLimits();
        updateUserSpecificTradeAmountLimit();
        updateUserSpecificTradeAmountLimitAsSliderValue();
        updateInputAmountLimits();
        updateFixAmountSliderValue();
        updateMinAmountSliderValue();
        updateMaxAmountSliderValue();
    }

    @Override
    protected void reset() {

    }


    /* --------------------------------------------------------------------- */
    // Observers
    /* --------------------------------------------------------------------- */

    @Override
    protected void addObservers() {
        pin(marketObservable().addObserver(value -> {
            updateTradeAmountLimits();
            updateUserSpecificTradeAmountLimit();
            updateDefaultTradeAmount();
            updateFixTradeAmount();
            updateMinTradeAmount();
            updateMaxTradeAmount();

            updateUserSpecificTradeAmountLimitAsSliderValue();
            updateInputAmountLimits();
        }));

        pin(directionObservable().addObserver(direction -> {
            updateUserSpecificTradeAmountLimit();
            updateUserSpecificTradeAmountLimitAsSliderValue();
        }));
        pin(selectedAccountByPaymentMethodObservable().addObserver(() -> {
        }));

        pin(priceQuoteObservable().addObserver(value -> {
            updateTradeAmountLimits();
            updateUserSpecificTradeAmountLimit();
            updateDefaultTradeAmount();
            updateFixTradeAmount();
            updateMinTradeAmount();
            updateMaxTradeAmount();
        }));
        pin(priceSpecObservable().addObserver(value -> {
        }));

        pin(useBaseCurrencyForAmountInputObservable().addObserver(value -> {
            updateInputAmountLimits();
        }));
        pin(useRangeAmountObservable().addObserver(value -> {
            updateAmountSpec();
        }));
        pin(defaultTradeAmountObservable().addObserver(value -> {
        }));
        pin(fixTradeAmountObservable().addObserver(value -> {
            if (!getUseRangeAmount()) {
                updateAmountSpec();
                updateFixAmountSliderValue();
            }
        }));
        pin(minTradeAmountObservable().addObserver(value -> {
            if (getUseRangeAmount()) {
                updateAmountSpec();
                updateMinAmountSliderValue();
            }
        }));
        pin(maxTradeAmountObservable().addObserver(value -> {
            if (getUseRangeAmount()) {
                updateAmountSpec();
                updateMaxAmountSliderValue();
            }
        }));
        pin(amountSpecObservable().addObserver(value -> {
        }));
        pin(tradeAmountLimitsObservable().addObserver(value -> {
            updateInputAmountLimits();
        }));
    }


    /* --------------------------------------------------------------------- */
    // Write methods
    /* --------------------------------------------------------------------- */

    public void setFixTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount fixTradeAmount = getFixTradeAmount();
        TradeAmount tradeAmount = toTradeAmountWithSliderValue(sliderValue, fixTradeAmount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount minTradeAmount = getMinTradeAmount();
        TradeAmount tradeAmount = toTradeAmountWithSliderValue(sliderValue, minTradeAmount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromSliderValue(double sliderValue) {
        TradeAmount maxTradeAmount = getMaxTradeAmount();
        TradeAmount tradeAmount = toTradeAmountWithSliderValue(sliderValue, maxTradeAmount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setMaxTradeAmount(tradeAmount);
    }

    public void setFixTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmountFromInputAmount(Monetary amount) {
        checkNotNull(amount, "amount must not be null");
        TradeAmount tradeAmount = toTradeAmount(amount);
        tradeAmount = clampTradeAmount(tradeAmount);
        setMaxTradeAmount(tradeAmount);
    }

    public Monetary toInputAmount(TradeAmount tradeAmount) {
        if (getUseBaseCurrencyForAmountInput()) {
            Monetary baseSideAmount = tradeAmount.getBaseSideAmount();
            return clampBaseSideAmount(baseSideAmount);
        } else {
            Monetary quoteSideAmount = tradeAmount.getQuoteSideAmount();
            return clampQuoteSideAmount(quoteSideAmount);
        }
    }

    public Monetary toPassiveAmount(TradeAmount tradeAmount) {
        if (getUseBaseCurrencyForAmountInput()) {
            Monetary quoteSideAmount = tradeAmount.getQuoteSideAmount();
            return clampQuoteSideAmount(quoteSideAmount);
        } else {
            Monetary baseSideAmount = tradeAmount.getBaseSideAmount();
            return clampBaseSideAmount(baseSideAmount);
        }
    }



    /* --------------------------------------------------------------------- */
    // Update methods
    /* --------------------------------------------------------------------- */

    // Depends on market, priceQuote
    private void updateDefaultTradeAmount() {
        Fiat minTradeAmountInUsd = TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD;
        Monetary defaultAmountInFiat = MarketBasedAmountConversion.usdToFiat(marketPriceService, getMarket(), minTradeAmountInUsd)
                .orElseThrow();
        TradeAmount defaultTradeAmount = toTradeAmount(defaultAmountInFiat);
        setDefaultTradeAmount(defaultTradeAmount);
    }

    private void updateFixTradeAmount() {
        Monetary inputAmount = toInputAmount(getFixTradeAmount());
        setFixTradeAmountFromInputAmount(inputAmount);
    }

    private void updateMinTradeAmount() {
        Monetary inputAmount = toInputAmount(getMinTradeAmount());
        setMinTradeAmountFromInputAmount(inputAmount);
    }

    private void updateMaxTradeAmount() {
        Monetary inputAmount = toInputAmount(getMaxTradeAmount());
        setMaxTradeAmountFromInputAmount(inputAmount);
    }

    private void updateAmountSpec() {
        //todo should we use base amount spec for non fiat as well?
        setAmountSpec(createBaseSideAmountSpec());
    }

    private void updateTradeAmountLimits() {
        Market market = getMarket();
        PriceQuote priceQuote = getPriceQuote();
        TradeAmount minTradeAmount = TradeAmountConversion.toTradeAmount(market,
                priceQuote,
                TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD);
        TradeAmount maxTradeAmount = TradeAmountConversion.toTradeAmount(market,
                priceQuote,
                getMaxTradeAmountInUsd());
      /*  TradeAmount minTradeAmount = MarketBasedAmountConversion.tradeAmountFromUsdAndMarket(marketPriceService,
                market,
                TradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD);*/
      /*  TradeAmount maxTradeAmount = MarketBasedAmountConversion.tradeAmountFromUsdAndMarket(marketPriceService,
                market,
                getMaxTradeAmountInUsd());*/

        TradeAmountRange tradeAmountLimit = new TradeAmountRange(minTradeAmount, maxTradeAmount);
        setTradeAmountLimits(tradeAmountLimit);
    }

    private void updateUserSpecificTradeAmountLimit() {
        if (getDirection().isBuy()) {
            //todo
            Fiat limit = Fiat.fromFaceValue(3000, "USD");
            Market market = getMarket();
            PriceQuote priceQuote = getPriceQuote();
            TradeAmount userSpecificTradeAmountLimit = TradeAmountConversion.toTradeAmount(market,
                    priceQuote,
                    limit);

           /* TradeAmount userSpecificTradeAmountLimit = MarketBasedAmountConversion.tradeAmountFromUsdAndMarket(marketPriceService,
                    getMarket(),
                    limit);*/
            setUserSpecificTradeAmountLimit(Optional.of(userSpecificTradeAmountLimit));
        } else {
            setUserSpecificTradeAmountLimit(Optional.empty());
        }
    }

    private void updateUserSpecificTradeAmountLimitAsSliderValue() {
        if (getDirection().isBuy() && getUserSpecificTradeAmountLimit().isPresent()) {
            TradeAmount userSpecificTradeAmountLimit = getUserSpecificTradeAmountLimit().get();
            double sliderValue = toSliderValue(userSpecificTradeAmountLimit);
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.of(sliderValue));
        } else {
            setUserSpecificTradeAmountLimitAsSliderValue(Optional.empty());
        }
    }

    private void updateInputAmountLimits() {
        TradeAmountRange tradeAmountLimits = getTradeAmountLimits();
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        MonetaryRange inputAmountLimits = toInputAmountLimits(tradeAmountLimits);
        setInputAmountLimits(inputAmountLimits);
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
    // Amount Utils
    /* --------------------------------------------------------------------- */

    private TradeAmount toTradeAmount(Monetary amount) {
        return TradeAmountConversion.toTradeAmount(getMarket(), getPriceQuote(), amount);
    }

    public BaseSideAmountSpec createBaseSideAmountSpec() {
        return AmountSpecFactory.createBaseSideAmountSpec(getUseRangeAmount(),
                getMinTradeAmount(),
                getMaxTradeAmount(),
                getFixTradeAmount());
    }

    private MonetaryRange toInputAmountLimits(TradeAmountRange tradeAmountLimits) {
        Monetary minInputAmount = toInputAmount(tradeAmountLimits.getMin());
        Monetary maxInputAmount = toInputAmount(tradeAmountLimits.getMax());
        return new MonetaryRange(minInputAmount, maxInputAmount);
    }

    private double toSliderValue(TradeAmount tradeAmount) {
        Monetary inputAmount = toInputAmount(tradeAmount);
        MonetaryRange inputAmountLimits = getInputAmountLimits();
        long min = inputAmountLimits.getMin().getValue();
        long max = inputAmountLimits.getMax().getValue();
        double diff = max - min;
        double sliderValue = (inputAmount.getValue() - min) / diff;
        return MathUtils.bounded(0, 1, sliderValue);
    }

    private TradeAmount toTradeAmountWithSliderValue(double sliderValue, TradeAmount tradeAmount) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");

        MonetaryRange inputAmountLimits = getInputAmountLimits();
        long min = inputAmountLimits.getMin().getValue();
        long max = inputAmountLimits.getMax().getValue();
        long diff = max - min;
        long sliderAmountValue = min + Math.round(sliderValue * diff);
        Monetary inputAmount = toInputAmount(tradeAmount);
        Monetary sliderAmount = Monetary.from(inputAmount, sliderAmountValue);
        return toTradeAmount(sliderAmount);
    }

    private static Fiat getMaxTradeAmountInUsd() {
        // todo based on payment method
        return TradeAmountLimits.MAX_USD_TRADE_AMOUNT;
    }


    /* --------------------------------------------------------------------- */
    // Apply limits
    /* --------------------------------------------------------------------- */

    private TradeAmount clampTradeAmount(TradeAmount tradeAmount) {
        TradeAmountRange tradeAmountLimits = getTradeAmountLimits();
        return clampTradeAmount(tradeAmountLimits, tradeAmount);

    }

    @VisibleForTesting
    static TradeAmount clampTradeAmount(TradeAmountRange tradeAmountLimits, TradeAmount tradeAmount) {
        TradeAmount min = tradeAmountLimits.getMin();
        TradeAmount max = tradeAmountLimits.getMax();
        return tradeAmount.clamp(min, max);

    }

    private Monetary clampBaseSideAmount(Monetary baseSideAmount) {
        TradeAmountRange tradeAmountLimits = getTradeAmountLimits();
        return clampBaseSideAmount(tradeAmountLimits, baseSideAmount);
    }

    @VisibleForTesting
    static Monetary clampBaseSideAmount(TradeAmountRange tradeAmountLimits, Monetary baseSideAmount) {
        Monetary min = tradeAmountLimits.getMin().getBaseSideAmount();
        Monetary max = tradeAmountLimits.getMax().getBaseSideAmount();
        return baseSideAmount.clamp(min, max);
    }

    private Monetary clampQuoteSideAmount(Monetary quoteSideAmount) {
        TradeAmountRange tradeAmountLimits = getTradeAmountLimits();
        return clampQuoteSideAmount(tradeAmountLimits, quoteSideAmount);
    }

    @VisibleForTesting
    static Monetary clampQuoteSideAmount(TradeAmountRange tradeAmountLimits, Monetary quoteSideAmount) {
        Monetary min = tradeAmountLimits.getMin().getQuoteSideAmount();
        Monetary max = tradeAmountLimits.getMax().getQuoteSideAmount();
        return quoteSideAmount.clamp(min, max);
    }


    /* --------------------------------------------------------------------- */
    // Setters
    /* --------------------------------------------------------------------- */

    public void setMarket(Market market) {
        checkNotNull(market, "Market must not be null");
        offerDraft.setMarket(market);
    }

    public void setDirection(Direction direction) {
        checkNotNull(direction, "Direction must not be null");
        offerDraft.setDirection(direction);
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
        offerDraft.setPriceQuote(priceQuote);
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        checkNotNull(priceSpec, "PriceSpec must not be null");
        offerDraft.setPriceSpec(priceSpec);
    }

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        offerDraft.setUseBaseCurrencyForAmountInput(value);
    }

    public void setUseRangeAmount(boolean useRangeAmount) {
        offerDraft.setUseRangeAmount(useRangeAmount);
        settingsService.setCookie(CookieKey.CREATE_MU_SIG_OFFER_IS_MIN_AMOUNT_ENABLED, useRangeAmount);
    }

    public void setDefaultTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setDefaultTradeAmount(tradeAmount);
    }

    public void setFixTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setFixTradeAmount(tradeAmount);
    }

    public void setMinTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setMinTradeAmount(tradeAmount);
    }

    public void setMaxTradeAmount(TradeAmount tradeAmount) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        offerDraft.setMaxTradeAmount(tradeAmount);
    }

    public void setAmountSpec(AmountSpec amountSpec) {
        checkNotNull(amountSpec, "AmountSpec must not be null");
        offerDraft.setAmountSpec(amountSpec);
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

}
