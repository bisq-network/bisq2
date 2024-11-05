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
import bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input.AmountInput;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input.BigAmountInput;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input.SmallAmountInput;
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
    private final BigAmountInput quoteSideAmountInput;
    private final SmallAmountInput baseSideAmountInput;
    private final ChangeListener<Monetary> baseSideAmountFromModelListener, quoteSideAmountFromModelListener;
    private final ChangeListener<PriceQuote> quoteListener;
    final PriceInput price;
    private final ChangeListener<Number> sliderListener;
    private Subscription baseAmountFromModelPin, baseAmountFromCompPin,
            quoteAmountFromCompPin, priceFromCompPin, minRangeCustomValuePin, maxRangeCustomValuePin,
            baseSideAmountValidPin, quoteSideAmountValidPin;

    public AmountSelectionController(ServiceProvider serviceProvider,
                                     boolean useQuoteCurrencyForMinMaxRange) {
        quoteSideAmountInput = new BigAmountInput(false);
        baseSideAmountInput = new SmallAmountInput(true);
        baseSideAmountInput.setUseLowPrecision(false);
        price = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());

        model = new AmountSelectionModel(useQuoteCurrencyForMinMaxRange);
        view = new AmountSelectionView(model, this,
                baseSideAmountInput,
                quoteSideAmountInput);

        // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
        // apply the conversion before the other component has processed the market change event.
        // The order of the event notification is not deterministic.
        baseSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setQuoteFromBase);
        quoteSideAmountFromModelListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setBaseFromQuote);
        quoteListener = (observable, oldValue, newValue) -> {
            model.getMinRangeBaseSideValue().set(null);
            model.getMaxRangeBaseSideValue().set(null);
            model.getMinRangeQuoteSideValue().set(null);
            model.getMaxRangeQuoteSideValue().set(null);
            applyInitialRangeValues();
            UIThread.runOnNextRenderFrame(this::applyQuote);
        };
        sliderListener = (observable, oldValue, newValue) -> {
            if (model.getMinRangeQuoteSideValue().get() != null && model.getMinRangeBaseSideValue().get() != null) {
                double sliderValue = newValue.doubleValue();
                long min = model.isUseQuoteCurrencyForMinMaxRange() ?
                        model.getMinRangeQuoteSideValue().get().getValue() :
                        model.getMinRangeBaseSideValue().get().getValue();
                long max = model.isUseQuoteCurrencyForMinMaxRange() ?
                        model.getMaxRangeQuoteSideValue().get().getValue() :
                        model.getMaxRangeBaseSideValue().get().getValue();
                long value = Math.round(sliderValue * (max - min)) + min;
                if (model.isUseQuoteCurrencyForMinMaxRange()) {
                    quoteSideAmountInput.setAmount(Monetary.from(value, model.getMarket().getQuoteCurrencyCode()));
                } else {
                    baseSideAmountInput.setAmount(Monetary.from(value, model.getMarket().getBaseCurrencyCode()));
                }
            }
        };
    }

    public void setBaseSideAmount(Monetary value) {
        model.getBaseSideAmount().set(value);
    }

    public void setQuoteSideAmount(Monetary value) {
        model.getQuoteSideAmount().set(value);
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideAmount() {
        return model.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideAmount() {
        return model.getQuoteSideAmount();
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        model.getSpendOrReceiveString().set(direction == Direction.BUY ? Res.get("offer.buying") : Res.get("offer.selling"));
    }

    public void setTooltip(String tooltip) {
        baseSideAmountInput.setTooltip(tooltip);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.setMarket(market);
        baseSideAmountInput.setSelectedMarket(market);
        quoteSideAmountInput.setSelectedMarket(market);
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
        quoteSideAmountInput.setAmount(model.getRightMarkerQuoteSideValue());
    }

    public void setQuote(PriceQuote priceQuote) {
        if (priceQuote != null) {
            price.setQuote(priceQuote);
        }
    }

    public ReadOnlyObjectProperty<PriceQuote> getQuote() {
        return price.getQuote();
    }

    public Monetary getRightMarkerQuoteSideValue() {
        return model.getRightMarkerQuoteSideValue();
    }

    public void useCompactFormat(boolean useCompactFormat) {
        model.getUseCompactFormat().set(useCompactFormat);
    }

    public void showHyphenInsteadOfCurrencyCode(boolean showHyphenInsteadOfCurrencyCode) {
        quoteSideAmountInput.setShowHyphenInsteadOfCurrencyCode(showHyphenInsteadOfCurrencyCode);
        baseSideAmountInput.setShowHyphenInsteadOfCurrencyCode(showHyphenInsteadOfCurrencyCode);
    }

    public void reset() {
        baseSideAmountInput.reset();
        quoteSideAmountInput.reset();
        price.reset();
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getMinRangeBaseSideValue().set(null);
        model.getMaxRangeBaseSideValue().set(null);
        model.getMinRangeQuoteSideValue().set(null);
        model.getMaxRangeQuoteSideValue().set(null);
        model.getDescription().set(Res.get("bisqEasy.tradeWizard.amount.description", model.getMarket().getQuoteCurrencyCode()));
        applyInitialRangeValues();

        model.getBaseSideAmount().addListener(baseSideAmountFromModelListener);
        model.getQuoteSideAmount().addListener(quoteSideAmountFromModelListener);
        price.getQuote().addListener(quoteListener);

        baseSideAmountInput.setAmount(null);
        if (model.getQuoteSideAmount().get() == null) {
            PriceQuote priceQuote = price.getQuote().get();
            if (priceQuote != null) {
                Monetary minRangeQuoteSideValue = model.getMinRangeQuoteSideValue().get();
                Monetary maxRangeQuoteSideValue = model.getMaxRangeQuoteSideValue().get();
                long midValue = minRangeQuoteSideValue.getValue() + (maxRangeQuoteSideValue.getValue() - minRangeQuoteSideValue.getValue()) / 2;
                Monetary exactAmount = Fiat.fromValue(midValue, priceQuote.getQuoteSideMonetary().getCode());
                quoteSideAmountInput.setAmount(exactAmount.round(0));
            } else {
                log.warn("price.quoteProperty().get() is null. We use a fiat value of 100 as default value.");
                Fiat defaultQuoteSideAmount = Fiat.fromFaceValue(100, model.getMarket().getQuoteCurrencyCode());
                quoteSideAmountInput.setAmount(defaultQuoteSideAmount);
            }
        } else {
            quoteSideAmountInput.setAmount(model.getQuoteSideAmount().get());
        }
        setBaseFromQuote();

        baseAmountFromModelPin = EasyBind.subscribe(model.getBaseSideAmount(), amount -> {
            // Only apply value from component to slider if we have no focus on slider (not used)
            if (amount != null) {
                if (!model.getSliderFocus().get()) {
                    long min = model.getMinRangeBaseSideValue().get().getValue();
                    long max = model.getMaxRangeBaseSideValue().get().getValue();
                    double sliderValue = (amount.getValue() - min) / ((double) max - min);
                    model.getSliderValue().set(sliderValue);
                }
            }
        });

        baseAmountFromCompPin = EasyBind.subscribe(baseSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeBaseSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeBaseSideValue().get();
                    if (amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getBaseSideAmount().set(maxRangeValue);
                        setQuoteFromBase();
                        baseSideAmountInput.setAmount(maxRangeValue);
                    } else if (amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getBaseSideAmount().set(minRangeValue);
                        setQuoteFromBase();
                        baseSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getBaseSideAmount().set(amount);
                    }
                });

        quoteAmountFromCompPin = EasyBind.subscribe(quoteSideAmountInput.amountProperty(),
                amount -> {
                    Monetary minRangeValue = model.getMinRangeQuoteSideValue().get();
                    Monetary maxRangeValue = model.getMaxRangeQuoteSideValue().get();
                    if (maxRangeValue != null && amount != null && amount.getValue() > maxRangeValue.getValue()) {
                        model.getQuoteSideAmount().set(maxRangeValue);
                        setBaseFromQuote();
                        quoteSideAmountInput.setAmount(maxRangeValue);
                    } else if (minRangeValue != null && amount != null && amount.getValue() < minRangeValue.getValue()) {
                        model.getQuoteSideAmount().set(minRangeValue);
                        setBaseFromQuote();
                        quoteSideAmountInput.setAmount(minRangeValue);
                    } else {
                        model.getQuoteSideAmount().set(amount);
                    }
                });
        priceFromCompPin = EasyBind.subscribe(price.getQuote(),
                quote -> applyInitialRangeValues());

        minRangeCustomValuePin = EasyBind.subscribe(model.getMinRangeMonetary(),
                value -> applyInitialRangeValues());
        maxRangeCustomValuePin = EasyBind.subscribe(model.getMaxRangeMonetary(),
                value -> applyInitialRangeValues());

        baseSideAmountValidPin = subscribeToAmountValidity(baseSideAmountInput, this::setBaseFromQuote);
        quoteSideAmountValidPin = subscribeToAmountValidity(quoteSideAmountInput, this::setQuoteFromBase);
        model.getSliderValue().addListener(sliderListener);
    }

    private Subscription subscribeToAmountValidity(AmountInput amountInput, Runnable autocorrect) {
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

        Monetary minRangeMonetaryAsCoin = !isMinRangeMonetaryFiat ?
                minRangeMonetary :
                priceQuote.toBaseSideMonetary(minRangeMonetary);
        model.getMinRangeBaseSideValue().set(minRangeMonetaryAsCoin);
        if (!model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMinRangeValueAsString().set(Res.get("bisqEasy.component.amount.minRangeValue",
                    AmountFormatter.formatAmountWithCode(minRangeMonetaryAsCoin)));
        }

        Monetary maxRangeMonetaryAsCoin = !isMaxRangeMonetaryFiat ?
                maxRangeMonetary :
                priceQuote.toBaseSideMonetary(maxRangeMonetary);
        model.getMaxRangeBaseSideValue().set(maxRangeMonetaryAsCoin);
        if (!model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMaxRangeValueAsString().set(Res.get("bisqEasy.component.amount.maxRangeValue",
                    AmountFormatter.formatAmountWithCode(maxRangeMonetaryAsCoin)));
        }

        Monetary minRangeMonetaryAsFiat = isMinRangeMonetaryFiat ?
                minRangeMonetary :
                priceQuote.toQuoteSideMonetary(minRangeMonetary).round(0);
        model.getMinRangeQuoteSideValue().set(minRangeMonetaryAsFiat);
        if (model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMinRangeValueAsString().set(Res.get("bisqEasy.component.amount.minRangeValue",
                    AmountFormatter.formatAmountWithCode(minRangeMonetaryAsFiat)));
        }

        Monetary maxRangeMonetaryAsFiat = isMaxRangeMonetaryFiat ?
                maxRangeMonetary :
                priceQuote.toQuoteSideMonetary(maxRangeMonetary).round(0);
        model.getMaxRangeQuoteSideValue().set(maxRangeMonetaryAsFiat);
        if (model.isUseQuoteCurrencyForMinMaxRange()) {
            model.getMaxRangeValueAsString().set(Res.get("bisqEasy.component.amount.maxRangeValue",
                    AmountFormatter.formatAmountWithCode(maxRangeMonetaryAsFiat)));
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

    @Override
    public void onDeactivate() {
        model.getBaseSideAmount().removeListener(baseSideAmountFromModelListener);
        model.getQuoteSideAmount().removeListener(quoteSideAmountFromModelListener);
        price.getQuote().removeListener(quoteListener);
        model.getSliderValue().removeListener(sliderListener);
        baseAmountFromModelPin.unsubscribe();
        baseAmountFromCompPin.unsubscribe();
        quoteAmountFromCompPin.unsubscribe();
        priceFromCompPin.unsubscribe();
        minRangeCustomValuePin.unsubscribe();
        maxRangeCustomValuePin.unsubscribe();
        baseSideAmountValidPin.unsubscribe();
        quoteSideAmountValidPin.unsubscribe();
        model.setLeftMarkerQuoteSideValue(null);
        model.setRightMarkerQuoteSideValue(null);
    }

    private void setQuoteFromBase() {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote == null) return;
        Monetary baseSideAmount = model.getBaseSideAmount().get();
        if (baseSideAmount == null) return;
        quoteSideAmountInput.setAmount(priceQuote.toQuoteSideMonetary(baseSideAmount).round(0));
    }

    private void setBaseFromQuote() {
        PriceQuote priceQuote = price.getQuote().get();
        if (priceQuote == null) return;
        Monetary quoteSideAmount = model.getQuoteSideAmount().get();
        if (quoteSideAmount == null) return;
        baseSideAmountInput.setAmount(priceQuote.toBaseSideMonetary(quoteSideAmount));
    }

    private void applyQuote() {
        if (model.getBaseSideAmount() == null) {
            setBaseFromQuote();
        } else {
            setQuoteFromBase();
        }
    }
}
