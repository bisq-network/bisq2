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

package bisq.desktop.primary.main.content.trade.components;

import bisq.common.monetary.Market;
import bisq.common.monetary.Quote;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.PriceValidator;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PriceInput {
    private final Controller controller;

    public PriceInput(ReadOnlyObjectProperty<Market> selectedMarket, MarketPriceService marketPriceService) {
        controller = new Controller(selectedMarket, marketPriceService);
    }

    public ReadOnlyObjectProperty<Quote> fixPriceProperty() {
        return controller.model.fixPrice;
    }

    public void setPrice(Quote price) {
        controller.model.fixPrice.set(price);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller, MarketPriceService.Listener {
        private final Model model;
        @Getter
        private final View view;
        private final PriceValidator validator = new PriceValidator();
        private final ChangeListener<Market> selectedMarketListener;

        private Controller(ReadOnlyObjectProperty<Market> selectedMarket,
                           MarketPriceService marketPriceService) {
            model = new Model(selectedMarket, marketPriceService);
            view = new View(model, this, validator);

            selectedMarketListener = (observable, oldValue, newValue) -> updateFromMarketPrice(newValue);
        }

        private void updateFromMarketPrice(Market newValue) {
            if (newValue != null) {
                model.marketString.set(newValue.toString());
                model.description.set(Res.get("createOffer.price.fix.description.buy", newValue.baseCurrencyCode()));
            }
            if (model.isCreateOffer) {
                model.fixPrice.set(null);
                setFixPriceFromMarketPrice();
            }
        }

        @Override
        public void onViewAttached() {
            if (model.isCreateOffer) {
                model.marketPriceService.addListener(this);
            }
            model.selectedMarket.addListener(selectedMarketListener);
            updateFromMarketPrice(model.selectedMarket.get());
        }

        @Override
        public void onViewDetached() {
            if (model.isCreateOffer) {
                model.marketPriceService.removeListener(this);
            }
            model.selectedMarket.removeListener(selectedMarketListener);
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(() -> {
                // We only set it initially
                if (model.fixPrice.get() != null) return;
                setFixPriceFromMarketPrice();
            });
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
        }

        // View events
        private void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        private void onFixPriceInput(String value) {
            if (value == null) return;
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

        private void setFixPriceFromMarketPrice() {
            if (model.selectedMarket.get() == null) return;
            MarketPrice marketPrice = model.marketPriceService.getMarketPriceByCurrencyMap().get(model.selectedMarket.get());
            if (marketPrice == null) return;
            model.fixPrice.set(marketPrice.quote());
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private ObjectProperty<Quote> fixPrice = new SimpleObjectProperty<>();
        private final ReadOnlyObjectProperty<Market> selectedMarket;
        private final MarketPriceService marketPriceService;
        private boolean hasFocus;
        private final StringProperty marketString = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private boolean isCreateOffer = true;

        private Model(ReadOnlyObjectProperty<Market> selectedMarket,
                      MarketPriceService marketPriceService) {
            this.selectedMarket = selectedMarket;
            this.marketPriceService = marketPriceService;
        }
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Quote> fixPriceListener;
        private final BisqLabel marketLabel;
        private final BisqLabel descriptionLabel;

        private View(Model model,
                     Controller controller,
                     PriceValidator validator) {
            super(new VBox(), model, controller);

            textInput = new BisqInputTextField(60);
            textInput.setPromptText(Res.get("createOffer.price.fix.prompt"));
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

        @Override
        public void onViewAttached() {
            if (model.isCreateOffer) {
                textInput.textProperty().addListener(textInputListener);
                textInput.focusedProperty().addListener(focusListener);
            } else {
                // editable/disable changes style. setMouseTransparent is just for prototyping now
                textInput.setMouseTransparent(true);
            }
            marketLabel.textProperty().bind(model.marketString);
            descriptionLabel.textProperty().bind(model.description);
            model.fixPrice.addListener(fixPriceListener);
            textInput.setText(model.fixPrice.get() == null ? "" : QuoteFormatter.format(model.fixPrice.get()));
        }

        @Override
        public void onViewDetached() {
            if (model.isCreateOffer) {
                textInput.textProperty().removeListener(textInputListener);
                textInput.focusedProperty().removeListener(focusListener);
            }
            marketLabel.textProperty().unbind();
            descriptionLabel.textProperty().unbind();
            model.fixPrice.removeListener(fixPriceListener);
        }
    }
}