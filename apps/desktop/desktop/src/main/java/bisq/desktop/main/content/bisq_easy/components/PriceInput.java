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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.i18n.Res;
import bisq.presentation.formatters.PriceFormatter;
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

    public ReadOnlyObjectProperty<PriceQuote> getQuote() {
        return controller.model.priceQuote;
    }

    public ReadOnlyStringProperty descriptionProperty() {
        return controller.model.description;
    }

    public void setDescription(String description) {
        controller.model.description.set(description);
    }

    public void setQuote(PriceQuote priceQuote) {
        controller.setQuote(priceQuote);
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

    public void requestFocus() {
        controller.view.textInput.requestFocus();
    }

    public void deselect() {
        controller.view.textInput.deselect();
    }

    public void resetValidation() {
        controller.model.doResetValidation.set(true);
        controller.model.doResetValidation.set(false);
    }

    public void setEditable(boolean value) {
        controller.view.textInput.setEditable(value);
    }

    public ReadOnlyBooleanProperty isPriceValid() {
        return controller.model.isPriceValid;
    }

    public String getErrorMessage() {
        return controller.validator.getMessage();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final NumberValidator validator = new NumberValidator(Res.get("bisqEasy.price.warn.invalidPrice.numberFormatException"));
        private final MarketPriceService marketPriceService;
        private Pin marketPricePin;
        private Subscription quotePin, pricePin;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            model = new Model();
            view = new View(model, this, validator);
        }

        public void setMarket(Market market) {
            model.reset();
            model.market = market;
            updateFromMarketPrice();
        }

        public void setQuote(PriceQuote priceQuote) {
            model.priceString.set(priceQuote == null ? "" : PriceFormatter.format(priceQuote));
            model.priceQuote.set(priceQuote);
        }

        private void updateFromMarketPrice() {
            if (model.market != null && model.description.get() == null) {
                model.description.set(Res.get("component.priceInput.description", model.market.getMarketCodes()));
            }
            if (model.isEditable) {
                setQuoteFromMarketPrice();
            }
        }

        @Override
        public void onActivate() {
            model.isPriceValid.set(true);
            updateFromMarketPrice();

            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() -> {
                // We only set it initially
                if (model.priceQuote.get() != null) return;
                UIThread.run(this::setQuoteFromMarketPrice);
            });

            pricePin = EasyBind.subscribe(model.priceString, this::onPriceInput);
            quotePin = EasyBind.subscribe(model.priceQuote, this::onQuoteChanged);
        }

        @Override
        public void onDeactivate() {
            marketPricePin.unbind();
            pricePin.unsubscribe();
            quotePin.unsubscribe();
            model.description.set(null);
        }

        private void onPriceInput(String price) {
            if (model.isFocused || price == null || price.isEmpty() || model.market == null) {
                return;
            }

            boolean isValid = MathUtils.isValidDouble(price);
            model.isPriceValid.set(isValid);
            if (!isValid) {
                return;
            }

            try {
                PriceQuote priceQuote = PriceParser.parse(price, model.market);
                checkArgument(priceQuote.getValue() > 0);
                model.priceQuote.set(priceQuote);
            } catch (Throwable ignore) {
                onQuoteChanged(model.priceQuote.get());
            }
        }

        private void onQuoteChanged(PriceQuote priceQuote) {
            if (model.isFocused) {
                return;
            }
            model.priceString.set(priceQuote == null ? "" : PriceFormatter.format(priceQuote));
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
                    .ifPresent(marketPrice -> model.priceQuote.set(marketPrice.getPriceQuote()));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();
        private final StringProperty priceString = new SimpleStringProperty();

        private Market market;
        private boolean isFocused;
        private final StringProperty description = new SimpleStringProperty();
        private boolean isEditable = true;
        private final BooleanProperty isPriceValid = new SimpleBooleanProperty();
        private final BooleanProperty doResetValidation = new SimpleBooleanProperty();

        private Model() {
        }

        public void reset() {
            priceQuote.set(null);
            priceString.set(null);
            market = null;
            isFocused = false;
            description.set(null);
            isEditable = true;
        }
    }

    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int WIDTH = 250;
        private final MaterialTextField textInput;
        private Subscription focusedPin, doResetValidationPin;

        private View(Model model, Controller controller, NumberValidator validator) {
            super(new VBox(), model, controller);

            textInput = new MaterialTextField(model.description.get(), Res.get("component.priceInput.prompt"));
            textInput.setPrefWidth(WIDTH);
            textInput.setValidator(validator);

            root.getChildren().add(textInput);
        }

        @Override
        protected void onViewAttached() {
            textInput.descriptionProperty().bind(model.description);
            textInput.textProperty().bindBidirectional(model.priceString);
            focusedPin = EasyBind.subscribe(textInput.textInputFocusedProperty(), controller::onFocusedChanged);
            doResetValidationPin = EasyBind.subscribe(model.doResetValidation, doResetValidation -> {
                if (doResetValidation != null && doResetValidation) {
                    textInput.resetValidation();
                }
            });
            textInput.setMouseTransparent(!model.isEditable);
        }

        @Override
        protected void onViewDetached() {
            textInput.descriptionProperty().unbind();
            textInput.textProperty().unbindBidirectional(model.priceString);
            textInput.resetValidation();
            focusedPin.unsubscribe();
            doResetValidationPin.unsubscribe();
        }
    }
}