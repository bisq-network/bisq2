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

package bisq.desktop.primary.main.content.trade.components_poc;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.AmountInput;
import bisq.desktop.primary.overlay.bisq_easy.components.PriceInput;
import bisq.offer.Direction;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountPriceGroup {
    private final Controller controller;

    public AmountPriceGroup(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    public ReadOnlyObjectProperty<Monetary> baseSideAmountProperty() {
        return controller.model.baseSideAmount;
    }

    public ReadOnlyObjectProperty<Monetary> quoteSideAmountProperty() {
        return controller.model.quoteSideAmount;
    }

    public ReadOnlyObjectProperty<PriceQuote> quoteProperty() {
        return controller.model.priceQuote;
    }


    public void setIsTakeOffer() {
        controller.baseAmount.setIsTakeOffer();
        controller.quoteAmount.setIsTakeOffer();
        controller.price.setIsTakeOffer();
        controller.model.isCreateOffer = false;
    }

    public void setBaseSideAmount(Monetary amount) {
        controller.baseAmount.setAmount(amount);
    }

    public void setQuoteSideAmount(Monetary amount) {
        controller.quoteAmount.setAmount(amount);
    }

    public void setQuote(PriceQuote price) {
        controller.price.setQuote(price);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.baseAmount.setSelectedMarket(selectedMarket);
        controller.quoteAmount.setSelectedMarket(selectedMarket);
        controller.price.setMarket(selectedMarket);
    }

    public void setDirection(Direction direction) {
        controller.baseAmount.setDirection(direction);
        controller.quoteAmount.setDirection(direction);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChangeListener<Monetary> baseCurrencyAmountListener, quoteCurrencyAmountListener;
        private final ChangeListener<PriceQuote> quoteListener;
        private final AmountInput baseAmount;
        private final AmountInput quoteAmount;
        private final PriceInput price;

        private Controller(MarketPriceService marketPriceService) {
            baseAmount = new AmountInput(true);
            quoteAmount = new AmountInput(false);
            price = new PriceInput(marketPriceService);

            model = new Model(baseAmount.amountProperty(), quoteAmount.amountProperty(), price.getQuote());

            view = new View(model,
                    this,
                    baseAmount.getRoot(),
                    price.getRoot(),
                    quoteAmount.getRoot());

            // We delay with runLater to avoid that we get triggered at market change from the component's data changes and
            // apply the conversion before the other component has processed the market change event.
            // The order of the event notification is not deterministic. 
            baseCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setQuoteFromBase);
            quoteCurrencyAmountListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::setBaseFromQuote);
            quoteListener = (observable, oldValue, newValue) -> UIThread.runOnNextRenderFrame(this::applyQuote);
        }

        @Override
        public void onActivate() {
            if (model.isCreateOffer) {
                model.baseSideAmount.addListener(baseCurrencyAmountListener);
                model.quoteSideAmount.addListener(quoteCurrencyAmountListener);
                model.priceQuote.addListener(quoteListener);
            }
        }

        @Override
        public void onDeactivate() {
            if (model.isCreateOffer) {
                model.baseSideAmount.removeListener(baseCurrencyAmountListener);
                model.quoteSideAmount.removeListener(quoteCurrencyAmountListener);
                model.priceQuote.removeListener(quoteListener);
            }
        }

        private void setQuoteFromBase() {
            PriceQuote priceQuote = model.priceQuote.get();
            if (priceQuote == null) return;
            Monetary baseCurrencyAmount = model.baseSideAmount.get();
            if (baseCurrencyAmount == null) return;
            if (priceQuote.getBaseSideMonetary().getClass() != baseCurrencyAmount.getClass()) return;
            quoteAmount.setAmount(priceQuote.toQuoteSideMonetary(baseCurrencyAmount));
        }

        private void setBaseFromQuote() {
            PriceQuote priceQuote = model.priceQuote.get();
            if (priceQuote == null) return;
            Monetary quoteCurrencyAmount = model.quoteSideAmount.get();
            if (quoteCurrencyAmount == null) return;
            if (priceQuote.getQuoteSideMonetary().getClass() != quoteCurrencyAmount.getClass()) return;
            baseAmount.setAmount(priceQuote.toBaseSideMonetary(quoteCurrencyAmount));
        }

        private void applyQuote() {
            if (model.baseSideAmount == null) {
                setBaseFromQuote();
            } else {
                setQuoteFromBase();
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ReadOnlyObjectProperty<Monetary> baseSideAmount;
        private final ReadOnlyObjectProperty<Monetary> quoteSideAmount;
        private final ReadOnlyObjectProperty<PriceQuote> priceQuote;
        private boolean isCreateOffer = true;

        private Model(ReadOnlyObjectProperty<Monetary> baseSideAmount,
                      ReadOnlyObjectProperty<Monetary> quoteSideAmount,
                      ReadOnlyObjectProperty<PriceQuote> priceQuote) {
            this.baseSideAmount = baseSideAmount;
            this.quoteSideAmount = quoteSideAmount;
            this.priceQuote = priceQuote;
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {

        private View(Model model,
                     Controller controller,
                     Pane baseAmount,
                     Pane price,
                     Pane quoteAmount) {
            super(new HBox(), model, controller);

            ImageView crossIcon = ImageUtil.getImageViewById("cross");
            HBox.setMargin(crossIcon, new Insets(22, 16, 0, 16));

            ImageView equalsIcon = ImageUtil.getImageViewById("equals");
            HBox.setMargin(equalsIcon, new Insets(22, 16, 0, 16));

            root.getChildren().addAll(baseAmount, crossIcon, price, equalsIcon, quoteAmount);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }
}