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

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AmountSelectionController implements Controller {
    private static final String SLIDER_TRACK_DEFAULT_COLOR = "-bisq-dark-grey-50";
    private static final String SLIDER_TRACK_MARKER_COLOR = "-bisq2-green";

    final AmountSelectionModel model;
    @Getter
    private final AmountSelectionView view;
    private final QuoteAmountInputBox maxOrFixedQuoteSideAmountInput, minQuoteSideAmountInput;
    private final BaseAmountBox maxOrFixedBaseSideAmountInput, minBaseSideAmountInput;
    private final ChangeListener<Monetary> maxOrFixedQuoteSideAmountFromModelListener, minQuoteSideAmountFromModelListener;
    private final ChangeListener<PriceQuote> quoteListener;
    private final PriceInput price;
    private final ChangeListener<Number> maxOrFixedSliderListener, minSliderListener;
    private Subscription maxOrFixedBaseAmountFromModelPin, maxOrFixedBaseAmountFromCompPin, maxOrFixedQuoteAmountFromCompPin,
            maxOrFixedQuoteSideAmountValidPin, minBaseAmountFromModelPin, minBaseAmountFromCompPin, minQuoteAmountFromCompPin,
            minQuoteSideAmountValidPin, priceFromCompPin, minRangeCustomValuePin, maxRangeCustomValuePin, isRangeAmountEnabledPin;

    public AmountSelectionController(ServiceProvider serviceProvider,
                                     boolean useQuoteCurrencyForMinMaxRange) {
        // max or fixed amount
        maxOrFixedQuoteSideAmountInput = new QuoteAmountInputBox(false, true);
        maxOrFixedBaseSideAmountInput = new BaseAmountBox(true);
        maxOrFixedBaseSideAmountInput.setUseLowPrecision(false);

        // min amount (only applies when selecting a range)
        minQuoteSideAmountInput = new QuoteAmountInputBox(false, false);
        minBaseSideAmountInput = new BaseAmountBox(false);
        minBaseSideAmountInput.setUseLowPrecision(false);

        price = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());

        model = new AmountSelectionModel(useQuoteCurrencyForMinMaxRange);
        view = new AmountSelectionView(model, this,
                maxOrFixedBaseSideAmountInput,
                maxOrFixedQuoteSideAmountInput,
                minBaseSideAmountInput,
                minQuoteSideAmountInput);

        // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
        // apply the conversion before the other component has processed the market change event.
        // The order of the event notification is not deterministic.
        maxOrFixedQuoteSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMaxOrFixedBaseFromQuote);
        minQuoteSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setMinBaseFromQuote);
        quoteListener = (observable, oldValue, newValue) -> {
            model.getMinRangeBaseSideValue().set(null);
            model.getMaxRangeBaseSideValue().set(null);
            model.getMinRangeQuoteSideValue().set(null);
            model.getMaxRangeQuoteSideValue().set(null);
            applyInitialRangeValues();
            UIThread.runOnNextRenderFrame(this::applyQuote);
        };
        maxOrFixedSliderListener = (observable, oldValue, newValue) ->
            applySliderValue(newValue.doubleValue(), maxOrFixedQuoteSideAmountInput, maxOrFixedBaseSideAmountInput);
        minSliderListener = (observable, oldValue, newValue) ->
            applySliderValue(newValue.doubleValue(), minQuoteSideAmountInput, minBaseSideAmountInput);
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

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        model.getSpendOrReceiveString().set(direction == Direction.BUY ? Res.get("offer.buying") : Res.get("offer.selling"));
    }

    public void setTooltip(String tooltip) {
        maxOrFixedBaseSideAmountInput.setTooltip(tooltip);
        minBaseSideAmountInput.setTooltip(tooltip);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.setMarket(market);
        maxOrFixedBaseSideAmountInput.setSelectedMarket(market);
        maxOrFixedQuoteSideAmountInput.setSelectedMarket(market);
        minBaseSideAmountInput.setSelectedMarket(market);
        minQuoteSideAmountInput.setSelectedMarket(market);
        price.setMarket(market);
    }

    public void setDescription(String description) {
        model.getDescription().set(description);
    }

    public void setMinMaxRange(Monetary minRangeValue, Monetary maxRangeValue) {
        boolean minRangeValueIsFiat = FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(minRangeValue.getCode());
        boolean maxRangeValueIsFiat = FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(maxRangeValue.getCode());
        if (model.isUseQuoteCurrencyForMinMaxRange()) {
            checkArgument(minRangeValueIsFiat && maxRangeValueIsFiat,
                    "The provided minRangeValue and maxRangeValue must be fiat currencies as useQuoteCurrencyForMinMaxRange is set to true.");
        } else {
            checkArgument(!minRangeValueIsFiat && !maxRangeValueIsFiat,
                    "The provided minRangeValue and maxRangeValue must not be fiat currencies as useQuoteCurrencyForMinMaxRange is set to false.");
        }

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

    public void applyReputationBasedQuoteSideAmount() {
        maxOrFixedQuoteSideAmountInput.setAmount(model.getRightMarkerQuoteSideValue());
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

    public void reset() {
        maxOrFixedBaseSideAmountInput.reset();
        maxOrFixedQuoteSideAmountInput.reset();
        minBaseSideAmountInput.reset();
        minQuoteSideAmountInput.reset();
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
        price.getQuote().addListener(quoteListener);

        maxOrFixedBaseSideAmountInput.setAmount(null);
        if (model.getMaxOrFixedQuoteSideAmount().get() == null) {
            initializeQuoteSideAmount(maxOrFixedQuoteSideAmountInput);
        } else {
            maxOrFixedQuoteSideAmountInput.setAmount(model.getMaxOrFixedQuoteSideAmount().get());
        }
        setMaxOrFixedBaseFromQuote();

        minBaseSideAmountInput.setAmount(null);
        if (model.getMinQuoteSideAmount().get() == null) {
            initializeQuoteSideAmount(minQuoteSideAmountInput);
        } else {
            minQuoteSideAmountInput.setAmount(model.getMinQuoteSideAmount().get());
        }
        setMinBaseFromQuote();

        maxOrFixedBaseAmountFromModelPin = EasyBind.subscribe(model.getMaxOrFixedBaseSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null) {
                if (!model.getMaxOrFixedAmountSliderFocus().get()) {
                    long min = model.getMinRangeBaseSideValue().get().getValue();
                    long max = model.getMaxRangeBaseSideValue().get().getValue();
                    double sliderValue = (amount.getValue() - min) / ((double) max - min);
                    model.getMaxOrFixedAmountSliderValue().set(sliderValue);
                }
            }
        });

        minBaseAmountFromModelPin = EasyBind.subscribe(model.getMinBaseSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null) {
                if (!model.getMinAmountSliderFocus().get()) {
                    long min = model.getMinRangeBaseSideValue().get().getValue();
                    long max = model.getMaxRangeBaseSideValue().get().getValue();
                    double sliderValue = (amount.getValue() - min) / ((double) max - min);
                    model.getMinAmountSliderValue().set(sliderValue);
                }
            }
        });

        maxOrFixedBaseAmountFromCompPin = EasyBind.subscribe(maxOrFixedBaseSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeBaseSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeBaseSideValue().get();
                    if (amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getMaxOrFixedBaseSideAmount().set(maxRangeValue);
                        setMaxOrFixedQuoteFromBase();
                        maxOrFixedBaseSideAmountInput.setAmount(maxRangeValue);
                    } else if (amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getMaxOrFixedBaseSideAmount().set(minRangeValue);
                        setMaxOrFixedQuoteFromBase();
                        maxOrFixedBaseSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getMaxOrFixedBaseSideAmount().set(amount);
                    }
                });

        minBaseAmountFromCompPin = EasyBind.subscribe(minBaseSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeBaseSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeBaseSideValue().get();
                    if (amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getMinBaseSideAmount().set(maxRangeValue);
                        setMinQuoteFromBase();
                        minBaseSideAmountInput.setAmount(maxRangeValue);
                    } else if (amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getMinBaseSideAmount().set(minRangeValue);
                        setMinQuoteFromBase();
                        minBaseSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getMinBaseSideAmount().set(amount);
                    }
                });

        maxOrFixedQuoteAmountFromCompPin = EasyBind.subscribe(maxOrFixedQuoteSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeQuoteSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeQuoteSideValue().get();
                    if (maxRangeValue != null && amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getMaxOrFixedQuoteSideAmount().set(maxRangeValue);
                        setMaxOrFixedBaseFromQuote();
                        maxOrFixedQuoteSideAmountInput.setAmount(maxRangeValue);
                    } else if (minRangeValue != null && amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getMaxOrFixedQuoteSideAmount().set(minRangeValue);
                        setMaxOrFixedBaseFromQuote();
                        maxOrFixedQuoteSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getMaxOrFixedQuoteSideAmount().set(amount);
                    }
                });

        minQuoteAmountFromCompPin = EasyBind.subscribe(minQuoteSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeQuoteSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeQuoteSideValue().get();
                    if (maxRangeValue != null && amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getMinQuoteSideAmount().set(maxRangeValue);
                        setMinBaseFromQuote();
                        minQuoteSideAmountInput.setAmount(maxRangeValue);
                    } else if (minRangeValue != null && amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getMinQuoteSideAmount().set(minRangeValue);
                        setMinBaseFromQuote();
                        minQuoteSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getMinQuoteSideAmount().set(amount);
                    }
                });

        priceFromCompPin = EasyBind.subscribe(price.getQuote(), quote -> applyInitialRangeValues());

        minRangeCustomValuePin = EasyBind.subscribe(model.getMinRangeMonetary(), value -> applyInitialRangeValues());
        maxRangeCustomValuePin = EasyBind.subscribe(model.getMaxRangeMonetary(), value -> applyInitialRangeValues());

        maxOrFixedQuoteSideAmountValidPin = subscribeToAmountValidity(maxOrFixedQuoteSideAmountInput, this::setMaxOrFixedQuoteFromBase);
        minQuoteSideAmountValidPin = subscribeToAmountValidity(minQuoteSideAmountInput, this::setMinQuoteFromBase);

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled ->
                model.getDescription().set(
                        Res.get(isRangeAmountEnabled
                                        ? "bisqEasy.tradeWizard.amount.description.range"
                                        : "bisqEasy.tradeWizard.amount.description.fixed"
                                , model.getMarket().getQuoteCurrencyCode())));

        model.getMaxOrFixedAmountSliderValue().addListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().addListener(minSliderListener);
    }

    @Override
    public void onDeactivate() {
        model.getMaxOrFixedQuoteSideAmount().removeListener(maxOrFixedQuoteSideAmountFromModelListener);
        model.getMinQuoteSideAmount().removeListener(minQuoteSideAmountFromModelListener);
        price.getQuote().removeListener(quoteListener);
        model.getMaxOrFixedAmountSliderValue().removeListener(maxOrFixedSliderListener);
        model.getMinAmountSliderValue().removeListener(minSliderListener);

        maxOrFixedBaseAmountFromModelPin.unsubscribe();
        minBaseAmountFromModelPin.unsubscribe();
        maxOrFixedBaseAmountFromCompPin.unsubscribe();
        minBaseAmountFromCompPin.unsubscribe();
        maxOrFixedQuoteAmountFromCompPin.unsubscribe();
        minQuoteAmountFromCompPin.unsubscribe();
        priceFromCompPin.unsubscribe();
        minRangeCustomValuePin.unsubscribe();
        maxRangeCustomValuePin.unsubscribe();
        maxOrFixedQuoteSideAmountValidPin.unsubscribe();
        minQuoteSideAmountValidPin.unsubscribe();
        isRangeAmountEnabledPin.unsubscribe();

        model.setLeftMarkerQuoteSideValue(null);
        model.setRightMarkerQuoteSideValue(null);
    }

    private void initializeQuoteSideAmount(QuoteAmountInputBox quoteSideAmountInput) {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote != null) {
            Monetary minRangeQuoteSideValue = model.getMinRangeQuoteSideValue().get();
            Monetary maxRangeQuoteSideValue = model.getMaxRangeQuoteSideValue().get();
            long midValue = minRangeQuoteSideValue.getValue() + (maxRangeQuoteSideValue.getValue() - minRangeQuoteSideValue.getValue()) / 2;
            Monetary exactAmount =  Fiat.fromValue(midValue, priceQuote.getQuoteSideMonetary().getCode());
            quoteSideAmountInput.setAmount(exactAmount.round(0));
        } else {
            log.warn("price.quoteProperty().get() is null. We use a fiat value of 100 as default value.");
            Fiat defaultQuoteSideAmount = Fiat.fromFaceValue(100, model.getMarket().getQuoteCurrencyCode());
            quoteSideAmountInput.setAmount(defaultQuoteSideAmount);
        }
    }

    private Subscription subscribeToAmountValidity(QuoteAmountInputBox amountInput, Runnable autocorrect) {
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
        boolean isMinRangeMonetaryFiat = FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(minRangeMonetary.getCode());
        boolean isMaxRangeMonetaryFiat = FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(maxRangeMonetary.getCode());

        Monetary minRangeMonetaryAsCoin = !isMinRangeMonetaryFiat ? minRangeMonetary : priceQuote.toBaseSideMonetary(minRangeMonetary);
        model.getMinRangeBaseSideValue().set(minRangeMonetaryAsCoin);
        if (!model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMinRangeValueAsString().set(AmountFormatter.formatAmount(minRangeMonetaryAsCoin));
            model.getMinRangeCodeAsString().set(minRangeMonetaryAsCoin.getCode());
        }

        Monetary maxRangeMonetaryAsCoin = !isMaxRangeMonetaryFiat ? maxRangeMonetary : priceQuote.toBaseSideMonetary(maxRangeMonetary);
        model.getMaxRangeBaseSideValue().set(maxRangeMonetaryAsCoin);
        if (!model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMaxRangeValueAsString().set(AmountFormatter.formatAmount(maxRangeMonetaryAsCoin));
            model.getMaxRangeCodeAsString().set(maxRangeMonetaryAsCoin.getCode());
        }

        Monetary minRangeMonetaryAsFiat = isMinRangeMonetaryFiat ? minRangeMonetary : priceQuote.toQuoteSideMonetary(minRangeMonetary).round(0);
        model.getMinRangeQuoteSideValue().set(minRangeMonetaryAsFiat);
        if (model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMinRangeValueAsString().set(AmountFormatter.formatAmount(minRangeMonetaryAsFiat));
            model.getMinRangeCodeAsString().set(minRangeMonetaryAsFiat.getCode());
        }

        Monetary maxRangeMonetaryAsFiat = isMaxRangeMonetaryFiat ? maxRangeMonetary : priceQuote.toQuoteSideMonetary(maxRangeMonetary).round(0);
        model.getMaxRangeQuoteSideValue().set(maxRangeMonetaryAsFiat);
        if (model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMaxRangeValueAsString().set(AmountFormatter.formatAmount(maxRangeMonetaryAsFiat));
            model.getMaxRangeCodeAsString().set(maxRangeMonetaryAsFiat.getCode());
        }

        applySliderTrackStyle();
    }

    private void applySliderTrackStyle() {
        Monetary minRangeMonetary = model.getMinRangeQuoteSideValue().get();
        Monetary maxRangeMonetary = model.getMaxRangeQuoteSideValue().get();
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

    private void applySliderValue(double sliderValue, QuoteAmountInputBox bigAmountInput, BaseAmountBox smallAmountInput) {
        if (model.getMinRangeQuoteSideValue().get() != null && model.getMinRangeBaseSideValue().get() != null) {
            long min = model.isUseQuoteCurrencyForMinMaxRange() ?
                    model.getMinRangeQuoteSideValue().get().getValue() :
                    model.getMinRangeBaseSideValue().get().getValue();
            long max = model.isUseQuoteCurrencyForMinMaxRange() ?
                    model.getMaxRangeQuoteSideValue().get().getValue() :
                    model.getMaxRangeBaseSideValue().get().getValue();
            long value = Math.round(sliderValue * (max - min)) + min;
            if (model.isUseQuoteCurrencyForMinMaxRange()) {
                bigAmountInput.setAmount(Monetary.from(value, model.getMarket().getQuoteCurrencyCode()));
            } else {
                smallAmountInput.setAmount(Monetary.from(value, model.getMarket().getBaseCurrencyCode()));
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
        maxOrFixedBaseSideAmountInput.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
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
        minBaseSideAmountInput.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
    }

    private void applyQuote() {
        if (model.getMaxOrFixedBaseSideAmount() == null) {
            setMaxOrFixedBaseFromQuote();
            setMinBaseFromQuote();
        } else {
            setMaxOrFixedQuoteFromBase();
            setMinQuoteFromBase();
        }
    }
}
