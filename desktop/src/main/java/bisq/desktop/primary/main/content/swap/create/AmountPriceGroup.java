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

package bisq.desktop.primary.main.content.swap.create;

import bisq.common.monetary.Direction;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPriceService;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountPriceGroup {
    public static class AmountPriceController implements Controller {
        private final AmountPriceModel model;
        @Getter
        private final AmountPriceGroup.AmountPriceView view;
        private final ChangeListener<Monetary> baseCurrencyAmountListener, quoteCurrencyAmountListener;
        private final ChangeListener<Quote> fixPriceQuoteListener;

        public AmountPriceController(MarketPriceService marketPriceService,
                                     ObjectProperty<Market> selectedMarket,
                                     ObjectProperty<Direction> direction,
                                     ObjectProperty<Monetary> baseCurrencyAmount,
                                     ObjectProperty<Monetary> quoteCurrencyAmount,
                                     ObjectProperty<Quote> fixPriceQuote) {
            model = new AmountPriceModel(marketPriceService,
                    selectedMarket,
                    direction,
                    baseCurrencyAmount,
                    quoteCurrencyAmount,
                    fixPriceQuote);

            var base = new AmountInput.AmountController(selectedMarket,
                    direction,
                    true,
                    model.baseCurrencyAmount);
            var quote = new AmountInput.AmountController(selectedMarket,
                    direction,
                    false,
                    model.quoteCurrencyAmount);
            var price = new PriceInput.PriceController(marketPriceService,
                    selectedMarket,
                    model.fixPriceQuote,
                    Res.offerbook.get("createOffer.price.fix.description.buy"));

            view = new AmountPriceGroup.AmountPriceView(model, this, base.getView(), price.getView(), quote.getView());

            // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
            // apply the conversion before the other component has processed the market change event.
            // The order of the event notification is not deterministic. 
            baseCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runLater(this::setQuoteFromBase);
            quoteCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runLater(this::setBaseFromQuote);
            fixPriceQuoteListener = (observable, oldValue, newValue) -> UIThread.runLater(this::applyFixPrice);
        }

        @Override
        public void onViewAttached() {
            model.baseCurrencyAmount.addListener(baseCurrencyAmountListener);
            model.quoteCurrencyAmount.addListener(quoteCurrencyAmountListener);
            model.fixPriceQuote.addListener(fixPriceQuoteListener);
        }

        @Override
        public void onViewDetached() {
            model.baseCurrencyAmount.removeListener(baseCurrencyAmountListener);
            model.quoteCurrencyAmount.removeListener(quoteCurrencyAmountListener);
            model.fixPriceQuote.removeListener(fixPriceQuoteListener);
        }

        private void setQuoteFromBase() {
            Quote fixPrice = model.getFixPriceQuote().get();
            if (fixPrice == null) return;
            Monetary baseCurrencyAmount = model.baseCurrencyAmount.get();
            if (baseCurrencyAmount == null) return;
            if (fixPrice.getBaseMonetary().getClass() != baseCurrencyAmount.getClass()) return;
            model.quoteCurrencyAmount.set(fixPrice.toQuoteMonetary(baseCurrencyAmount));
        }

        private void setBaseFromQuote() {
            Quote fixPrice = model.getFixPriceQuote().get();
            if (fixPrice == null) return;
            Monetary quoteCurrencyAmount = model.quoteCurrencyAmount.get();
            if (quoteCurrencyAmount == null) return;
            if (fixPrice.getQuoteMonetary().getClass() != quoteCurrencyAmount.getClass()) return;
            model.baseCurrencyAmount.set(fixPrice.toBaseMonetary(quoteCurrencyAmount));
        }

        private void applyFixPrice() {
            if (model.baseCurrencyAmount.get() == null) {
                setBaseFromQuote();
            } else {
                setQuoteFromBase();
            }
        }
    }

    @Getter
    private static class AmountPriceModel implements Model {
        private final MarketPriceService marketPriceService;
        private final ObjectProperty<Market> selectedMarket;
        private final ObjectProperty<Direction> direction;
        private final ObjectProperty<Monetary> baseCurrencyAmount;
        private final ObjectProperty<Monetary> quoteCurrencyAmount;
        private final ObjectProperty<Quote> fixPriceQuote;

        public AmountPriceModel(MarketPriceService marketPriceService,
                                ObjectProperty<Market> selectedMarket,
                                ObjectProperty<Direction> direction,
                                ObjectProperty<Monetary> baseCurrencyAmount,
                                ObjectProperty<Monetary> quoteCurrencyAmount,
                                ObjectProperty<Quote> fixPriceQuote) {
            this.marketPriceService = marketPriceService;
            this.selectedMarket = selectedMarket;
            this.direction = direction;
            this.baseCurrencyAmount = baseCurrencyAmount;
            this.quoteCurrencyAmount = quoteCurrencyAmount;
            this.fixPriceQuote = fixPriceQuote;
        }
    }

    @Slf4j
    public static class AmountPriceView extends View<VBox, AmountPriceModel, AmountPriceGroup.AmountPriceController> {
        public AmountPriceView(AmountPriceModel model,
                               AmountPriceController controller,
                               AmountInput.MonetaryView baseView,
                               PriceInput.PriceView priceView,
                               AmountInput.MonetaryView quoteView) {
            super(new VBox(), model, controller);

            root.setSpacing(0);

            Label headline = new BisqLabel(Res.offerbook.get("createOffer.setAmountAndPrice"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            Label xLabel = new Label();
            xLabel.getStyleClass().add("opaque-icon-character");
            Text xIcon = Icons.getIconForLabel(MaterialDesignIcon.CLOSE, "2em", xLabel);
            xIcon.getStyleClass().add("opaque-icon");

            Label resultLabel = new Label("=");
            resultLabel.getStyleClass().add("opaque-icon-character");

            HBox hBox = new HBox();
            hBox.getChildren().addAll(headline, baseView.getRoot(), xLabel, priceView.getRoot(), resultLabel, quoteView.getRoot());

            root.getChildren().addAll(headline, hBox);
        }

        public void onViewAttached() {
        }

        public void onViewDetached() {
        }
    }
}