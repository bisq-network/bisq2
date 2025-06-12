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

package bisq.desktop.main.content.bisq_easy.components.amount_selection;

import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AmountSelectionController implements Controller {
    private static final String SLIDER_TRACK_DEFAULT_COLOR = "-bisq-dark-grey-50";
    private static final String SLIDER_TRACK_MARKER_COLOR = "-bisq2-green";
    private static final int RANGE_INPUT_TEXT_MAX_LENGTH = 11;
    private static final int FIXED_INPUT_TEXT_MAX_LENGTH = 18;
    private static final Map<Integer, Integer> CHAR_WIDTH_MAP = new HashMap<>();
    static {
        CHAR_WIDTH_MAP.put(10, 28);
        CHAR_WIDTH_MAP.put(11, 25);
        CHAR_WIDTH_MAP.put(12, 23);
        CHAR_WIDTH_MAP.put(13, 21);
        CHAR_WIDTH_MAP.put(14, 19);
        CHAR_WIDTH_MAP.put(15, 18);
        CHAR_WIDTH_MAP.put(16, 17);
        CHAR_WIDTH_MAP.put(17, 16);
    }

    final AmountSelectionModel model;
    @Getter
    private final AmountSelectionView view;
    private final BigNumberAmountInputBox maxOrFixedQuoteSideAmountInput, minQuoteSideAmountInput, invertedMinBaseSideAmountInput,
            invertedMaxOrFixedBaseSideAmountInput;
    private final SmallNumberDisplayBox maxOrFixedBaseSideAmountDisplay, minBaseSideAmountDisplay, invertedMinQuoteSideAmountDisplay,
            invertedMaxOrFixedQuoteSideAmountDisplay;
    private final ChangeListener<Monetary> maxOrFixedQuoteSideAmountFromModelListener, minQuoteSideAmountFromModelListener,
            maxOrFixedBaseSideAmountFromModelListener, minBaseSideAmountFromModelListener;
    private final ChangeListener<PriceQuote> quoteListener;
    private final PriceInput price;
    private final ChangeListener<Number> maxOrFixedSliderListener, minSliderListener;
    private Subscription maxOrFixedQuoteAmountFromModelPin, maxOrFixedBaseAmountFromCompPin, invertedMaxOrFixedBaseAmountFromCompPin,
            maxOrFixedQuoteAmountFromCompPin, invertedMaxOrFixedQuoteAmountFromCompPin, invertedMinBaseSideAmountValidPin,
            maxOrFixedQuoteSideAmountValidPin, minQuoteAmountFromModelPin, minBaseAmountFromCompPin, invertedMinBaseAmountFromCompPin,
            minQuoteAmountFromCompPin, invertedMinQuoteAmountFromCompPin, invertedMaxOrFixedBaseSideAmountValidPin,
            minQuoteSideAmountValidPin, priceFromCompPin, minRangeCustomValuePin, maxRangeCustomValuePin, isRangeAmountEnabledPin,
            areBaseAndQuoteCurrenciesInvertedPin, maxOrFixedQuoteSideAmountInputFocusPin, minQuoteSideAmountInputFocusPin,
            maxOrFixedQuoteSideAmountInputLengthPin, minQuoteSideAmountInputLengthPin, invertedMaxOrFixedBaseSideAmountInputFocusPin,
            invertedMaxOrFixedBaseSideAmountInputLengthPin, invertedMinBaseSideAmountInputFocusPin, invertedMinBaseSideAmountInputLengthPin;

    public AmountSelectionController(ServiceProvider serviceProvider) {
        // max or fixed amount
        maxOrFixedQuoteSideAmountInput = new BigNumberAmountInputBox(false, true);
        maxOrFixedBaseSideAmountDisplay = new SmallNumberDisplayBox(true, true);
        // inverted to select amount using base
        invertedMaxOrFixedQuoteSideAmountDisplay = new SmallNumberDisplayBox(false, true);
        invertedMaxOrFixedBaseSideAmountInput = new BigNumberAmountInputBox(true, true);

        // min amount (only applies when selecting a range)
        minQuoteSideAmountInput = new BigNumberAmountInputBox(false, false);
        minBaseSideAmountDisplay = new SmallNumberDisplayBox(true, false);
        // inverted to select amount using base
        invertedMinQuoteSideAmountDisplay = new SmallNumberDisplayBox(false, false);
        invertedMinBaseSideAmountInput = new BigNumberAmountInputBox(true, false);

        price = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());

        model = new AmountSelectionModel();
        view = new AmountSelectionView(model,
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
            model.getMinRangeBaseSideValue().set(null);
            model.getMaxRangeBaseSideValue().set(null);
            model.getMinRangeQuoteSideValue().set(null);
            model.getMaxRangeQuoteSideValue().set(null);
            applyInitialRangeValues();
            UIThread.runOnNextRenderFrame(this::applyQuote);
        };
        maxOrFixedSliderListener = (observable, oldValue, newValue) ->
                applySliderValue(newValue.doubleValue(), maxOrFixedQuoteSideAmountInput, invertedMaxOrFixedBaseSideAmountInput);
        minSliderListener = (observable, oldValue, newValue) ->
                applySliderValue(newValue.doubleValue(), minQuoteSideAmountInput, invertedMinBaseSideAmountInput);
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

    public ReadOnlyBooleanProperty getAreBaseAndQuoteCurrenciesInverted() {
        return model.getAreBaseAndQuoteCurrenciesInverted();
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        model.getSpendOrReceiveString().set(direction == Direction.BUY ? Res.get("offer.buying") : Res.get("offer.selling"));
    }

    public void setAllowInvertingBaseAndQuoteCurrencies(boolean allowInvertingBaseAndQuoteCurrencies) {
        model.getAllowInvertingBaseAndQuoteCurrencies().set(allowInvertingBaseAndQuoteCurrencies);
        model.getBaseAmountSelectionHBoxWidth().set(allowInvertingBaseAndQuoteCurrencies ? model.getAmountBoxWidth() - 10 : model.getAmountBoxWidth());
    }

    public void setBaseAsInputCurrency(boolean setBaseAsInputCurrency) {
        model.getAreBaseAndQuoteCurrenciesInverted().set(setBaseAsInputCurrency);
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
        price.setMarket(market);
    }

    public void setDescription(String description) {
        model.getDescription().set(description);
    }

    public void setMaxAllowedLimitation(Monetary maxAllowedLimitation) {
        model.getMaxQuoteAllowedLimitation().set(maxAllowedLimitation);
    }

    public void setMinMaxRange(Monetary minRangeValue, Monetary maxRangeValue) {
        boolean minRangeValueIsFiat = TradeCurrency.isFiat(minRangeValue.getCode());
        boolean maxRangeValueIsFiat = TradeCurrency.isFiat(maxRangeValue.getCode());
        checkArgument(minRangeValueIsFiat && maxRangeValueIsFiat,
                "The provided minRangeValue and maxRangeValue must be fiat currencies as useQuoteCurrencyForMinMaxRange is set to true.");

        model.getMinRangeMonetary().set(minRangeValue);
        model.getMaxRangeMonetary().set(maxRangeValue);
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
            price.setQuote(priceQuote);
        }
    }

    public void setIsRangeAmountEnabled(boolean isRangeAmountEnabled) {
        model.getIsRangeAmountEnabled().set(isRangeAmountEnabled);
    }

    public ReadOnlyObjectProperty<PriceQuote> getQuote() {
        return price.getQuote();
    }

    public Monetary getRightMarkerQuoteSideValue() {
        return model.getRightMarkerQuoteSideValue();
    }

    public boolean isUsingInvertedBaseAndQuoteCurrencies() {
        return model.getAllowInvertingBaseAndQuoteCurrencies().get()
                && model.getAreBaseAndQuoteCurrenciesInverted().get();
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
        price.reset();
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getMinRangeBaseSideValue().set(null);
        model.getMaxRangeBaseSideValue().set(null);
        model.getMinRangeQuoteSideValue().set(null);
        model.getMaxRangeQuoteSideValue().set(null);
        applyInitialRangeValues();

        model.getMaxOrFixedQuoteSideAmount().addListener(maxOrFixedQuoteSideAmountFromModelListener);
        model.getMinQuoteSideAmount().addListener(minQuoteSideAmountFromModelListener);
        model.getMaxOrFixedBaseSideAmount().addListener(maxOrFixedBaseSideAmountFromModelListener);
        model.getMinBaseSideAmount().addListener(minBaseSideAmountFromModelListener);
        price.getQuote().addListener(quoteListener);

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

        maxOrFixedQuoteAmountFromModelPin = EasyBind.subscribe(model.getMaxOrFixedQuoteSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null && !model.getMaxOrFixedAmountSliderFocus().get()) {
                model.getMaxOrFixedAmountSliderValue().set(getSliderValue(amount.getValue()));
            }
        });

        minQuoteAmountFromModelPin = EasyBind.subscribe(model.getMinQuoteSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null && !model.getMinAmountSliderFocus().get()) {
                model.getMinAmountSliderValue().set(getSliderValue(amount.getValue()));
            }
        });

        maxOrFixedBaseAmountFromCompPin = EasyBind.subscribe(maxOrFixedBaseSideAmountDisplay.amountProperty(),
                amount -> updateMaxOrFixedBaseSideAmount(amount, maxOrFixedBaseSideAmountDisplay));

        invertedMaxOrFixedBaseAmountFromCompPin = EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.amountProperty(),
                amount -> updateMaxOrFixedBaseSideAmount(amount, invertedMaxOrFixedBaseSideAmountInput));

        minBaseAmountFromCompPin = EasyBind.subscribe(minBaseSideAmountDisplay.amountProperty(),
                amount -> updateMinBaseAmount(amount, minBaseSideAmountDisplay));

        invertedMinBaseAmountFromCompPin = EasyBind.subscribe(invertedMinBaseSideAmountInput.amountProperty(),
                amount -> updateMinBaseAmount(amount, invertedMinBaseSideAmountInput));

        maxOrFixedQuoteAmountFromCompPin = EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.amountProperty(),
                amount -> updateMaxOrFixedQuoteSideAmount(amount, maxOrFixedQuoteSideAmountInput));

        invertedMaxOrFixedQuoteAmountFromCompPin = EasyBind.subscribe(invertedMaxOrFixedQuoteSideAmountDisplay.amountProperty(),
                amount -> updateMaxOrFixedQuoteSideAmount(amount, invertedMaxOrFixedQuoteSideAmountDisplay));

        minQuoteAmountFromCompPin = EasyBind.subscribe(minQuoteSideAmountInput.amountProperty(),
                amount -> updateMinQuoteSideAmount(amount, minQuoteSideAmountInput));

        invertedMinQuoteAmountFromCompPin = EasyBind.subscribe(invertedMinQuoteSideAmountDisplay.amountProperty(),
                amount -> updateMinQuoteSideAmount(amount, invertedMinQuoteSideAmountDisplay));

        priceFromCompPin = EasyBind.subscribe(price.getQuote(), quote -> applyInitialRangeValues());

        minRangeCustomValuePin = EasyBind.subscribe(model.getMinRangeMonetary(), value -> applyInitialRangeValues());
        maxRangeCustomValuePin = EasyBind.subscribe(model.getMaxRangeMonetary(), value -> applyInitialRangeValues());

        maxOrFixedQuoteSideAmountValidPin = subscribeToAmountValidity(maxOrFixedQuoteSideAmountInput, this::setMaxOrFixedQuoteFromBase);
        minQuoteSideAmountValidPin = subscribeToAmountValidity(minQuoteSideAmountInput, this::setMinQuoteFromBase);
        invertedMinBaseSideAmountValidPin = subscribeToAmountValidity(invertedMinBaseSideAmountInput, this::setMinBaseFromQuote);
        invertedMaxOrFixedBaseSideAmountValidPin = subscribeToAmountValidity(invertedMaxOrFixedBaseSideAmountInput, this::setMaxOrFixedBaseFromQuote);

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            model.getDescription().set(
                    Res.get(isRangeAmountEnabled
                                    ? "bisqEasy.tradeWizard.amount.description.range"
                                    : "bisqEasy.tradeWizard.amount.description.fixed"
                            , model.getMarket().getQuoteCurrencyCode()));
            updateShouldShowAmounts();
            maxOrFixedQuoteSideAmountInput.setTextInputMaxCharCount(isRangeAmountEnabled ? RANGE_INPUT_TEXT_MAX_LENGTH : FIXED_INPUT_TEXT_MAX_LENGTH);
            minQuoteSideAmountInput.setTextInputMaxCharCount(RANGE_INPUT_TEXT_MAX_LENGTH);
            applyTextInputPrefWidth();
            deselectAll();
            UIScheduler.run(maxOrFixedQuoteSideAmountInput::requestFocus).after(100);
        });

        areBaseAndQuoteCurrenciesInvertedPin = EasyBind.subscribe(model.getAreBaseAndQuoteCurrenciesInverted(),
                areBaseAndQuoteCurrenciesInverted -> updateShouldShowAmounts());

        model.getMaxOrFixedAmountSliderValue().addListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().addListener(minSliderListener);

        UIScheduler.run(() -> {
            if (isUsingInvertedBaseAndQuoteCurrencies()) {
                invertedMaxOrFixedBaseSideAmountInput.requestFocus();
            } else {
                maxOrFixedQuoteSideAmountInput.requestFocus();
            }

            maxOrFixedQuoteSideAmountInputFocusPin = EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(minQuoteSideAmountInput.focusedProperty().get() || focus));
            minQuoteSideAmountInputFocusPin = EasyBind.subscribe(minQuoteSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(maxOrFixedQuoteSideAmountInput.focusedProperty().get() || focus));
            invertedMaxOrFixedBaseSideAmountInputFocusPin = EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(invertedMinBaseSideAmountInput.focusedProperty().get() || focus));
            invertedMinBaseSideAmountInputFocusPin = EasyBind.subscribe(invertedMinBaseSideAmountInput.focusedProperty(),
                    focus -> model.getShouldFocusInputTextField().set(invertedMaxOrFixedBaseSideAmountInput.focusedProperty().get() || focus));

            maxOrFixedQuoteSideAmountInputLengthPin = EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.lengthProperty(), length -> applyNewStyles());
            minQuoteSideAmountInputLengthPin = EasyBind.subscribe(minQuoteSideAmountInput.lengthProperty(), length -> applyNewStyles());
            invertedMaxOrFixedBaseSideAmountInputLengthPin = EasyBind.subscribe(invertedMaxOrFixedBaseSideAmountInput.lengthProperty(), length -> applyNewStyles());
            invertedMinBaseSideAmountInputLengthPin = EasyBind.subscribe(invertedMinBaseSideAmountInput.lengthProperty(), length -> applyNewStyles());
        }).after(700);
    }

    @Override
    public void onDeactivate() {
        model.getMaxOrFixedQuoteSideAmount().removeListener(maxOrFixedQuoteSideAmountFromModelListener);
        model.getMinQuoteSideAmount().removeListener(minQuoteSideAmountFromModelListener);
        model.getMaxOrFixedBaseSideAmount().removeListener(maxOrFixedBaseSideAmountFromModelListener);
        model.getMinBaseSideAmount().removeListener(minBaseSideAmountFromModelListener);
        price.getQuote().removeListener(quoteListener);
        model.getMaxOrFixedAmountSliderValue().removeListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().removeListener(minSliderListener);

        maxOrFixedQuoteAmountFromModelPin.unsubscribe();
        minQuoteAmountFromModelPin.unsubscribe();
        maxOrFixedBaseAmountFromCompPin.unsubscribe();
        invertedMaxOrFixedBaseAmountFromCompPin.unsubscribe();
        minBaseAmountFromCompPin.unsubscribe();
        invertedMinBaseAmountFromCompPin.unsubscribe();
        maxOrFixedQuoteAmountFromCompPin.unsubscribe();
        invertedMaxOrFixedQuoteAmountFromCompPin.unsubscribe();
        minQuoteAmountFromCompPin.unsubscribe();
        invertedMinQuoteAmountFromCompPin.unsubscribe();
        priceFromCompPin.unsubscribe();
        minRangeCustomValuePin.unsubscribe();
        maxRangeCustomValuePin.unsubscribe();
        maxOrFixedQuoteSideAmountValidPin.unsubscribe();
        minQuoteSideAmountValidPin.unsubscribe();
        invertedMinBaseSideAmountValidPin.unsubscribe();
        invertedMaxOrFixedBaseSideAmountValidPin.unsubscribe();
        isRangeAmountEnabledPin.unsubscribe();
        areBaseAndQuoteCurrenciesInvertedPin.unsubscribe();

        maxOrFixedQuoteSideAmountInput.isAmountValidProperty().set(true);
        minQuoteSideAmountInput.isAmountValidProperty().set(true);

        model.setLeftMarkerQuoteSideValue(null);
        model.setRightMarkerQuoteSideValue(null);

        if (maxOrFixedQuoteSideAmountInputFocusPin != null) {
            maxOrFixedQuoteSideAmountInputFocusPin.unsubscribe();
        }
        if (minQuoteSideAmountInputFocusPin != null) {
            minQuoteSideAmountInputFocusPin.unsubscribe();
        }
        if (invertedMaxOrFixedBaseSideAmountInputFocusPin != null) {
            invertedMaxOrFixedBaseSideAmountInputFocusPin.unsubscribe();
        }
        if (invertedMinBaseSideAmountInputFocusPin != null) {
            invertedMinBaseSideAmountInputFocusPin.unsubscribe();
        }
        if (maxOrFixedQuoteSideAmountInputLengthPin != null) {
            maxOrFixedQuoteSideAmountInputLengthPin.unsubscribe();
        }
        if (minQuoteSideAmountInputLengthPin != null) {
            minQuoteSideAmountInputLengthPin.unsubscribe();
        }
        if (invertedMaxOrFixedBaseSideAmountInputLengthPin != null) {
            invertedMaxOrFixedBaseSideAmountInputLengthPin.unsubscribe();
        }
        if (invertedMinBaseSideAmountInputLengthPin != null) {
            invertedMinBaseSideAmountInputLengthPin.unsubscribe();
        }
    }

    double onGetMaxAllowedSliderValue() {
        return getSliderValue(model.getMaxRangeQuoteSideValue().get().getValue());
    }

    void onClickFlipCurrenciesButton() {
        if (model.getAllowInvertingBaseAndQuoteCurrencies() == null || !model.getAllowInvertingBaseAndQuoteCurrencies().get()) {
            return;
        }

        boolean currentValue = model.getAreBaseAndQuoteCurrenciesInverted().get();
        model.getAreBaseAndQuoteCurrenciesInverted().set(!currentValue);
        applyNewStyles();
    }

    int onGetCalculatedTotalCharCount() {
        return isUsingInvertedBaseAndQuoteCurrencies()
                ? getCount(invertedMinBaseSideAmountInput, invertedMaxOrFixedBaseSideAmountInput)
                : getCount(minQuoteSideAmountInput, maxOrFixedQuoteSideAmountInput);
    }

    private int getCount(BigNumberAmountInputBox minSideAmountInput, BigNumberAmountInputBox maxOrFixedSideAmountInput) {
        int count = model.getIsRangeAmountEnabled().get()
                ? minSideAmountInput.getTextInputLength() + maxOrFixedSideAmountInput.getTextInputLength() + 1 // 1 for the dash
                : maxOrFixedSideAmountInput.getTextInputLength();

        if (!minSideAmountInput.getTextInput().contains(".") || !maxOrFixedSideAmountInput.getTextInput().contains(".")) {
            // If using an integer we need to count one more char since a dot occupies much less space.
            ++count;
        }
        return count;
    }

    private double getSliderValue(long amountValue) {
        long min = model.getMinRangeQuoteSideValue().get().getValue();
        long max = model.getMaxQuoteAllowedLimitation().get() != null
                ? model.getMaxQuoteAllowedLimitation().get().getValue()
                : model.getMaxRangeQuoteSideValue().get().getValue();
        return (double) (amountValue - min) / (max - min);
    }

    private void initializeQuoteSideAmount(BigNumberAmountInputBox quoteSideAmountInput, SmallNumberDisplayBox smallNumberDisplayBox) {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote != null) {
            Monetary minRangeQuoteSideValue = model.getMinRangeQuoteSideValue().get();
            Monetary maxRangeQuoteSideValue = model.getMaxRangeQuoteSideValue().get();
            long midValue = minRangeQuoteSideValue.getValue() + (maxRangeQuoteSideValue.getValue() - minRangeQuoteSideValue.getValue()) / 2;
            Monetary exactAmount = Fiat.fromValue(midValue, priceQuote.getQuoteSideMonetary().getCode());
            quoteSideAmountInput.setAmount(exactAmount.round(0));
            smallNumberDisplayBox.setAmount(exactAmount.round(0));
        } else {
            log.warn("price.quoteProperty().get() is null. We use a fiat value of 100 as default value.");
            Fiat defaultQuoteSideAmount = Fiat.fromFaceValue(100, model.getMarket().getQuoteCurrencyCode());
            quoteSideAmountInput.setAmount(defaultQuoteSideAmount);
            smallNumberDisplayBox.setAmount(defaultQuoteSideAmount);
        }
    }

    private Subscription subscribeToAmountValidity(BigNumberAmountInputBox amountInput, Runnable autocorrect) {
        return EasyBind.subscribe(amountInput.isAmountValidProperty(), isAmountValid -> {
            if (!amountInput.isAmountValidProperty().get()) {
                autocorrect.run();
                amountInput.isAmountValidProperty().set(true);
            }
        });
    }

    private void applyInitialRangeValues() {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote == null) {
            return;
        }

        Monetary minRangeMonetary = model.getMinRangeMonetary().get();
        Monetary maxRangeMonetary = model.getMaxRangeMonetary().get();
        boolean isMinRangeMonetaryFiat = TradeCurrency.isFiat(minRangeMonetary.getCode());
        boolean isMaxRangeMonetaryFiat = TradeCurrency.isFiat(maxRangeMonetary.getCode());

        Monetary minRangeMonetaryAsCoin = !isMinRangeMonetaryFiat ? minRangeMonetary : priceQuote.toBaseSideMonetary(minRangeMonetary);
        model.getMinRangeBaseSideValue().set(minRangeMonetaryAsCoin);

        Monetary maxRangeMonetaryAsCoin = !isMaxRangeMonetaryFiat ? maxRangeMonetary : priceQuote.toBaseSideMonetary(maxRangeMonetary);
        model.getMaxRangeBaseSideValue().set(maxRangeMonetaryAsCoin);

        Monetary minRangeMonetaryAsFiat = isMinRangeMonetaryFiat ? minRangeMonetary : priceQuote.toQuoteSideMonetary(minRangeMonetary).round(0);
        model.getMinRangeQuoteSideValue().set(minRangeMonetaryAsFiat);

        Monetary maxRangeMonetaryAsFiat = isMaxRangeMonetaryFiat ? maxRangeMonetary : priceQuote.toQuoteSideMonetary(maxRangeMonetary).round(0);
        model.getMaxRangeQuoteSideValue().set(maxRangeMonetaryAsFiat);

        if (isUsingInvertedBaseAndQuoteCurrencies()) {
            model.getMinRangeValueAsString().set(AmountFormatter.formatBaseAmount(minRangeMonetaryAsCoin));
            model.getMinRangeCodeAsString().set(minRangeMonetaryAsCoin.getCode());
            model.getMaxRangeCodeAsString().set(maxRangeMonetaryAsCoin.getCode());
            model.getMaxRangeValueLimitationAsString().set(AmountFormatter.formatBaseAmount(maxRangeMonetaryAsCoin));
        } else {
            model.getMinRangeValueAsString().set(AmountFormatter.formatQuoteAmount(minRangeMonetaryAsFiat));
            model.getMinRangeCodeAsString().set(minRangeMonetaryAsFiat.getCode());
            model.getMaxRangeCodeAsString().set(maxRangeMonetaryAsFiat.getCode());
            Monetary maxRangeMonetaryLimitationAsFiat = maxRangeMonetaryAsFiat;
            if (model.getMaxQuoteAllowedLimitation().get() != null) {
                Monetary maxQuoteAllowedLimitation = model.getMaxQuoteAllowedLimitation().get();
                maxRangeMonetaryLimitationAsFiat = isMaxRangeMonetaryFiat ? maxQuoteAllowedLimitation : priceQuote.toQuoteSideMonetary(maxQuoteAllowedLimitation).round(0);
            }
            model.getMaxRangeValueLimitationAsString().set(AmountFormatter.formatQuoteAmount(maxRangeMonetaryLimitationAsFiat));
        }

        applySliderTrackStyle();
    }

    private void applySliderTrackStyle() {
        Monetary minRangeMonetary = model.getMinRangeQuoteSideValue().get();
        Monetary maxRangeMonetary = model.getMaxQuoteAllowedLimitation() != null
                ? model.getMaxQuoteAllowedLimitation().get()
                : model.getMaxRangeQuoteSideValue().get();
        if (minRangeMonetary == null || maxRangeMonetary == null) {
            return;
        }
        long minRangeMonetaryValue = minRangeMonetary.getValue();
        long maxRangeMonetaryValue = maxRangeMonetary.getValue();
        double range = maxRangeMonetaryValue - minRangeMonetaryValue;

        // If left value is not set we use minRange
        // If left value is set but right value not set we don't show any marker
        Monetary markerQuoteSideValue = model.getLeftMarkerQuoteSideValue();
        long leftMarkerQuoteSideValue = Optional.ofNullable(markerQuoteSideValue).orElse(minRangeMonetary).getValue();
        double left = leftMarkerQuoteSideValue - minRangeMonetaryValue;
        double leftPercentage = range != 0 ? 100 * left / range : 0;

        long rightMarkerQuoteSideValue = Optional.ofNullable(model.getRightMarkerQuoteSideValue()).orElse(minRangeMonetary).getValue();
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
                SLIDER_TRACK_DEFAULT_COLOR + " 0%%, " +
                        SLIDER_TRACK_DEFAULT_COLOR + " %1$.1f%%, " +

                        SLIDER_TRACK_MARKER_COLOR + " %1$.1f%%, " +
                        SLIDER_TRACK_MARKER_COLOR + " %2$.1f%%, " +

                        SLIDER_TRACK_DEFAULT_COLOR + " %2$.1f%%, " +
                        SLIDER_TRACK_DEFAULT_COLOR + " 100%%)",
                leftPercentage, rightPercentage);
        String style = "-track-color: linear-gradient(to right, " + segments + ";";
        model.getSliderTrackStyle().set(style);
    }

    private void applySliderValue(double sliderValue, BigNumberAmountInputBox quoteAmountInput, BigNumberAmountInputBox invertedBaseAmountInput) {
        if (isUsingInvertedBaseAndQuoteCurrencies()) {
            if (model.getMinRangeBaseSideValue().get() != null && model.getMaxRangeBaseSideValue().get() != null) {
                long min = model.getMinRangeBaseSideValue().get().getValue();
                long max = model.getMaxRangeBaseSideValue().get().getValue();
                long value = Math.round(sliderValue * (max - min)) + min;

                String baseCurrencyCode = model.getMarket().getBaseCurrencyCode();
                Monetary exactBaseAmount = Monetary.from(value, baseCurrencyCode);
                invertedBaseAmountInput.setAmount(exactBaseAmount);
            }
        } else {
            if (model.getMinRangeQuoteSideValue().get() != null) {
                long min = model.getMinRangeQuoteSideValue().get().getValue();
                long max = model.getMaxQuoteAllowedLimitation().get() != null
                        ? model.getMaxQuoteAllowedLimitation().get().getValue()
                        : model.getMaxRangeQuoteSideValue().get().getValue();
                long value = Math.round(sliderValue * (max - min)) + min;

                String quoteCurrencyCode = model.getMarket().getQuoteCurrencyCode();
                Monetary exactQuoteAmount = Monetary.from(value, quoteCurrencyCode);
                long roundedValueForLowPrecision = exactQuoteAmount.getRoundedValueForPrecision(0);
                Monetary roundedQuoteAmount = Monetary.from(roundedValueForLowPrecision, quoteCurrencyCode);
                quoteAmountInput.setAmount(roundedQuoteAmount);
            }
        }
    }

    private void setMaxOrFixedQuoteFromBase() {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote == null) {
            return;
        }
        Monetary baseSideAmount = model.getMaxOrFixedBaseSideAmount().get();
        if (baseSideAmount == null) {
            return;
        }
        maxOrFixedQuoteSideAmountInput.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount).round(0));
        invertedMaxOrFixedQuoteSideAmountDisplay.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount).round(0));
    }

    private void setMinQuoteFromBase() {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote == null) {
            return;
        }
        Monetary baseSideAmount = model.getMinBaseSideAmount().get();
        if (baseSideAmount == null) {
            return;
        }
        minQuoteSideAmountInput.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount).round(0));
        invertedMinQuoteSideAmountDisplay.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount).round(0));
    }

    private void setMaxOrFixedBaseFromQuote() {
        PriceQuote priceQuote = price.getQuote().get();
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
        PriceQuote priceQuote = price.getQuote().get();
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
        if (model.getAreBaseAndQuoteCurrenciesInverted().get()) {
            setMaxOrFixedQuoteFromBase();
            setMinQuoteFromBase();
        } else {
            setMaxOrFixedBaseFromQuote();
            setMinBaseFromQuote();
        }
    }

    private void updateShouldShowAmounts() {
        if (model.getAreBaseAndQuoteCurrenciesInverted() != null) {
            boolean areCurrenciesInverted = model.getAreBaseAndQuoteCurrenciesInverted().get();
            model.getShouldShowMaxOrFixedAmounts().set(!areCurrenciesInverted);
            model.getShouldShowInvertedMaxOrFixedAmounts().set(areCurrenciesInverted);
            if (model.getIsRangeAmountEnabled() != null) {
                boolean isRangeAmountEnabled = model.getIsRangeAmountEnabled().get();
                model.getShouldShowMinAmounts().set(isRangeAmountEnabled && !areCurrenciesInverted);
                model.getShouldShowInvertedMinAmounts().set(isRangeAmountEnabled && areCurrenciesInverted);
            }
        }
        applyInitialRangeValues();
    }

    private void updateMaxOrFixedBaseSideAmount(Monetary amount, AmountNumberBox amountNumberBox) {
        Monetary minRangeValue = model.getMinRangeBaseSideValue().get();
        Monetary maxRangeValue = model.getMaxRangeBaseSideValue().get();
        if (amount != null && amount.getValue() > maxRangeValue.getValue()) {
            model.getMaxOrFixedBaseSideAmount().set(maxRangeValue);
            setMaxOrFixedQuoteFromBase();
            amountNumberBox.setAmount(maxRangeValue);
        } else if (amount != null && amount.getValue() < minRangeValue.getValue()) {
            model.getMaxOrFixedBaseSideAmount().set(minRangeValue);
            setMaxOrFixedQuoteFromBase();
            amountNumberBox.setAmount(minRangeValue);
        } else {
            model.getMaxOrFixedBaseSideAmount().set(amount);
        }
    }

    private void updateMinBaseAmount(Monetary amount, AmountNumberBox amountNumberBox) {
        Monetary minRangeValue = model.getMinRangeBaseSideValue().get();
        Monetary maxRangeValue = model.getMaxRangeBaseSideValue().get();
        if (amount != null && amount.getValue() > maxRangeValue.getValue()) {
            model.getMinBaseSideAmount().set(maxRangeValue);
            setMinQuoteFromBase();
            amountNumberBox.setAmount(maxRangeValue);
        } else if (amount != null && amount.getValue() < minRangeValue.getValue()) {
            model.getMinBaseSideAmount().set(minRangeValue);
            setMinQuoteFromBase();
            amountNumberBox.setAmount(minRangeValue);
        } else {
            model.getMinBaseSideAmount().set(amount);
        }
    }

    private void updateMaxOrFixedQuoteSideAmount(Monetary amount, AmountNumberBox amountNumberBox) {
        Monetary minRangeValue = model.getMinRangeQuoteSideValue().get();
        Monetary maxRangeValue = model.getMaxRangeQuoteSideValue().get();
        if (maxRangeValue != null && amount != null && amount.getValue() > maxRangeValue.getValue()) {
            model.getMaxOrFixedQuoteSideAmount().set(maxRangeValue);
            setMaxOrFixedBaseFromQuote();
            amountNumberBox.setAmount(maxRangeValue);
        } else if (minRangeValue != null && amount != null && amount.getValue() < minRangeValue.getValue()) {
            model.getMaxOrFixedQuoteSideAmount().set(minRangeValue);
            setMaxOrFixedBaseFromQuote();
            amountNumberBox.setAmount(minRangeValue);
        } else {
            model.getMaxOrFixedQuoteSideAmount().set(amount);
        }
    }

    private void updateMinQuoteSideAmount(Monetary amount, AmountNumberBox amountNumberBox) {
        Monetary minRangeValue = model.getMinRangeQuoteSideValue().get();
        Monetary maxRangeValue = model.getMaxRangeQuoteSideValue().get();
        if (maxRangeValue != null && amount != null && amount.getValue() > maxRangeValue.getValue()) {
            model.getMinQuoteSideAmount().set(maxRangeValue);
            setMinBaseFromQuote();
            amountNumberBox.setAmount(maxRangeValue);
        } else if (minRangeValue != null && amount != null && amount.getValue() < minRangeValue.getValue()) {
            model.getMinQuoteSideAmount().set(minRangeValue);
            setMinBaseFromQuote();
            amountNumberBox.setAmount(minRangeValue);
        } else {
            model.getMinQuoteSideAmount().set(amount);
        }
    }

    private void applyTextInputPrefWidth() {
        int charCount = onGetCalculatedTotalCharCount();
        if (isUsingInvertedBaseAndQuoteCurrencies()) {
            applyPrefWidth(charCount, invertedMinBaseSideAmountInput);
            applyPrefWidth(charCount, invertedMaxOrFixedBaseSideAmountInput);
        } else {
            applyPrefWidth(charCount, minQuoteSideAmountInput);
            applyPrefWidth(charCount, maxOrFixedQuoteSideAmountInput);
        }
    }

    private void applyPrefWidth(int charCount, BigNumberAmountInputBox amountInputBox) {
        int length = getCalculatedTextInputLength(amountInputBox);
        amountInputBox.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));
    }

    private int getCalculatedTextInputLength(BigNumberAmountInputBox quoteAmountInputBox) {
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

    private static int getFontCharWidth(int charCount) {
        if (charCount < 10) {
            return 31;
        } else {
            return CHAR_WIDTH_MAP.getOrDefault(charCount, 15); // Default to 15 if not found
        }
    }
}
