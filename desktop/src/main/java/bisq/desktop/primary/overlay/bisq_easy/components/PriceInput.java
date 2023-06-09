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

package bisq.desktop.primary.overlay.bisq_easy.components;

import bisq.common.currency.Market;
import bisq.common.monetary.Quote;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.PriceValidator;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class PriceInput {
    private final Controller controller;

    public PriceInput(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    public void setMarket(Market market) {
        controller.setMarket(market);
    }

    public ReadOnlyObjectProperty<Quote> getQuote() {
        return controller.model.quote;
    }

    public ReadOnlyStringProperty descriptionProperty() {
        return controller.model.description;
    }

    public void setDescription(String description) {
        controller.model.description.set(description);
    }

    public void setQuote(Quote price) {
        controller.model.quote.set(price);
    }

    public void setIsTakeOffer() {
        controller.model.isEditable = false;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reset() {
        controller.model.reset();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final PriceValidator validator = new PriceValidator();
        private final MarketPriceService marketPriceService;
        private Pin marketPriceUpdateFlagPin;
        private Subscription quotePin, pricePin;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            model = new Model();
            view = new View(model, this, validator);
        }

        public void setMarket(Market market) {
            model.market = market;
            updateFromMarketPrice();
        }

        private void updateFromMarketPrice() {
            if (model.market != null && model.description.get() == null) {
                model.description.set(Res.get("priceInput.description", model.market.getMarketCodes()));
            }
            if (model.isEditable && model.quote.get() == null) {
                setQuoteFromMarketPrice();
            }
        }

        @Override
        public void onActivate() {
            updateFromMarketPrice();

            marketPriceUpdateFlagPin = marketPriceService.getMarketPriceUpdateFlag().addObserver(__ -> {
                UIThread.run(() -> {
                    // We only set it initially
                    if (model.quote.get() != null) return;
                    setQuoteFromMarketPrice();
                });
            });

            pricePin = EasyBind.subscribe(model.priceString, this::onPriceInput);
            quotePin = EasyBind.subscribe(model.quote, this::onQuoteChanged);
        }

        @Override
        public void onDeactivate() {
            marketPriceUpdateFlagPin.unbind();
            pricePin.unsubscribe();
            quotePin.unsubscribe();
            model.description.set(null);
        }

        private void onPriceInput(String price) {
            if (model.isFocused) {
                return;
            }
            if (price == null ||
                    price.isEmpty() ||
                    model.market == null ||
                    !validator.validate(price).isValid) {
                onQuoteChanged(model.quote.get());
                return;
            }
            try {
                Quote quote = PriceParser.parse(price, model.market);
                checkArgument(quote.getValue() > 0);
                model.quote.set(quote);
            } catch (Throwable ignore) {
                onQuoteChanged(model.quote.get());
            }
        }

        private void onQuoteChanged(Quote quote) {
            if (model.isFocused) {
                return;
            }
            model.priceString.set(quote == null ? "" : QuoteFormatter.format(quote));
        }

        private void onFocusedChanged(boolean isFocused) {
            model.isFocused = isFocused;
            if (!isFocused) {
                onPriceInput(model.priceString.get());
            }
        }

        private void setQuoteFromMarketPrice() {
            if (model.market == null) return;
            marketPriceService.findMarketPrice(model.market)
                    .ifPresent(marketPrice -> model.quote.set(marketPrice.getQuote()));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Quote> quote = new SimpleObjectProperty<>();
        private final StringProperty priceString = new SimpleStringProperty();

        private Market market;
        private boolean isFocused;
        private final StringProperty description = new SimpleStringProperty();
        private boolean isEditable = true;

        private Model() {
        }

        public void reset() {
            quote.set(null);
            market = null;
            isFocused = false;
            description.set(null);
            isEditable = true;
        }
    }

    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int WIDTH = 250;
        private final MaterialTextField textInput;
        private Subscription focusedPin;

        private View(Model model, Controller controller, PriceValidator validator) {
            super(new VBox(), model, controller);

            textInput = new MaterialTextField(model.description.get(), Res.get("priceInput.prompt"));
            textInput.setPrefWidth(WIDTH);
            textInput.setValidator(validator);

            root.getChildren().add(textInput);
        }

        @Override
        protected void onViewAttached() {
            textInput.descriptionProperty().bind(model.description);
            textInput.textProperty().bindBidirectional(model.priceString);
            focusedPin = EasyBind.subscribe(textInput.inputTextFieldFocusedProperty(), controller::onFocusedChanged);
            textInput.setMouseTransparent(!model.isEditable);
        }

        @Override
        protected void onViewDetached() {
            textInput.descriptionProperty().unbind();
            textInput.textProperty().unbindBidirectional(model.priceString);
            focusedPin.unsubscribe();
        }
    }
}