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

package bisq.desktop.primary.onboardingOld.onboardNewbie;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AmountInput;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
class BtcFiatAmountGroup {
    private final Controller controller;

    BtcFiatAmountGroup(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    ReadOnlyObjectProperty<Monetary> baseSideAmountProperty() {
        return controller.model.baseSideAmount;
    }

    Pane getRoot() {
        return controller.view.getRoot();
    }

    void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    void setDirection(Direction direction) {
        controller.baseAmount.setDirection(direction);
        controller.quoteAmount.setDirection(direction);
    }

    private static class Controller implements bisq.desktop.common.view.Controller, MarketPriceService.Listener {
        private final Model model;
        @Getter
        private final View view;
        private final ChangeListener<Monetary> baseCurrencyAmountListener, quoteCurrencyAmountListener;
        private final AmountInput baseAmount;
        private final AmountInput quoteAmount;
        private final MarketPriceService marketPriceService;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            baseAmount = new AmountInput(true);
            quoteAmount = new AmountInput(false);

            model = new Model(baseAmount.amountProperty(), quoteAmount.amountProperty());

            view = new View(model,
                    this,
                    baseAmount.getRoot(),
                    quoteAmount.getRoot());

            // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
            // apply the conversion before the other component has processed the market change event.
            // The order of the event notification is not deterministic. 
            baseCurrencyAmountListener = (observable, oldValue, newValue) -> {
                UIThread.runOnNextRenderFrame(this::setQuoteFromBase);
            };
            quoteCurrencyAmountListener = (observable, oldValue, newValue) -> {
                UIThread.runOnNextRenderFrame(this::setBaseFromQuote);
            };
        }

        private void setSelectedMarket(Market selectedMarket) {
            baseAmount.setSelectedMarket(selectedMarket);
            quoteAmount.setSelectedMarket(selectedMarket);
            model.selectedMarket = selectedMarket;
            updateMarketPriceString();
        }

        @Override
        public void onActivate() {
            model.baseSideAmount.addListener(baseCurrencyAmountListener);
            model.quoteSideAmount.addListener(quoteCurrencyAmountListener);
            marketPriceService.addListener(this);
            updateMarketPriceString();
        }

        @Override
        public void onDeactivate() {
            model.baseSideAmount.removeListener(baseCurrencyAmountListener);
            model.quoteSideAmount.removeListener(quoteCurrencyAmountListener);
            marketPriceService.removeListener(this);
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(this::updateMarketPriceString);
        }

        @Override
        public void onMarketSelected(Market selectedMarket) {
            UIThread.run(() -> {
                model.selectedMarket = selectedMarket;
                updateMarketPriceString();
            });
        }

        private void setQuoteFromBase() {
            Quote fixPrice = model.fixPrice;
            if (fixPrice == null) return;
            Monetary baseCurrencyAmount = model.baseSideAmount.get();
            if (baseCurrencyAmount == null) return;
            if (fixPrice.getBaseMonetary().getClass() != baseCurrencyAmount.getClass()) return;
            quoteAmount.setAmount(fixPrice.toQuoteMonetary(baseCurrencyAmount));
        }

        private void setBaseFromQuote() {
            Quote fixPrice = model.fixPrice;
            if (fixPrice == null) return;
            Monetary quoteCurrencyAmount = model.quoteSideAmount.get();
            if (quoteCurrencyAmount == null) return;
            if (fixPrice.getQuoteMonetary().getClass() != quoteCurrencyAmount.getClass()) return;
            baseAmount.setAmount(fixPrice.toBaseMonetary(quoteCurrencyAmount));
        }

        private void applyFixPrice() {
            if (model.baseSideAmount == null) {
                setBaseFromQuote();
            } else {
                setQuoteFromBase();
            }
        }

        private void updateMarketPriceString() {
            if (model.selectedMarket == null) return;
            MarketPrice marketPrice = marketPriceService.getMarketPriceByCurrencyMap().get(model.selectedMarket);
            if (marketPrice == null) return;
            Quote marketPriceQuote = marketPrice.quote();
            model.fixPrice = marketPriceQuote;
            model.marketPriceString.set(marketPriceQuote == null ? "" : "@ " + QuoteFormatter.formatWithQuoteCode(marketPriceQuote));
            applyFixPrice();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ReadOnlyObjectProperty<Monetary> baseSideAmount;
        private final ReadOnlyObjectProperty<Monetary> quoteSideAmount;
        private Quote fixPrice;

        private final StringProperty marketPriceString = new SimpleStringProperty();
        private Market selectedMarket;

        private Model(ReadOnlyObjectProperty<Monetary> baseSideAmount, ReadOnlyObjectProperty<Monetary> quoteSideAmount) {
            this.baseSideAmount = baseSideAmount;
            this.quoteSideAmount = quoteSideAmount;
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label maxAmountLabel, marketPriceLabel;
        private final Pane baseAmount, quoteAmount;

        private View(Model model,
                     Controller controller,
                     Pane baseAmount,
                     Pane quoteAmount) {
            super(new HBox(), model, controller);

            this.baseAmount = baseAmount;
            this.quoteAmount = quoteAmount;

            VBox baseAmountBox = new VBox();
            baseAmountBox.setSpacing(3);
            maxAmountLabel = new Label(Res.get("satoshisquareapp.createOffer.maxAmount"));
            maxAmountLabel.setAlignment(Pos.CENTER_RIGHT);
            maxAmountLabel.setPadding(new Insets(0, 5, 0, 0));
            maxAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");
            baseAmountBox.getChildren().addAll(baseAmount, maxAmountLabel);

            VBox quoteAmountBox = new VBox();
            quoteAmountBox.setSpacing(3);
            marketPriceLabel = new Label();
            marketPriceLabel.setAlignment(Pos.CENTER_RIGHT);
            marketPriceLabel.setPadding(new Insets(0, 5, 0, 0));
            marketPriceLabel.getStyleClass().add("bisq-small-light-label-dimmed");
            quoteAmountBox.getChildren().addAll(quoteAmount, marketPriceLabel);

            root.getChildren().addAll(baseAmountBox, Spacer.width(20), quoteAmountBox);
        }

        @Override
        protected void onViewAttached() {
            maxAmountLabel.prefWidthProperty().bind(baseAmount.widthProperty());
            marketPriceLabel.prefWidthProperty().bind(quoteAmount.widthProperty());
            marketPriceLabel.textProperty().bind(model.marketPriceString);
        }

        @Override
        protected void onViewDetached() {
            maxAmountLabel.prefWidthProperty().unbind();
            marketPriceLabel.prefWidthProperty().unbind();
            marketPriceLabel.textProperty().unbind();
        }
    }
}