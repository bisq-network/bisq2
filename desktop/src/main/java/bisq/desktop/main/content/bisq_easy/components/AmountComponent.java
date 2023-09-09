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

package bisq.desktop.main.content.bisq_easy.components;

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static bisq.desktop.components.controls.validator.ValidatorBase.PSEUDO_CLASS_ERROR;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AmountComponent {
    private static final Coin MIN_RANGE_BASE_SIDE_VALUE = Coin.asBtcFromValue(10000);
    private static final Coin MAX_RANGE_BASE_SIDE_VALUE = Coin.asBtcFromValue(1000000);

    private final Controller controller;

    public AmountComponent(ServiceProvider serviceProvider,
                           boolean useQuoteCurrencyForMinMaxRange) {
        controller = new Controller(serviceProvider, useQuoteCurrencyForMinMaxRange);
    }

    public View getView() {
        return controller.getView();
    }

    public ReadOnlyObjectProperty<Monetary> getBaseSideAmount() {
        return controller.getBaseSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getQuoteSideAmount() {
        return controller.getQuoteSideAmount();
    }

    public BooleanProperty areAmountsValid() {
        return controller.model.areAmountsValid;
    }

    public void setBaseSideAmount(Monetary value) {
        controller.setBaseSideAmount(value);
    }

    public void setQuoteSideAmount(Monetary value) {
        controller.setQuoteSideAmount(value);
    }

    public void setDirection(Direction direction) {
        controller.setDirection(direction);
    }

    public void setMarket(Market market) {
        controller.setMarket(market);
    }

    public void setMinMaxRange(Monetary minRangeValue, Monetary maxRangeValue) {
        controller.setMinMaxRange(minRangeValue, maxRangeValue);
    }

    public void setTooltip(String tooltip) {
        controller.setTooltip(tooltip);
    }

    public void setQuote(PriceQuote priceQuote) {
        if (priceQuote != null) {
            controller.setQuote(priceQuote);
        }
    }

    public void reset() {
        controller.reset();
    }

    public void setDescription(String description) {
        controller.setDescription(description);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final BigAmountInput quoteSideAmountInput;
        private final SmallAmountInput baseSideAmountInput;
        private final ChangeListener<Monetary> baseSideAmountFromModelListener, quoteSideAmountFromModelListener;
        private final ChangeListener<PriceQuote> quoteListener;
        private final PriceInput price;
        private final ChangeListener<Number> sliderListener;
        private Subscription baseAmountFromModelPin, baseAmountFromCompPin,
                quoteAmountFromCompPin, priceFromCompPin, minRangeCustomValuePin, maxRangeCustomValuePin,
                areAmountsValidPin, baseSideAmountValidPin, quoteSideAmountValidPin;

        private Controller(ServiceProvider serviceProvider,
                           boolean useQuoteCurrencyForMinMaxRange) {
            quoteSideAmountInput = new BigAmountInput(false);
            baseSideAmountInput = new SmallAmountInput(true);
            baseSideAmountInput.setUseLowPrecision(false);
            price = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());

            model = new Model(useQuoteCurrencyForMinMaxRange);
            view = new View(model, this,
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
                    long min = model.useQuoteCurrencyForMinMaxRange ?
                            model.getMinRangeQuoteSideValue().get().getValue() :
                            model.getMinRangeBaseSideValue().get().getValue();
                    long max = model.useQuoteCurrencyForMinMaxRange ?
                            model.getMaxRangeQuoteSideValue().get().getValue() :
                            model.getMaxRangeBaseSideValue().get().getValue();
                    long value = Math.round(sliderValue * (max - min)) + min;
                    if (model.useQuoteCurrencyForMinMaxRange) {
                        quoteSideAmountInput.setAmount(Monetary.from(value, model.getMarket().getQuoteCurrencyCode()));
                    } else {
                        baseSideAmountInput.setAmount(Monetary.from(value, model.getMarket().getBaseCurrencyCode()));
                    }
                }
            };
        }

        private void setBaseSideAmount(Monetary value) {
            model.getBaseSideAmount().set(value);
        }

        private void setQuoteSideAmount(Monetary value) {
            model.getQuoteSideAmount().set(value);
        }

        private ReadOnlyObjectProperty<Monetary> getBaseSideAmount() {
            return model.getBaseSideAmount();
        }

        private ReadOnlyObjectProperty<Monetary> getQuoteSideAmount() {
            return model.getQuoteSideAmount();
        }

        private void setDirection(Direction direction) {
            if (direction == null) {
                return;
            }
            model.setDirection(direction);
            model.getSpendOrReceiveString().set(direction == Direction.BUY ? Res.get("offer.buying") : Res.get("offer.selling"));
        }

        private void setTooltip(String tooltip) {
            baseSideAmountInput.setTooltip(tooltip);
        }

        private void setMarket(Market market) {
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
            if (model.useQuoteCurrencyForMinMaxRange) {
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

        public void setQuote(PriceQuote priceQuote) {
            price.setQuote(priceQuote);
        }

        private void reset() {
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
                    Fiat defaultQuoteSideAmount = Fiat.fromValue(1000000, model.getMarket().getQuoteCurrencyCode());
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

            baseSideAmountValidPin = subscribeToAmountValidity(baseSideAmountInput);
            quoteSideAmountValidPin = subscribeToAmountValidity(quoteSideAmountInput);
            var binding = EasyBind.combine(
                    baseSideAmountInput.isAmountValidProperty(),
                    quoteSideAmountInput.isAmountValidProperty(),
                    (isBaseAmountValid, isQuoteAmountValid) -> isBaseAmountValid && isQuoteAmountValid);
            areAmountsValidPin = EasyBind.subscribe(binding, model.areAmountsValid::set);

            model.getSliderValue().addListener(sliderListener);
        }

        private Subscription subscribeToAmountValidity(AmountInput amountInput) {
            return EasyBind.subscribe(amountInput.isAmountValidProperty(), isAmountValid -> {
                if (amountInput.focusedProperty().get()) {
                    model.isFocusedAmountValid.set(isAmountValid);
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

            if (model.getMinRangeBaseSideValue().get() == null) {
                Monetary minRangeMonetaryAsCoin = !isMinRangeMonetaryFiat ?
                        minRangeMonetary :
                        priceQuote.toBaseSideMonetary(minRangeMonetary);
                model.getMinRangeBaseSideValue().set(minRangeMonetaryAsCoin);
                if (!model.useQuoteCurrencyForMinMaxRange) {
                    model.getMinRangeValueAsString().set(Res.get("bisqEasy.component.amount.minRangeValue",
                            AmountFormatter.formatAmountWithCode(minRangeMonetaryAsCoin)));
                }
            }
            if (model.getMaxRangeBaseSideValue().get() == null) {
                Monetary maxRangeMonetaryAsCoin = !isMaxRangeMonetaryFiat ?
                        maxRangeMonetary :
                        priceQuote.toBaseSideMonetary(maxRangeMonetary);
                model.getMaxRangeBaseSideValue().set(maxRangeMonetaryAsCoin);
                if (!model.useQuoteCurrencyForMinMaxRange) {
                    model.getMaxRangeValueAsString().set(Res.get("bisqEasy.component.amount.maxRangeValue",
                            AmountFormatter.formatAmountWithCode(maxRangeMonetaryAsCoin)));
                }
            }

            if (model.getMinRangeQuoteSideValue().get() == null) {
                Monetary minRangeMonetaryAsFiat = isMinRangeMonetaryFiat ?
                        minRangeMonetary :
                        priceQuote.toQuoteSideMonetary(minRangeMonetary).round(0);
                model.getMinRangeQuoteSideValue().set(minRangeMonetaryAsFiat);
                if (model.useQuoteCurrencyForMinMaxRange) {
                    model.getMinRangeValueAsString().set(Res.get("bisqEasy.component.amount.minRangeValue",
                            AmountFormatter.formatAmountWithCode(minRangeMonetaryAsFiat)));
                }
            }

            if (model.getMaxRangeQuoteSideValue().get() == null) {
                Monetary maxRangeMonetaryAsFiat = isMaxRangeMonetaryFiat ?
                        maxRangeMonetary :
                        priceQuote.toQuoteSideMonetary(maxRangeMonetary).round(0);
                model.getMaxRangeQuoteSideValue().set(maxRangeMonetaryAsFiat);
                if (model.useQuoteCurrencyForMinMaxRange) {
                    model.getMaxRangeValueAsString().set(Res.get("bisqEasy.component.amount.maxRangeValue",
                            AmountFormatter.formatAmountWithCode(maxRangeMonetaryAsFiat)));
                }
            }

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
            areAmountsValidPin.unsubscribe();
            baseSideAmountValidPin.unsubscribe();
            quoteSideAmountValidPin.unsubscribe();
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


    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final boolean useQuoteCurrencyForMinMaxRange;
        private final double sliderMin = 0;
        private final double sliderMax = 1;

        private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
        private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
        private final StringProperty spendOrReceiveString = new SimpleStringProperty();

        private final DoubleProperty sliderValue = new SimpleDoubleProperty();
        private final BooleanProperty sliderFocus = new SimpleBooleanProperty();
        private final BooleanProperty areAmountsValid = new SimpleBooleanProperty(true);
        private final BooleanProperty isFocusedAmountValid = new SimpleBooleanProperty(true);

        @Setter
        private ObjectProperty<Monetary> minRangeMonetary = new SimpleObjectProperty<>(MIN_RANGE_BASE_SIDE_VALUE);
        @Setter
        private ObjectProperty<Monetary> maxRangeMonetary = new SimpleObjectProperty<>(MAX_RANGE_BASE_SIDE_VALUE);
        @Setter
        private ObjectProperty<Monetary> minRangeBaseSideValue = new SimpleObjectProperty<>();
        @Setter
        private ObjectProperty<Monetary> maxRangeBaseSideValue = new SimpleObjectProperty<>();
        @Setter
        private ObjectProperty<Monetary> minRangeQuoteSideValue = new SimpleObjectProperty<>();
        @Setter
        private ObjectProperty<Monetary> maxRangeQuoteSideValue = new SimpleObjectProperty<>();
        @Setter
        private Market market = MarketRepository.getDefault();
        @Setter
        private Direction direction = Direction.BUY;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty minRangeValueAsString = new SimpleStringProperty();
        private final StringProperty maxRangeValueAsString = new SimpleStringProperty();

        Model(boolean useQuoteCurrencyForMinMaxRange) {
            this.useQuoteCurrencyForMinMaxRange = useQuoteCurrencyForMinMaxRange;
        }

        void reset() {
            baseSideAmount.set(null);
            quoteSideAmount.set(null);
            spendOrReceiveString.set(null);
            sliderValue.set(0L);
            sliderFocus.set(false);
            market = MarketRepository.getDefault();
            direction = Direction.BUY;
            areAmountsValid.set(true);
            isFocusedAmountValid.set(true);
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static int AMOUNT_BOX_WIDTH = 330;
        private final Slider slider;
        private final Label minRangeValue, maxRangeValue, description;
        private final Region selectionLine;
        private final SmallAmountInput baseAmount;
        private final BigAmountInput quoteAmount;
        private Subscription baseAmountFocusPin, quoteAmountFocusPin, isFocusedAmountValidPin;

        private View(Model model, AmountComponent.Controller controller, SmallAmountInput baseAmount, BigAmountInput quoteAmount) {
            super(new VBox(10), model, controller);

            Pane baseAmountRoot = baseAmount.getRoot();
            this.baseAmount = baseAmount;
            Pane quoteAmountRoot = quoteAmount.getRoot();
            this.quoteAmount = quoteAmount;

            root.setAlignment(Pos.TOP_CENTER);

            description = new Label();
            description.setTextAlignment(TextAlignment.CENTER);
            description.setAlignment(Pos.CENTER);
            description.getStyleClass().addAll("bisq-text-3", "wrap-text");

            VBox.setMargin(quoteAmountRoot, new Insets(-15, 0, 0, 0));
            VBox.setMargin(baseAmountRoot, new Insets(-17, 0, 0, 0));
            VBox vbox = new VBox(0, quoteAmountRoot, baseAmountRoot);
            vbox.getStyleClass().add("bisq-dual-amount-bg");
            vbox.setAlignment(Pos.CENTER);
            vbox.setMinWidth(AMOUNT_BOX_WIDTH);
            vbox.setMaxWidth(AMOUNT_BOX_WIDTH);
            vbox.setPadding(new Insets(25, 20, 10, 20));

            Region line = new Region();
            line.setLayoutY(121);
            line.setPrefHeight(1);
            line.setPrefWidth(AMOUNT_BOX_WIDTH);
            line.setStyle("-fx-background-color: -bisq-grey-dimmed");
            line.setMouseTransparent(true);

            selectionLine = new Region();
            selectionLine.getStyleClass().add("material-text-field-selection-line");
            selectionLine.setPrefHeight(3);
            selectionLine.setPrefWidth(0);
            selectionLine.setLayoutY(119);
            selectionLine.setMouseTransparent(true);

            Pane amountPane = new Pane(vbox, line, selectionLine);
            amountPane.setMaxWidth(AMOUNT_BOX_WIDTH);

            slider = new Slider();
            slider.setMin(model.getSliderMin());
            slider.setMax(model.getSliderMax());

            minRangeValue = new Label();
            minRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

            maxRangeValue = new Label();
            maxRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

            VBox sliderBox = new VBox(2, slider, new HBox(minRangeValue, Spacer.fillHBox(), maxRangeValue));
            sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH);

            VBox.setMargin(amountPane, new Insets(0, 0, 28, 0));
            root.getChildren().addAll(description, amountPane, sliderBox);
        }

        @Override
        protected void onViewAttached() {
            UIScheduler.run(() -> {
                quoteAmount.requestFocus();
                baseAmountFocusPin = EasyBind.subscribe(baseAmount.focusedProperty(),
                        focus -> onInputTextFieldFocus(quoteAmount.focusedProperty(), focus, baseAmount.isAmountValidProperty()));
                quoteAmountFocusPin = EasyBind.subscribe(quoteAmount.focusedProperty(),
                        focus -> onInputTextFieldFocus(baseAmount.focusedProperty(), focus, quoteAmount.isAmountValidProperty()));
                isFocusedAmountValidPin = EasyBind.subscribe(model.isFocusedAmountValid, this::updateSelectionLine);
            }).after(700);

            slider.valueProperty().bindBidirectional(model.getSliderValue());
            model.getSliderFocus().bind(slider.focusedProperty());
            description.textProperty().bind(model.description);
            minRangeValue.textProperty().bind(model.getMinRangeValueAsString());
            maxRangeValue.textProperty().bind(model.getMaxRangeValueAsString());
            // Needed to trigger focusOut event on amount components
            // We handle all parents mouse events.
            Parent node = root;
            while (node.getParent() != null) {
                node.setOnMousePressed(e -> root.requestFocus());
                node = node.getParent();
            }
        }

        @Override
        protected void onViewDetached() {
            if (baseAmountFocusPin != null) {
                baseAmountFocusPin.unsubscribe();
            }
            if (quoteAmountFocusPin != null) {
                quoteAmountFocusPin.unsubscribe();
            }
            if (isFocusedAmountValidPin != null) {
                isFocusedAmountValidPin.unsubscribe();
            }
            slider.valueProperty().unbindBidirectional(model.getSliderValue());
            model.getSliderFocus().unbind();
            description.textProperty().unbind();
            minRangeValue.textProperty().unbind();
            maxRangeValue.textProperty().unbind();

            Parent node = root;
            while (node.getParent() != null) {
                node.setOnMousePressed(null);
                node = node.getParent();
            }
        }

        private void onInputTextFieldFocus(ReadOnlyBooleanProperty other, boolean focus, BooleanProperty isAmountValid) {
            if (focus) {
                selectionLine.setPrefWidth(0);
                selectionLine.setOpacity(1);
                model.isFocusedAmountValid.set(isAmountValid.getValue());
                Transitions.animateWidth(selectionLine, AMOUNT_BOX_WIDTH);
            } else if (!other.get()) {
                // If switching between the 2 fields we want to avoid to get the fadeout called that's why
                // we do the check with !other.get()  
                Transitions.fadeOut(selectionLine, 200);
            }
        }

        private void updateSelectionLine(boolean isAmountValid) {
            selectionLine.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, !isAmountValid);
        }
    }
}