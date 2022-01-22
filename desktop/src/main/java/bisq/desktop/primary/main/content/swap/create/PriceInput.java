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

import bisq.common.monetary.Market;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.PriceValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
public class PriceInput {
    public static class PriceController implements Controller, MarketPriceService.Listener {
        private final PriceModel model;
        @Getter
        private final PriceView view;
        private final PriceValidator validator = new PriceValidator();
        private final ChangeListener<Market> selectedMarketListener;

        public PriceController(MarketPriceService marketPriceService,
                               ObjectProperty<Market> selectedMarket,
                               ObjectProperty<Quote> fixPriceQuote,
                               String description) {
            model = new PriceModel(marketPriceService, selectedMarket, fixPriceQuote);
            view = new PriceView(model, this, validator, description);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    model.marketString.set(newValue.toString());
                }
                model.fixPrice.set(null);
                setFixPriceFromMarketPrice();
            };
        }

        // View events
        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        public void onFixPriceInput(String value) {
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.fixPrice.set(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.fixPrice.set(null);
                return;
            }
            if (model.selectedMarket.get() == null) return;
            model.fixPrice.set(PriceParser.parse(value, model.selectedMarket.get()));
        }

        public void onViewAttached() {
            model.marketPriceService.addListener(this);
            model.selectedMarket.addListener(selectedMarketListener);
        }

        public void onViewDetached() {
            model.marketPriceService.removeListener(this);
            model.selectedMarket.removeListener(selectedMarketListener);
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(() -> {
                if (model.fixPrice.get() != null) return;
                setFixPriceFromMarketPrice();
            });
        }

        private void setFixPriceFromMarketPrice() {
            if (model.selectedMarket.get() == null) return;
            MarketPrice marketPrice = model.marketPriceService.getMarketPriceByCurrencyMap().get(model.selectedMarket.get());
            if (marketPrice == null) return;
            model.fixPrice.set(marketPrice.quote());
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
        }
    }

    @Getter
    public static class PriceModel implements Model {
        private final MarketPriceService marketPriceService;
        private final ObjectProperty<Market> selectedMarket;
        private final ObjectProperty<Quote> fixPrice;
        public boolean hasFocus;
        public final StringProperty marketString = new SimpleStringProperty();

        public PriceModel(MarketPriceService marketPriceService,
                          ObjectProperty<Market> selectedMarket,
                          ObjectProperty<Quote> fixPrice) {
            this.marketPriceService = marketPriceService;
            this.selectedMarket = selectedMarket;
            this.fixPrice = fixPrice;
        }
    }

    public static class PriceView extends View<VBox, PriceModel, PriceController> {
        private final BisqInputTextField fixedPrice;
        private final ChangeListener<String> fixedPriceTextListener;
        private final ChangeListener<Boolean> fixedPriceFocusListener;
        private final ChangeListener<Quote> fixedPriceQuoteListener;
        private final BisqLabel market;

        public PriceView(PriceModel model,
                         PriceController controller,
                         PriceValidator validator,
                         String description) {
            super(new VBox(), model, controller);

            fixedPrice = new BisqInputTextField(60);
            fixedPrice.setPromptText(Res.offerbook.get("createOffer.price.fix.prompt"));
            fixedPrice.setMaxWidth(Double.MAX_VALUE);
            fixedPrice.setValidator(validator);

            market = new BisqLabel();
            market.setMinHeight(42);
            market.setFixWidth(100);
            market.setAlignment(Pos.CENTER);

            HBox hBox = new HBox();
            hBox.getStyleClass().add("input-with-border");
            HBox.setHgrow(fixedPrice, Priority.ALWAYS);
            hBox.getChildren().addAll(fixedPrice, market);

            BisqLabel descriptionLabel = new BisqLabel(description);
            descriptionLabel.setId("input-description-label");
            descriptionLabel.setPrefWidth(190);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(descriptionLabel, hBox);

            //  Listeners on view component events
            fixedPriceFocusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onFixPriceInput(fixedPrice.getText());
            };
            fixedPriceTextListener = (o, old, newValue) -> controller.onFixPriceInput(fixedPrice.getText());

            // Listeners on model change
            fixedPriceQuoteListener = (o, old, newValue) -> fixedPrice.setText(newValue == null ? "" : QuoteFormatter.format(newValue));
        }

        public void onViewAttached() {
            market.textProperty().bind(model.marketString);
            fixedPrice.textProperty().addListener(fixedPriceTextListener);
            fixedPrice.focusedProperty().addListener(fixedPriceFocusListener);
            model.fixPrice.addListener(fixedPriceQuoteListener);
        }

        public void onViewDetached() {
            market.textProperty().unbind();
            fixedPrice.textProperty().removeListener(fixedPriceTextListener);
            fixedPrice.focusedProperty().removeListener(fixedPriceFocusListener);
            model.fixPrice.removeListener(fixedPriceQuoteListener);
        }
    }
}