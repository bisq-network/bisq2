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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.PriceInput;
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

public class AmountComponent {
    private final Controller controller;

    public AmountComponent(DefaultApplicationService applicationService, ReadOnlyStringProperty description) {
        controller = new Controller(applicationService, description);
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

    public void reset() {
        controller.reset();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final BigAmountInput quoteAmount;
        private final SmallAmountInput baseAmount;
        private final ChangeListener<Monetary> baseCurrencyAmountListener, quoteCurrencyAmountListener;
        private final ChangeListener<Quote> fixPriceQuoteListener;
        private final PriceInput price;
        private final ChangeListener<Number> sliderListener;
        private long minAmount, minMaxDiff;
        private Subscription baseAmountFromModelSubscription, baseAmountFromCompSubscription,
                quoteAmountFromCompSubscription, priceFromCompSubscription;

        private Controller(DefaultApplicationService applicationService, ReadOnlyStringProperty description) {
            quoteAmount = new BigAmountInput(false);
            baseAmount = new SmallAmountInput(true);
            price = new PriceInput(applicationService.getOracleService().getMarketPriceService());

            model = new Model(description);
            view = new View(model, this,
                    baseAmount,
                    quoteAmount);

            // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
            // apply the conversion before the other component has processed the market change event.
            // The order of the event notification is not deterministic. 
            baseCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setQuoteFromBase);
            quoteCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setBaseFromQuote);
            fixPriceQuoteListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::applyQuote);

            sliderListener = (observable, oldValue, newValue) -> {
                double sliderValue = newValue.doubleValue();
                long value = Math.round(sliderValue * minMaxDiff) + minAmount;
                Coin amount = Coin.of(value, "BTC");
                baseAmount.setAmount(amount);
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
            model.getSpendOrReceiveString().set(direction == Direction.BUY ? Res.get("buying") : Res.get("selling"));
        }

        private void setMarket(Market market) {
            if (market == null) {
                return;
            }
            model.setMarket(market);
            baseAmount.setSelectedMarket(market);
            quoteAmount.setSelectedMarket(market);
            price.setSelectedMarket(market);
        }

        private void reset() {
            baseAmount.reset();
            quoteAmount.reset();
            price.reset();
            model.reset();
        }

        @Override
        public void onActivate() {
            model.getBaseSideAmount().addListener(baseCurrencyAmountListener);
            model.getQuoteSideAmount().addListener(quoteCurrencyAmountListener);
            model.getQuote().addListener(fixPriceQuoteListener);

            minAmount = model.getMinAmount().getValue();
            minMaxDiff = model.getMaxAmount().getValue() - minAmount;

            baseAmount.setAmount(null);
            if (model.getQuoteSideAmount().get() == null) {
                // We use 0.004 BTC as default value converted with the price to the fiat amount
                Quote fixPrice = price.fixPriceProperty().get();
                if (fixPrice != null) {
                    Monetary baseCurrencyAmount = Coin.asBtc(400000);
                    Monetary exactAmount = fixPrice.toQuoteMonetary(baseCurrencyAmount);
                    quoteAmount.setAmount(exactAmount.round(0));
                } else {
                    quoteAmount.setAmount(Fiat.parse("100", model.getMarket().getQuoteCurrencyCode()));
                }
            } else {
                quoteAmount.setAmount(model.getQuoteSideAmount().get());
            }
            setBaseFromQuote();

            baseAmountFromModelSubscription = EasyBind.subscribe(model.getBaseSideAmount(), amount -> {
                // Only apply value from component to slider if we have no focus on slider (not used)
                if (amount != null && !model.getSliderFocus().get()) {
                    double sliderValue = (amount.getValue() - minAmount) / (double) minMaxDiff;
                    model.getSliderValue().set(sliderValue);
                }
            });

            baseAmountFromCompSubscription = EasyBind.subscribe(baseAmount.amountProperty(),
                    amount -> {
                        if (amount != null && amount.getValue() > model.getMaxAmount().getValue()) {
                            model.getBaseSideAmount().set(model.getMaxAmount());
                            setQuoteFromBase();
                            baseAmount.setAmount(model.getBaseSideAmount().get());
                        } else if (amount != null && amount.getValue() < model.getMinAmount().getValue()) {
                            model.getBaseSideAmount().set(model.getMinAmount());
                            setQuoteFromBase();
                            baseAmount.setAmount(model.getBaseSideAmount().get());
                        } else {
                            model.getBaseSideAmount().set(amount);
                        }
                    });
            quoteAmountFromCompSubscription = EasyBind.subscribe(quoteAmount.amountProperty(),
                    amount -> model.getQuoteSideAmount().set(amount));
            priceFromCompSubscription = EasyBind.subscribe(price.fixPriceProperty(),
                    price -> model.getQuote().set(price));

            model.getSliderValue().addListener(sliderListener);
        }

