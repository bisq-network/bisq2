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

package bisq.desktop.primary.main.content.social.onboarding.onboardNewbie;

import bisq.common.monetary.Market;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqLabel;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MarketPriceDisplay {
    private final Controller controller;

    public MarketPriceDisplay(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller, MarketPriceService.Listener {
        private final Model model;
        @Getter
        private final View view;

        private Controller(MarketPriceService marketPriceService) {
            model = new Model(marketPriceService);
            view = new View(model, this);
        }

        public void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            updateMarketPriceString();
        }

        @Override
        public void onActivate() {
            model.marketPriceService.addListener(this);
            updateMarketPriceString();
        }

        @Override
        public void onDeactivate() {
            model.marketPriceService.removeListener(this);
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

        private void updateMarketPriceString() {
            if (model.selectedMarket == null) return;
            MarketPrice marketPrice = model.marketPriceService.getMarketPriceByCurrencyMap().get(model.selectedMarket);
            if (marketPrice == null) return;
            Quote marketPriceQuote = marketPrice.quote();
            model.marketPriceString.set(marketPriceQuote == null ? "" : QuoteFormatter.formatWithQuoteCode(marketPriceQuote));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final MarketPriceService marketPriceService;
        private final StringProperty marketPriceString = new SimpleStringProperty();
        private Market selectedMarket;

        private Model(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
        }
    }

    //todo if no more fields gets added we can use BisqLabel as root
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqLabel marketPriceLabel;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            marketPriceLabel = new BisqLabel();
            root.getChildren().addAll(marketPriceLabel);
        }

        @Override
        protected void onViewAttached() {
            marketPriceLabel.textProperty().bind(model.marketPriceString);
        }

        @Override
        protected void onViewDetached() {
            marketPriceLabel.textProperty().unbind();
        }
    }
}