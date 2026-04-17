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

package bisq.desktop.main.content.mu_sig.offer.components.amount_selection;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.asset.Asset;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.components.MuSigPriceInput;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.OfferDraftWorkflow;
import bisq.presentation.formatters.AmountFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigAmountSelectionController implements Controller {

    final MuSigAmountSelectionModel model;
    @Getter
    private final MuSigAmountSelectionView view;
    private final MarketPriceService marketPriceService;
    private final MuSigBigAmountNumberBox maxOrFixedQuoteSideAmountInput, minQuoteSideAmountInput, invertedMinBaseSideAmountInput,
            invertedMaxOrFixedBaseSideAmountInput;
    private final MuSigSmallAmountNumberBox maxOrFixedBaseSideAmountDisplay, minBaseSideAmountDisplay, invertedMinQuoteSideAmountDisplay,
            invertedMaxOrFixedQuoteSideAmountDisplay;
    private final ChangeListener<Monetary> maxOrFixedQuoteSideAmountFromModelListener, minQuoteSideAmountFromModelListener,
            maxOrFixedBaseSideAmountFromModelListener, minBaseSideAmountFromModelListener;
    private final ChangeListener<PriceQuote> quoteListener;
    private final MuSigPriceInput priceInput;
    private final ChangeListener<Number> maxOrFixedSliderListener, minSliderListener;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<UIScheduler> schedulers = new HashSet<>();
    private final SettingsService settingsService;

    public MuSigAmountSelectionController(ServiceProvider serviceProvider, OfferDraftWorkflow<?> offerDraftWorkflow) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();

        // max or fixed amount
        maxOrFixedQuoteSideAmountInput = new MuSigBigAmountNumberBox(false, true);
        maxOrFixedBaseSideAmountDisplay = new MuSigSmallAmountNumberBox(true, true);
        // inverted to select amount using base
        invertedMaxOrFixedQuoteSideAmountDisplay = new MuSigSmallAmountNumberBox(false, true);
        invertedMaxOrFixedBaseSideAmountInput = new MuSigBigAmountNumberBox(true, true);

        // min amount (only applies when selecting a range)
        minQuoteSideAmountInput = new MuSigBigAmountNumberBox(false, false);
        minBaseSideAmountDisplay = new MuSigSmallAmountNumberBox(true, false);
        // inverted to select amount using base
        invertedMinQuoteSideAmountDisplay = new MuSigSmallAmountNumberBox(false, false);
        invertedMinBaseSideAmountInput = new MuSigBigAmountNumberBox(true, false);

        priceInput = new MuSigPriceInput(serviceProvider.getBondedRolesService().getMarketPriceService(), offerDraftWorkflow);

        model = new MuSigAmountSelectionModel(getWidthByNumCharsMap());
        view = new MuSigAmountSelectionView(model,
                this,
                maxOrFixedBaseSideAmountDisplay.getRoot(),
                maxOrFixedQuoteSideAmountInput.getRoot(),
                invertedMaxOrFixedQuoteSideAmountDisplay.getRoot(),
                invertedMaxOrFixedBaseSideAmountInput.getRoot(),
                minBaseSideAmountDisplay.getRoot(),
                minQuoteSideAmountInput.getRoot(),
                invertedMinQuoteSideAmountDisplay.getRoot(),
                invertedMinBaseSideAmountInput.getRoot());

        // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
        // apply the conversion before the other component has processed the market change event.
        // The order of the event notification is not deterministic.
        maxOrFixedQuoteSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMaxOrFixedBaseFromQuote);
        minQuoteSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMinBaseFromQuote);
        maxOrFixedBaseSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMaxOrFixedQuoteFromBase);
        minBaseSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMinQuoteFromBase);
        quoteListener = (observable, oldValue, newValue) -> {
            model.getRangeBaseSideAmount().set(null);
            model.getRangeQuoteSideAmount().set(null);

            applyInitialRangeValues();
            UIThread.runOnNextRenderFrame(this::applyQuote);
        };
        maxOrFixedSliderListener = (observable, oldValue, newValue) ->
                applySliderValue(newValue.doubleValue(), maxOrFixedQuoteSideAmountInput, invertedMaxOrFixedBaseSideAmountInput);
        minSliderListener = (observable, oldValue, newValue) ->
                applySliderValue(newValue.doubleValue(), minQuoteSideAmountInput, invertedMinBaseSideAmountInput);
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        if (model.getMarket().isBtcFiatMarket()) {
            model.getIsDefaultAmountInputBtc().set(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(true));
        } else {
            model.getIsDefaultAmountInputBtc().set(settingsService.getCookie().asBoolean(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC)
                    .orElse(true));
        }

        model.getRangeBaseSideAmount().set(null);
        model.getRangeQuoteSideAmount().set(null);

        applyInitialRangeValues();

        model.getMaxOrFixedQuoteSideAmount().addListener(maxOrFixedQuoteSideAmountFromModelListener);
        model.getMinQuoteSideAmount().addListener(minQuoteSideAmountFromModelListener);
        model.getMaxOrFixedBaseSideAmount().addListener(maxOrFixedBaseSideAmountFromModelListener);
        model.getMinBaseSideAmount().addListener(minBaseSideAmountFromModelListener);
        priceInput.priceQuoteProperty().addListener(quoteListener);

        maxOrFixedBaseSideAmountDisplay.setAmount(null);
        invertedMaxOrFixedBaseSideAmountInput.setAmount(null);
        if (model.getMaxOrFixedQuoteSideAmount().get() == null) {
            initializeQuoteSideAmount(maxOrFixedQuoteSideAmountInput, invertedMaxOrFixedQuoteSideAmountDisplay);
        } else {
            maxOrFixedQuoteSideAmountInput.setAmount(model.getMaxOrFixedQuoteSideAmount().get());
            invertedMaxOrFixedQuoteSideAmountDisplay.setAmount(model.getMaxOrFixedQuoteSideAmount().get());
        }
        setMaxOrFixedBaseFromQuote();

        minBaseSideAmountDisplay.setAmount(null);
        invertedMinBaseSideAmountInput.setAmount(null);
        if (model.getMinQuoteSideAmount().get() == null) {
            initializeQuoteSideAmount(minQuoteSideAmountInput, invertedMinQuoteSideAmountDisplay);
        } else {
            minQuoteSideAmountInput.setAmount(model.getMinQuoteSideAmount().get());
            invertedMinQuoteSideAmountDisplay.setAmount(model.getMinQuoteSideAmount().get());
        }
        setMinBaseFromQuote();

        subscriptions.add(EasyBind.subscribe(model.getMaxOrFixedQuoteSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on the high thumb (max)
            boolean isMaxOrFixedThumbFocused = model.getUseRangeAmount().get()
                    ? model.getRangeSliderHighThumbFocus().get()
                    : model.getMaxOrFixedAmountSliderFocus().get();
            if (amount != null && !isMaxOrFixedThumbFocused) {
                UIThread.run(() -> model.getMaxOrFixedAmountSliderValue().set(getSliderValue(amount.getValue())));
            }
        }));

        subscriptions.add(EasyBind.subscribe(model.getMinQuoteSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on the low thumb (min)
            boolean isMinThumbFocused = model.getUseRangeAmount().get()
                    ? model.getRangeSliderLowThumbFocus().get()
                    : model.getMinAmountSliderFocus().get();
            if (amount != null && !isMinThumbFocused) {
                UIThread.run(() -> model.getMinAmountSliderValue().set(getSliderValue(amount.getValue())));
            }
        }));

        subscriptions.add(EasyBind.subscribe(maxOrFixedBaseSideAmountDisplay.amountProperty(),
                amount -> updateMaxOrFixedBaseSideAmount(amount, maxOrFixedBaseSideAmountDisplay)));

        subscriptions.add(EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.amountProperty(),
                amount -> updateMaxOrFixedBaseSideAmount(amount, invertedMaxOrFixedBaseSideAmountInput)));

        subscriptions.add(EasyBind.subscribe(minBaseSideAmountDisplay.amountProperty(),
                amount -> updateMinBaseAmount(amount, minBaseSideAmountDisplay)));

        subscriptions.add(EasyBind.subscribe(invertedMinBaseSideAmountInput.amountProperty(),
                amount -> updateMinBaseAmount(amount, invertedMinBaseSideAmountInput)));

        subscriptions.add(EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.amountProperty(),
                amount -> updateMaxOrFixedQuoteSideAmount(amount, maxOrFixedQuoteSideAmountInput)));

        subscriptions.add(EasyBind.subscribe(invertedMaxOrFixedQuoteSideAmountDisplay.amountProperty(),
                amount -> updateMaxOrFixedQuoteSideAmount(amount, invertedMaxOrFixedQuoteSideAmountDisplay)));

        subscriptions.add(EasyBind.subscribe(minQuoteSideAmountInput.amountProperty(),
                amount -> updateMinQuoteSideAmount(amount, minQuoteSideAmountInput)));

        subscriptions.add(EasyBind.subscribe(invertedMinQuoteSideAmountDisplay.amountProperty(),
                amount -> updateMinQuoteSideAmount(amount, invertedMinQuoteSideAmountDisplay)));

        subscriptions.add(EasyBind.subscribe(priceInput.priceQuoteProperty(), quote -> applyInitialRangeValues()));

        subscriptions.add(EasyBind.subscribe(model.getQuoteSideTradeAmountLimits(), value -> applyInitialRangeValues()));

        subscriptions.add(subscribeToAmountValidity(maxOrFixedQuoteSideAmountInput, this::setMaxOrFixedQuoteFromBase));
        subscriptions.add(subscribeToAmountValidity(minQuoteSideAmountInput, this::setMinQuoteFromBase));
        subscriptions.add(subscribeToAmountValidity(invertedMinBaseSideAmountInput, this::setMinBaseFromQuote));
        subscriptions.add(subscribeToAmountValidity(invertedMaxOrFixedBaseSideAmountInput, this::setMaxOrFixedBaseFromQuote));

        subscriptions.add(EasyBind.subscribe(model.getUseRangeAmount(), useRangeAmount -> {
            model.getDescription().set(
                    Res.get(useRangeAmount
                                    ? "muSig.offer.create.amount.description.range"
                                    : "muSig.offer.create.amount.description.fixed"
                            , model.getMarket().getQuoteCurrencyCode()));
            updateShouldShowAmounts();
            maxOrFixedQuoteSideAmountInput.setTextInputMaxCharCount(useRangeAmount
                    ? MuSigAmountSelectionModel.RANGE_INPUT_TEXT_MAX_LENGTH
                    : MuSigAmountSelectionModel.FIXED_INPUT_TEXT_MAX_LENGTH);
            minQuoteSideAmountInput.setTextInputMaxCharCount(MuSigAmountSelectionModel.RANGE_INPUT_TEXT_MAX_LENGTH);
            applyTextInputPrefWidth();
            deselectAll();
            schedulers.add(UIScheduler.run(this::requestFocusForAmountInput).after(150));
        }));


        subscriptions.add(EasyBind.subscribe(model.getIsDefaultAmountInputBtc(), isDefaultAmountInputBtc -> {
            if (model.getMarket().isBtcFiatMarket()) {
                settingsService.setCookie(CookieKey.MU_SIG_FIAT_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC, isDefaultAmountInputBtc);
            } else {
                settingsService.setCookie(CookieKey.MU_SIG_OTHER_MARKET_IS_DEFAULT_AMOUNT_INPUT_BTC, isDefaultAmountInputBtc);
            }
            updateShowTradeAmountLimitInUsd();
            updateShouldShowAmounts();
            applyInitialRangeValues();
            schedulers.add(UIScheduler.run(this::requestFocusForAmountInput).after(150));
        }));

        model.getMaxOrFixedAmountSliderValue().addListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().addListener(minSliderListener);

        schedulers.add(UIScheduler.run(() -> {
            requestFocusForAmountInput();

            subscriptions.add(EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(shouldFocusAmountComponent())));
            subscriptions.add(EasyBind.subscribe(minQuoteSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(shouldFocusAmountComponent())));
            subscriptions.add(EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(shouldFocusAmountComponent())));
            subscriptions.add(EasyBind.subscribe(invertedMinBaseSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(shouldFocusAmountComponent())));

            subscriptions.add(EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.lengthProperty(), length -> applyNewStyles()));
            subscriptions.add(EasyBind.subscribe(minQuoteSideAmountInput.lengthProperty(), length -> applyNewStyles()));
            subscriptions.add(EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.lengthProperty(), length -> applyNewStyles()));
            subscriptions.add(EasyBind.subscribe(invertedMinBaseSideAmountInput.lengthProperty(), length -> applyNewStyles()));
        }).after(700));
    }

    @Override
    public void onDeactivate() {
        schedulers.forEach(UIScheduler::stop);
        schedulers.clear();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();

        model.getMaxOrFixedQuoteSideAmount().removeListener(maxOrFixedQuoteSideAmountFromModelListener);
        model.getMinQuoteSideAmount().removeListener(minQuoteSideAmountFromModelListener);
        model.getMaxOrFixedBaseSideAmount().removeListener(maxOrFixedBaseSideAmountFromModelListener);
        model.getMinBaseSideAmount().removeListener(minBaseSideAmountFromModelListener);
        priceInput.priceQuoteProperty().removeListener(quoteListener);
        model.getMaxOrFixedAmountSliderValue().removeListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().removeListener(minSliderListener);

        maxOrFixedQuoteSideAmountInput.isAmountValidProperty().set(true);
        minQuoteSideAmountInput.isAmountValidProperty().set(true);

        model.setLeftMarkerQuoteSideValue(null);
        model.setRightMarkerQuoteSideValue(null);
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setTradeAmountLimitsInUsd(MonetaryRange tradeAmountLimitsInUsd) {
        model.getTradeAmountLimitsInUsd().set(tradeAmountLimitsInUsd);

        model.getFormattedMinTradeAmountLimitInUsd().set(AmountFormatter.formatAmount(tradeAmountLimitsInUsd.getMin(), true));
        model.getFormattedMaxTradeAmountLimitInUsd().set(AmountFormatter.formatAmount(tradeAmountLimitsInUsd.getMax(), true));
    }

    public void setMaxOrFixedBaseSideAmount(Monetary value) {
        model.getMaxOrFixedBaseSideAmount().set(value);
    }

    public void setMinBaseSideAmount(Monetary value) {
        model.getMinBaseSideAmount().set(value);
    }

    public void setMaxOrFixedQuoteSideAmount(Monetary value) {
        model.getMaxOrFixedQuoteSideAmount().set(value);
    }

    public void setMinQuoteSideAmount(Monetary value) {
        model.getMinQuoteSideAmount().set(value);
    }

    public ReadOnlyObjectProperty<Monetary> getMaxOrFixedBaseSideAmount() {
        return model.getMaxOrFixedBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getMinBaseSideAmount() {
        return model.getMinBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getMaxOrFixedQuoteSideAmount() {
        return model.getMaxOrFixedQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getMinQuoteSideAmount() {
        return model.getMinQuoteSideAmount();
    }

    public ReadOnlyBooleanProperty getIsDefaultAmountInputBtc() {
        return model.getIsDefaultAmountInputBtc();
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        model.getSpendOrReceiveString().set(direction.isBuy() ? Res.get("offer.buying") : Res.get("offer.selling"));
    }

    public void setTooltip(String tooltip) {
        maxOrFixedBaseSideAmountDisplay.setTooltip(tooltip);
        minBaseSideAmountDisplay.setTooltip(tooltip);
        invertedMaxOrFixedQuoteSideAmountDisplay.setTooltip(tooltip);
        invertedMinQuoteSideAmountDisplay.setTooltip(tooltip);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.setMarket(market);

        maxOrFixedBaseSideAmountDisplay.setSelectedMarket(market);
        invertedMaxOrFixedBaseSideAmountInput.setSelectedMarket(market);
        maxOrFixedQuoteSideAmountInput.setSelectedMarket(market);
        invertedMaxOrFixedQuoteSideAmountDisplay.setSelectedMarket(market);
        minBaseSideAmountDisplay.setSelectedMarket(market);
        invertedMinBaseSideAmountInput.setSelectedMarket(market);
        minQuoteSideAmountInput.setSelectedMarket(market);
        invertedMinQuoteSideAmountDisplay.setSelectedMarket(market);

        // Reset all amounts to avoid currency mismatch when market changes
        model.getMaxOrFixedQuoteSideAmount().set(null);
        model.getMinQuoteSideAmount().set(null);
        model.getMaxOrFixedBaseSideAmount().set(null);
        model.getMinBaseSideAmount().set(null);

        updateShowTradeAmountLimitInUsd();
    }

    public void setDescription(String description) {
        model.getDescription().set(description);
    }

    public void setMaxAllowedLimitation(Monetary maxAllowedLimitation) {
        model.getMaxAllowedQuoteSideAmount().set(maxAllowedLimitation);
        // TODO: this should work for any coin, not just BTC
        Monetary maxAllowedLimitationInBtc = marketPriceService.findMarketPriceQuote(model.getMarket())
                .map(btcFiatPriceQuote -> btcFiatPriceQuote.toBaseSideMonetary(maxAllowedLimitation)).orElseThrow();
        model.getMaxAllowedBaseSideAmount().set(maxAllowedLimitationInBtc);
    }

    public void setQuoteSideTradeAmountLimits(MonetaryRange quoteSideTradeAmountLimits) {
        checkArgument(quoteSideTradeAmountLimits.getMin().getValue() <= quoteSideTradeAmountLimits.getMax().getValue(),
                "Min value must not be larger than max value");
        model.getQuoteSideTradeAmountLimits().set(quoteSideTradeAmountLimits);
        applyInitialRangeValues();
    }

    public void setLeftMarkerQuoteSideValue(Monetary quoteSideAmount) {
        model.setLeftMarkerQuoteSideValue(quoteSideAmount);
        applySliderTrackStyle();
    }

    public void setRightMarkerQuoteSideValue(Monetary quoteSideAmount) {
        model.setRightMarkerQuoteSideValue(quoteSideAmount);
        applySliderTrackStyle();
    }

    public void setQuote(PriceQuote priceQuote) {
        if (priceQuote != null) {
            priceInput.setQuote(priceQuote);
        }
    }

    public void setUseRangeAmount(boolean value) {
        model.getUseRangeAmount().set(value);
    }

    public ReadOnlyObjectProperty<PriceQuote> getQuote() {
        return priceInput.priceQuoteProperty();
    }

    public Monetary getRightMarkerQuoteSideValue() {
        return model.getRightMarkerQuoteSideValue();
    }

    public boolean isDefaultAmountInputBtc() {
        return model.getIsDefaultAmountInputBtc().get();
    }

    public void reset() {
        maxOrFixedBaseSideAmountDisplay.reset();
        invertedMaxOrFixedBaseSideAmountInput.reset();
        maxOrFixedQuoteSideAmountInput.reset();
        invertedMaxOrFixedQuoteSideAmountDisplay.reset();
        minBaseSideAmountDisplay.reset();
        invertedMinBaseSideAmountInput.reset();
        minQuoteSideAmountInput.reset();
        invertedMinQuoteSideAmountDisplay.reset();
        priceInput.reset();
        model.reset();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    double onGetMaxAllowedSliderValue() {
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return 0;
        }
        Monetary maxRangeQuoteSideAmount = rangeQuoteSideAmount.getMax();
        return getSliderValue(maxRangeQuoteSideAmount.getValue());
    }

    void onClickFlipCurrenciesButton() {
        boolean currentValue = model.getIsDefaultAmountInputBtc().get();
        model.getIsDefaultAmountInputBtc().set(!currentValue);
        applyNewStyles();
    }

    int onGetCalculatedTotalCharCount() {
        return isDefaultAmountInputBtc()
                ? getCount(invertedMinBaseSideAmountInput, invertedMaxOrFixedBaseSideAmountInput)
                : getCount(minQuoteSideAmountInput, maxOrFixedQuoteSideAmountInput);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private boolean shouldFocusAmountComponent() {
        return maxOrFixedQuoteSideAmountInput.focusedProperty().get()
                || minQuoteSideAmountInput.focusedProperty().get()
                || invertedMinBaseSideAmountInput.focusedProperty().get()
                || invertedMaxOrFixedBaseSideAmountInput.focusedProperty().get();
    }

    private void requestFocusForAmountInput() {
        if (isDefaultAmountInputBtc()) {
            invertedMaxOrFixedBaseSideAmountInput.requestFocus();
        } else {
            maxOrFixedQuoteSideAmountInput.requestFocus();
        }
    }

    private int getCount(MuSigBigAmountNumberBox minSideAmountInput,
                         MuSigBigAmountNumberBox maxOrFixedSideAmountInput) {
        int count = model.getUseRangeAmount().get()
                ? minSideAmountInput.getTextInputLength() + maxOrFixedSideAmountInput.getTextInputLength() + 1 // 1 for the dash
                : maxOrFixedSideAmountInput.getTextInputLength();

        if (!minSideAmountInput.getTextInput().contains(".") || !maxOrFixedSideAmountInput.getTextInput().contains(".")) {
            // If using an integer we need to count one more char since a dot occupies much less space.
            ++count;
        }
        return count;
    }

    private double getSliderValue(long amountValue) {
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return 0;
        }
        long min = rangeQuoteSideAmount.getMin().getValue();
        long max = model.getMaxAllowedQuoteSideAmount().get() != null
                ? model.getMaxAllowedQuoteSideAmount().get().getValue()
                : rangeQuoteSideAmount.getMax().getValue();
        long base = max - min;
        if (base == 0) {
            return 0;
        }
        return (double) (amountValue - min) / base;
    }

    private void initializeQuoteSideAmount(MuSigBigAmountNumberBox quoteSideAmountInput,
                                           MuSigSmallAmountNumberBox smallNumberDisplayBox) {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return;
        }
        if (priceQuote != null) {
            Monetary minRangeQuoteSideAmount = rangeQuoteSideAmount.getMin();
            Monetary maxRangeQuoteSideAmount = rangeQuoteSideAmount.getMax();
            long midValue = minRangeQuoteSideAmount.getValue() + (maxRangeQuoteSideAmount.getValue() - minRangeQuoteSideAmount.getValue()) / 2;
            Monetary exactAmount = model.getMarket().isCrypto()
                    ? Coin.asBtcFromValue(midValue)
                    : Fiat.fromValue(midValue, priceQuote.getQuoteSideMonetary().getCode()).round(0);
            quoteSideAmountInput.setAmount(exactAmount);
            smallNumberDisplayBox.setAmount(exactAmount);
        } else {
            log.warn("price.quoteProperty().get() is null. We use a fiat value of 100 as default value.");
            Monetary defaultQuoteSideAmount = model.getMarket().isCrypto()
                    ? Coin.asBtcFromFaceValue(100)
                    : Fiat.fromFaceValue(100, model.getMarket().getQuoteCurrencyCode());
            quoteSideAmountInput.setAmount(defaultQuoteSideAmount);
            smallNumberDisplayBox.setAmount(defaultQuoteSideAmount);
        }
    }

    private Subscription subscribeToAmountValidity(MuSigBigAmountNumberBox amountInput, Runnable autocorrect) {
        return EasyBind.subscribe(amountInput.isAmountValidProperty(), isAmountValid -> {
            if (!amountInput.isAmountValidProperty().get()) {
                autocorrect.run();
                amountInput.isAmountValidProperty().set(true);
            }
        });
    }

    private void applyInitialRangeValues() {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        MonetaryRange quoteSideTradeAmountLimits = model.getQuoteSideTradeAmountLimits().get();
        if (priceQuote == null || quoteSideTradeAmountLimits == null) {
            return;
        }

        if (model.getMarket().isCrypto()) {
            Monetary minRangeQuoteMonetary = quoteSideTradeAmountLimits.getMin();
            Monetary maxRangeQuoteMonetary = quoteSideTradeAmountLimits.getMax();
            Monetary minRangeBaseMonetary = priceQuote.toBaseSideMonetary(minRangeQuoteMonetary);
            Monetary maxRangeBaseMonetary = priceQuote.toBaseSideMonetary(maxRangeQuoteMonetary);

            model.getRangeBaseSideAmount().set(new MonetaryRange(minRangeBaseMonetary, maxRangeBaseMonetary));
            model.getRangeQuoteSideAmount().set(new MonetaryRange(minRangeQuoteMonetary, maxRangeQuoteMonetary));

            if (isDefaultAmountInputBtc()) {
                // Input now is base side
                model.getFormattedMinTradeAmountLimit().set(AmountFormatter.formatBaseAmount(minRangeBaseMonetary));
                model.getTradeAmountLimitCode().set(minRangeBaseMonetary.getCode());
                model.getFormattedMaxTradeAmountLimit().set(AmountFormatter.formatBaseAmount(maxRangeBaseMonetary));
            } else {
                // Input now is quote side
                model.getFormattedMinTradeAmountLimit().set(AmountFormatter.formatQuoteAmount(minRangeQuoteMonetary));
                model.getTradeAmountLimitCode().set(minRangeQuoteMonetary.getCode());
                model.getFormattedMaxTradeAmountLimit().set(AmountFormatter.formatQuoteAmount(maxRangeQuoteMonetary));
            }
        } else {
            Monetary minRangeAmount = quoteSideTradeAmountLimits.getMin();
            Monetary maxRangeAmount = quoteSideTradeAmountLimits.getMax();
            String code = minRangeAmount.getCode();
            boolean isFiat = Asset.isFiat(code);

            Monetary minRangeBaseSideAmount = !isFiat ? minRangeAmount : priceQuote.toBaseSideMonetary(minRangeAmount);
            Monetary maxRangeBaseSideAmount = !isFiat ? maxRangeAmount : priceQuote.toBaseSideMonetary(maxRangeAmount);
            model.getRangeBaseSideAmount().set(new MonetaryRange(minRangeBaseSideAmount, maxRangeBaseSideAmount));

            Monetary minRangeQuoteSideAmount = isFiat ? minRangeAmount : priceQuote.toQuoteSideMonetary(minRangeAmount).round(0);
            Monetary maxRangeQuoteSideAmount = isFiat ? maxRangeAmount : priceQuote.toQuoteSideMonetary(maxRangeAmount).round(0);
            model.getRangeQuoteSideAmount().set(new MonetaryRange(minRangeQuoteSideAmount, maxRangeQuoteSideAmount));

            if (isDefaultAmountInputBtc()) {
                // Input now is base side
                model.getFormattedMinTradeAmountLimit().set(AmountFormatter.formatBaseAmount(minRangeBaseSideAmount));
                model.getTradeAmountLimitCode().set(minRangeBaseSideAmount.getCode());
                model.getFormattedMaxTradeAmountLimit().set(AmountFormatter.formatBaseAmount(maxRangeBaseSideAmount));
            } else {
                // Input now is quote side
                model.getFormattedMinTradeAmountLimit().set(AmountFormatter.formatQuoteAmount(minRangeQuoteSideAmount));
                model.getTradeAmountLimitCode().set(minRangeQuoteSideAmount.getCode());
                Monetary maxRangeMonetaryLimitationAsFiat = maxRangeQuoteSideAmount;
                if (model.getMaxAllowedQuoteSideAmount().get() != null) {
                    Monetary maxQuoteAllowedLimitation = model.getMaxAllowedQuoteSideAmount().get();
                    maxRangeMonetaryLimitationAsFiat = isFiat ? maxQuoteAllowedLimitation : priceQuote.toQuoteSideMonetary(maxQuoteAllowedLimitation).round(0);
                }
                model.getFormattedMaxTradeAmountLimit().set(AmountFormatter.formatQuoteAmount(maxRangeMonetaryLimitationAsFiat));
            }
        }

        applySliderTrackStyle();
    }

    private void applySliderTrackStyle() {
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return;
        }
        Monetary min = rangeQuoteSideAmount.getMin();
        Monetary max = model.getMaxAllowedQuoteSideAmount().get() != null
                ? model.getMaxAllowedQuoteSideAmount().get()
                : rangeQuoteSideAmount.getMax();
        if (min == null || max == null) {
            return;
        }
        long minRangeMonetaryValue = min.getValue();
        long maxRangeMonetaryValue = max.getValue();
        double range = maxRangeMonetaryValue - minRangeMonetaryValue;

        // If left value is not set we use minRange
        // If left value is set but right value not set we don't show any marker
        Monetary markerQuoteSideValue = model.getLeftMarkerQuoteSideValue();
        long leftMarkerQuoteSideValue = Optional.ofNullable(markerQuoteSideValue).orElse(min).getValue();
        double left = leftMarkerQuoteSideValue - minRangeMonetaryValue;
        double leftPercentage = range != 0 ? 100 * left / range : 0;

        long rightMarkerQuoteSideValue = Optional.ofNullable(model.getRightMarkerQuoteSideValue()).orElse(min).getValue();
        double right = rightMarkerQuoteSideValue - minRangeMonetaryValue;
        double rightPercentage = range != 0 ? 100 * right / range : 0;

        if (model.getDirection().isSell()) {
            // Adjust values to match slider knob better
            if (rightPercentage < 2) {
                rightPercentage += 1.2;
            } else if (rightPercentage < 8) {
                rightPercentage += 1;
            } else if (rightPercentage < 15) {
                rightPercentage += 0.9;
            } else if (rightPercentage < 24) {
                rightPercentage += 0.7;
            } else if (rightPercentage < 60) {
                rightPercentage += 0.5;
            }
        }

        // E.g.: -bisq-dark-grey-50 0%, -bisq-dark-grey-50 30.0%, -bisq2-green 30.0%, -bisq2-green 60.0%, -bisq-dark-grey-50 60.0%, -bisq-dark-grey-50 100%)
        String segments = String.format(
                MuSigAmountSelectionModel.SLIDER_TRACK_DEFAULT_COLOR + " 0%%, " +
                        MuSigAmountSelectionModel.SLIDER_TRACK_DEFAULT_COLOR + " %1$.1f%%, " +

                        MuSigAmountSelectionModel.SLIDER_TRACK_MARKER_COLOR + " %1$.1f%%, " +
                        MuSigAmountSelectionModel.SLIDER_TRACK_MARKER_COLOR + " %2$.1f%%, " +

                        MuSigAmountSelectionModel.SLIDER_TRACK_DEFAULT_COLOR + " %2$.1f%%, " +
                        MuSigAmountSelectionModel.SLIDER_TRACK_DEFAULT_COLOR + " 100%%)",
                leftPercentage, rightPercentage);
        String style = "-track-color: linear-gradient(to right, " + segments + ";";
        model.getSliderTrackStyle().set(style);
    }

    private void applySliderValue(double sliderValue,
                                  MuSigBigAmountNumberBox quoteAmountInput,
                                  MuSigBigAmountNumberBox baseAmountInput) {
        if (isDefaultAmountInputBtc()) {
            MonetaryRange rangeBaseSideAmount = model.getRangeBaseSideAmount().get();
            if (rangeBaseSideAmount == null) {
                return;
            }
            long min = rangeBaseSideAmount.getMin().getValue();
            long max;
            Monetary maxAllowedBaseSideAmount = model.getMaxAllowedBaseSideAmount().get();
            if (maxAllowedBaseSideAmount != null) {
                max = maxAllowedBaseSideAmount.getValue();
            } else {
                max = rangeBaseSideAmount.getMax().getValue();
            }
            long value = Math.round(sliderValue * (max - min)) + min;
            String baseCurrencyCode = model.getMarket().getBaseCurrencyCode();
            Monetary baseAmount = Monetary.from(value, baseCurrencyCode);
            baseAmountInput.setAmount(baseAmount);
        } else {
            MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
            if (rangeQuoteSideAmount == null) {
                return;
            }
            long min = rangeQuoteSideAmount.getMin().getValue();
            long max;
            Monetary maxAllowedQuoteSideAmount = model.getMaxAllowedQuoteSideAmount().get();
            if (maxAllowedQuoteSideAmount != null) {
                max = maxAllowedQuoteSideAmount.getValue();
            } else {
                max = rangeQuoteSideAmount.getMax().getValue();
            }
            long value = Math.round(sliderValue * (max - min)) + min;
            String quoteCurrencyCode = model.getMarket().getQuoteCurrencyCode();
            Monetary quoteAmount = Monetary.from(value, quoteCurrencyCode);
            quoteAmountInput.setAmount(quoteAmount);

        }
    }

    private void setMaxOrFixedQuoteFromBase() {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        if (priceQuote == null) {
            return;
        }
        Monetary baseSideAmount = model.getMaxOrFixedBaseSideAmount().get();
        if (baseSideAmount == null) {
            return;
        }
        maxOrFixedQuoteSideAmountInput.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount));
        invertedMaxOrFixedQuoteSideAmountDisplay.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount));
    }

    private void setMinQuoteFromBase() {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        if (priceQuote == null) {
            return;
        }
        Monetary baseSideAmount = model.getMinBaseSideAmount().get();
        if (baseSideAmount == null) {
            return;
        }
        minQuoteSideAmountInput.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount));
        invertedMinQuoteSideAmountDisplay.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount));
    }

    private void setMaxOrFixedBaseFromQuote() {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        if (priceQuote == null) {
            return;
        }
        Monetary quoteSideAmount = model.getMaxOrFixedQuoteSideAmount().get();
        if (quoteSideAmount == null) {
            return;
        }
        maxOrFixedBaseSideAmountDisplay.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
        invertedMaxOrFixedBaseSideAmountInput.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
    }

    private void setMinBaseFromQuote() {
        PriceQuote priceQuote = priceInput.priceQuoteProperty().get();
        if (priceQuote == null) {
            return;
        }
        Monetary quoteSideAmount = model.getMinQuoteSideAmount().get();
        if (quoteSideAmount == null) {
            return;
        }
        minBaseSideAmountDisplay.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
        invertedMinBaseSideAmountInput.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
    }

    private void applyQuote() {
        if (model.getIsDefaultAmountInputBtc().get()) {
            setMaxOrFixedQuoteFromBase();
            setMinQuoteFromBase();
        } else {
            setMaxOrFixedBaseFromQuote();
            setMinBaseFromQuote();
        }
    }

    private void updateShouldShowAmounts() {
        if (model.getIsDefaultAmountInputBtc() != null) {
            boolean areCurrenciesInverted = model.getIsDefaultAmountInputBtc().get();
            model.getShouldShowMaxOrFixedAmounts().set(!areCurrenciesInverted);
            model.getShouldShowInvertedMaxOrFixedAmounts().set(areCurrenciesInverted);
            if (model.getUseRangeAmount() != null) {
                boolean useRangeAmount = model.getUseRangeAmount().get();
                model.getShouldShowMinAmounts().set(useRangeAmount && !areCurrenciesInverted);
                model.getShouldShowInvertedMinAmounts().set(useRangeAmount && areCurrenciesInverted);
            }
        }
    }

    private void updateMaxOrFixedBaseSideAmount(Monetary amount, MuSigAmountNumberBox amountNumberBox) {
        MonetaryRange rangeBaseSideAmount = model.getRangeBaseSideAmount().get();
        if (rangeBaseSideAmount == null) {
            return;
        }
        Monetary min = rangeBaseSideAmount.getMin();
        Monetary max = rangeBaseSideAmount.getMax();
        if (amount != null && amount.getValue() > max.getValue()) {
            model.getMaxOrFixedBaseSideAmount().set(max);
            setMaxOrFixedQuoteFromBase();
            amountNumberBox.setAmount(max);
        } else if (amount != null && amount.getValue() < min.getValue()) {
            model.getMaxOrFixedBaseSideAmount().set(min);
            setMaxOrFixedQuoteFromBase();
            amountNumberBox.setAmount(min);
        } else {
            model.getMaxOrFixedBaseSideAmount().set(amount);
        }
    }

    private void updateMinBaseAmount(Monetary amount, MuSigAmountNumberBox amountNumberBox) {
        MonetaryRange rangeBaseSideAmount = model.getRangeBaseSideAmount().get();
        if (rangeBaseSideAmount == null) {
            return;
        }
        Monetary min = rangeBaseSideAmount.getMin();
        Monetary max = rangeBaseSideAmount.getMax();
        if (amount != null && amount.getValue() > max.getValue()) {
            model.getMinBaseSideAmount().set(max);
            setMinQuoteFromBase();
            amountNumberBox.setAmount(max);
        } else if (amount != null && amount.getValue() < min.getValue()) {
            model.getMinBaseSideAmount().set(min);
            setMinQuoteFromBase();
            amountNumberBox.setAmount(min);
        } else {
            model.getMinBaseSideAmount().set(amount);
        }
    }

    private void updateMaxOrFixedQuoteSideAmount(Monetary amount, MuSigAmountNumberBox amountNumberBox) {
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return;
        }
        Monetary min = rangeQuoteSideAmount.getMin();
        Monetary max = rangeQuoteSideAmount.getMax();
        if (max != null && amount != null && amount.getValue() > max.getValue()) {
            model.getMaxOrFixedQuoteSideAmount().set(max);
            setMaxOrFixedBaseFromQuote();
            amountNumberBox.setAmount(max);
        } else if (min != null && amount != null && amount.getValue() < min.getValue()) {
            model.getMaxOrFixedQuoteSideAmount().set(min);
            setMaxOrFixedBaseFromQuote();
            amountNumberBox.setAmount(min);
        } else {
            model.getMaxOrFixedQuoteSideAmount().set(amount);
        }
    }

    private void updateMinQuoteSideAmount(Monetary amount, MuSigAmountNumberBox amountNumberBox) {
        MonetaryRange rangeQuoteSideAmount = model.getRangeQuoteSideAmount().get();
        if (rangeQuoteSideAmount == null) {
            return;
        }
        Monetary min = rangeQuoteSideAmount.getMin();
        Monetary max = rangeQuoteSideAmount.getMax();
        if (max != null && amount != null && amount.getValue() > max.getValue()) {
            model.getMinQuoteSideAmount().set(max);
            setMinBaseFromQuote();
            amountNumberBox.setAmount(max);
        } else if (min != null && amount != null && amount.getValue() < min.getValue()) {
            model.getMinQuoteSideAmount().set(min);
            setMinBaseFromQuote();
            amountNumberBox.setAmount(min);
        } else {
            model.getMinQuoteSideAmount().set(amount);
        }
    }

    private void updateShowTradeAmountLimitInUsd() {
        if (model.getMarket() != null) {
            model.getShowTradeAmountLimitInUsd().set(model.getIsDefaultAmountInputBtc().get() || !model.getMarket().isUsdMarket());
        }
    }

    private void applyTextInputPrefWidth() {
        int charCount = onGetCalculatedTotalCharCount();
        if (isDefaultAmountInputBtc()) {
            applyPrefWidth(charCount, invertedMinBaseSideAmountInput);
            applyPrefWidth(charCount, invertedMaxOrFixedBaseSideAmountInput);
        } else {
            applyPrefWidth(charCount, minQuoteSideAmountInput);
            applyPrefWidth(charCount, maxOrFixedQuoteSideAmountInput);
        }
    }

    private void applyPrefWidth(int charCount, MuSigBigAmountNumberBox amountInputBox) {
        int length = getCalculatedTextInputLength(amountInputBox);
        amountInputBox.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));
    }

    private int getCalculatedTextInputLength(MuSigBigAmountNumberBox quoteAmountInputBox) {
        // If using an integer we need to count one more char since a dot occupies much less space.
        return !quoteAmountInputBox.getTextInput().contains(".")
                ? quoteAmountInputBox.getTextInputLength() + 1
                : quoteAmountInputBox.getTextInputLength();
    }

    private void deselectAll() {
        minQuoteSideAmountInput.deselect();
        maxOrFixedQuoteSideAmountInput.deselect();
        invertedMinBaseSideAmountInput.deselect();
        invertedMaxOrFixedBaseSideAmountInput.deselect();
    }

    private void applyNewStyles() {
        model.getShouldApplyNewInputTextFontStyle().set(true);
        applyTextInputPrefWidth();
    }

    private int getFontCharWidth(int charCount) {
        if (charCount < 10) {
            return 31;
        } else {
            return model.getWidthByNumCharsMap().getOrDefault(charCount, 15); // Default to 15 if not found
        }
    }

    private static Map<Integer, Integer> getWidthByNumCharsMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(10, 28);
        map.put(11, 25);
        map.put(12, 23);
        map.put(13, 21);
        map.put(14, 19);
        map.put(15, 18);
        map.put(16, 17);
        map.put(17, 16);
        return map;
    }
}