        @Override
        public void onDeactivate() {
            model.getBaseSideAmount().removeListener(baseCurrencyAmountListener);
            model.getQuoteSideAmount().removeListener(quoteCurrencyAmountListener);
            model.getQuote().removeListener(fixPriceQuoteListener);
            model.getSliderValue().removeListener(sliderListener);
            baseAmountFromModelSubscription.unsubscribe();
            baseAmountFromCompSubscription.unsubscribe();
            quoteAmountFromCompSubscription.unsubscribe();
            priceFromCompSubscription.unsubscribe();
        }

        private void setQuoteFromBase() {
            Quote fixPrice = model.getQuote().get();
            if (fixPrice == null) return;
            Monetary baseCurrencyAmount = model.getBaseSideAmount().get();
            if (baseCurrencyAmount == null) return;
            if (fixPrice.getBaseMonetary().getClass() != baseCurrencyAmount.getClass()) return;

            Monetary exactAmount = fixPrice.toQuoteMonetary(baseCurrencyAmount);
            quoteAmount.setAmount(exactAmount.round(0));
        }

        private void setBaseFromQuote() {
            Quote fixPrice = model.getQuote().get();
            if (fixPrice == null) return;
            Monetary quoteCurrencyAmount = model.getQuoteSideAmount().get();
            if (quoteCurrencyAmount == null) return;
            if (fixPrice.getQuoteMonetary().getClass() != quoteCurrencyAmount.getClass()) return;
            baseAmount.setAmount(fixPrice.toBaseMonetary(quoteCurrencyAmount));
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
        private final ReadOnlyStringProperty description;
        private final Monetary minAmount = Coin.asBtc(10000);
        private final Monetary maxAmount = Coin.asBtc(1000000);
        private final double sliderMin = 0;
        private final double sliderMax = 1;

        private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
        private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
        private final ObjectProperty<Quote> quote = new SimpleObjectProperty<>();
        private final StringProperty spendOrReceiveString = new SimpleStringProperty();

        private final DoubleProperty sliderValue = new SimpleDoubleProperty();
        private final BooleanProperty sliderFocus = new SimpleBooleanProperty();

        @Setter
        private Market market = MarketRepository.getDefault();
        @Setter
        private Direction direction = Direction.BUY;

        Model(ReadOnlyStringProperty description) {
            this.description = description;
        }

        void reset() {
            baseSideAmount.set(null);
            quoteSideAmount.set(null);
            quote.set(null);
            spendOrReceiveString.set(null);
            sliderValue.set(0L);
            sliderFocus.set(false);
            market = MarketRepository.getDefault();
            direction = Direction.BUY;
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static int AMOUNT_BOX_WIDTH = 330;
        private final Slider slider;
        private final Label minAmountLabel, maxAmountLabel, descriptionLabel;
        private final Region line, selectionLine;
        private final Pane baseAmountRoot, quoteAmountRoot;
        private final SmallAmountInput baseAmount;
        private final BigAmountInput quoteAmount;
        private Subscription baseAmountFocusPin, quoteAmountFocusPin;

        private View(Model model, Controller controller, SmallAmountInput baseAmount, BigAmountInput quoteAmount) {
            super(new VBox(10), model, controller);

            baseAmountRoot = baseAmount.getRoot();
            this.baseAmount = baseAmount;
            quoteAmountRoot = quoteAmount.getRoot();
            this.quoteAmount = quoteAmount;

            root.setAlignment(Pos.TOP_CENTER);

            descriptionLabel = new Label();
            descriptionLabel.setTextAlignment(TextAlignment.CENTER);
            descriptionLabel.setAlignment(Pos.CENTER);
            descriptionLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

            VBox.setMargin(quoteAmountRoot, new Insets(-15, 0, 0, 0));
            VBox.setMargin(baseAmountRoot, new Insets(-17, 0, 0, 0));
            VBox vbox = new VBox(0, quoteAmountRoot, baseAmountRoot);
            vbox.getStyleClass().add("bisq-dual-amount-bg");
            vbox.setAlignment(Pos.CENTER);
            vbox.setMinWidth(AMOUNT_BOX_WIDTH);
            vbox.setMaxWidth(AMOUNT_BOX_WIDTH);
            vbox.setPadding(new Insets(25, 20, 10, 20));

            line = new Region();
            line.setLayoutY(121);
            line.setPrefHeight(1);
            line.setPrefWidth(AMOUNT_BOX_WIDTH);
            line.setStyle("-fx-background-color: -bisq-grey-dimmed");
            line.setMouseTransparent(true);

            selectionLine = new Region();
            selectionLine.getStyleClass().add("bisq-green-line");
            selectionLine.setPrefHeight(3);
            selectionLine.setPrefWidth(0);
            selectionLine.setLayoutY(119);
            selectionLine.setMouseTransparent(true);

            Pane amountPane = new Pane(vbox, line, selectionLine);
            amountPane.setMaxWidth(AMOUNT_BOX_WIDTH);

            slider = new Slider();
            slider.setMin(model.getSliderMin());
            slider.setMax(model.getSliderMax());

            minAmountLabel = new Label(Res.get("onboarding.amount.minLabel",
                    AmountFormatter.formatAmountWithCode(model.getMinAmount(), true)));
            minAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");

            maxAmountLabel = new Label(Res.get("onboarding.amount.maxLabel",
                    AmountFormatter.formatAmountWithCode(model.getMaxAmount(), true)));
            maxAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");

            VBox sliderBox = new VBox(2, slider, new HBox(minAmountLabel, Spacer.fillHBox(), maxAmountLabel));
            sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH);

            VBox.setMargin(amountPane, new Insets(0, 0, 28, 0));
            root.getChildren().addAll(descriptionLabel, amountPane, sliderBox);
        }

        @Override
        protected void onViewAttached() {
            UIScheduler.run(() -> {
                quoteAmount.requestFocus();
                baseAmountFocusPin = EasyBind.subscribe(baseAmount.focusedProperty(),
                        focus -> onInputTextFieldFocus(quoteAmount.focusedProperty(), focus));
                quoteAmountFocusPin = EasyBind.subscribe(quoteAmount.focusedProperty(),
                        focus -> onInputTextFieldFocus(baseAmount.focusedProperty(), focus));
            }).after(700);

            slider.valueProperty().bindBidirectional(model.getSliderValue());
            model.getSliderFocus().bind(slider.focusedProperty());
            descriptionLabel.textProperty().bind(model.description);

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
            slider.valueProperty().unbindBidirectional(model.getSliderValue());
            model.getSliderFocus().unbind();
            descriptionLabel.textProperty().unbind();

            Parent node = root;
            while (node.getParent() != null) {
                node.setOnMousePressed(null);
                node = node.getParent();
            }
        }

        private void onInputTextFieldFocus(ReadOnlyBooleanProperty other, boolean focus) {
            if (focus) {
                selectionLine.setPrefWidth(0);
                selectionLine.setOpacity(1);
                Transitions.animateWidth(selectionLine, AMOUNT_BOX_WIDTH);
            } else if (!other.get()) {
                // If switching between the 2 fields we want to avoid to get the fadeout called that's why
                // we do the check with !other.get()  
                Transitions.fadeOut(selectionLine, 200);
            }
        }
    }
}