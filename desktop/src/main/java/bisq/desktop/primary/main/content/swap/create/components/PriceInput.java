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

package bisq.desktop.primary.main.content.swap.create.components;

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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PriceInput {
    public static class PriceController implements Controller, MarketPriceService.Listener {
        private final PriceModel model;
        @Getter
        private final PriceView view;
        private final PriceValidator validator = new PriceValidator();
        private final ChangeListener<Market> selectedMarketListener;

        public PriceController(OfferPreparationModel offerPreparationModel,
                               MarketPriceService marketPriceService) {
            model = new PriceModel(offerPreparationModel, marketPriceService);
            view = new PriceView(model, this, validator);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    model.marketString.set(newValue.toString());
                    model.description.set(Res.offerbook.get("createOffer.price.fix.description.buy", newValue.baseCurrencyCode()));
                }
                model.setFixPrice(null);
                setFixPriceFromMarketPrice();
            };
        }

        // View events
        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        public void onFixPriceInput(String value) {
            if (value == null) return;
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.setFixPrice(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.setFixPrice(null);
                return;
            }
            if (model.getSelectedMarket() == null) return;
            model.setFixPrice(PriceParser.parse(value, model.getSelectedMarket()));
        }

        public void onViewAttached() {
            model.marketPriceService.addListener(this);
            model.selectedMarketProperty().addListener(selectedMarketListener);
            if (model.getFixPrice() != null) return;
            setFixPriceFromMarketPrice();
        }

        public void onViewDetached() {
            model.marketPriceService.removeListener(this);
            model.selectedMarketProperty().removeListener(selectedMarketListener);
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(() -> {
                // We only set it initially
                if (model.getFixPrice() != null) return;
                setFixPriceFromMarketPrice();
            });
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
        }

        private void setFixPriceFromMarketPrice() {
            if (model.getSelectedMarket() == null) return;
            MarketPrice marketPrice = model.marketPriceService.getMarketPriceByCurrencyMap().get(model.getSelectedMarket());
            if (marketPrice == null) return;
            model.setFixPrice(marketPrice.quote());
        }
    }

    private static class PriceModel implements Model {
        @Delegate
        private final OfferPreparationModel offerPreparationModel;
        private final MarketPriceService marketPriceService;
        public boolean hasFocus;
        public final StringProperty marketString = new SimpleStringProperty();
        public final StringProperty description = new SimpleStringProperty();

        public PriceModel(OfferPreparationModel offerPreparationModel, MarketPriceService marketPriceService) {
            this.offerPreparationModel = offerPreparationModel;
            this.marketPriceService = marketPriceService;
        }
    }

    public static class PriceView extends View<VBox, PriceModel, PriceController> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Quote> fixPriceListener;
        private final BisqLabel marketLabel;
        private final BisqLabel descriptionLabel;

        public PriceView(PriceModel model,
                         PriceController controller,
                         PriceValidator validator) {
            super(new VBox(), model, controller);

            textInput = new BisqInputTextField(60);
            textInput.setPromptText(Res.offerbook.get("createOffer.price.fix.prompt"));
            textInput.setMaxWidth(Double.MAX_VALUE);
            textInput.setValidator(validator);

            marketLabel = new BisqLabel();
            marketLabel.setMinHeight(42);
            marketLabel.setFixWidth(100);
            marketLabel.setAlignment(Pos.CENTER);

            HBox hBox = new HBox();
            hBox.getStyleClass().add("input-with-border");
            HBox.setHgrow(textInput, Priority.ALWAYS);
            hBox.getChildren().addAll(textInput, marketLabel);

            descriptionLabel = new BisqLabel();
            descriptionLabel.setId("input-description-label");
            descriptionLabel.setPrefWidth(190);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(descriptionLabel, hBox);

            //  Listeners on view component events
            focusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onFixPriceInput(textInput.getText());
            };
            textInputListener = (o, old, newValue) -> controller.onFixPriceInput(textInput.getText());

            // Listeners on model change
            fixPriceListener = (o, old, newValue) -> textInput.setText(newValue == null ? "" : QuoteFormatter.format(newValue));
        }

        public void onViewAttached() {
            marketLabel.textProperty().bind(model.marketString);
            descriptionLabel.textProperty().bind(model.description);
            textInput.textProperty().addListener(textInputListener);
            textInput.focusedProperty().addListener(focusListener);
            model.fixPriceProperty().addListener(fixPriceListener);
        }

        public void onViewDetached() {
            marketLabel.textProperty().unbind();
            descriptionLabel.textProperty().unbind();
            textInput.textProperty().removeListener(textInputListener);
            textInput.focusedProperty().removeListener(focusListener);
            model.fixPriceProperty().removeListener(fixPriceListener);
        }
    }
}