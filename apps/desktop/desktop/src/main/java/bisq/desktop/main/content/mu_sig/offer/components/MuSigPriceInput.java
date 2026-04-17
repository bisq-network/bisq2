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

package bisq.desktop.main.content.mu_sig.offer.components;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import bisq.offer.mu_sig.draft.OfferDraftWorkflow;
import bisq.presentation.formatters.PriceFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigPriceInput {
    private final Controller controller;

    public MuSigPriceInput(MarketPriceService marketPriceService, OfferDraftWorkflow<?> offerDraftWorkflow) {
        controller = new Controller(marketPriceService, offerDraftWorkflow);
    }

    public ReadOnlyObjectProperty<PriceQuote> priceQuoteProperty() {
        return controller.model.priceQuote;
    }

    public ReadOnlyStringProperty getPriceString() {
        return controller.model.priceString;
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

    public void setPriceString(String priceString) {
        controller.model.priceString.set(priceString);
    }

    public void setPercentage(String percentage) {
        controller.setPercentage(percentage);
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

    public void activate(boolean shouldRequestFocus) {
        controller.view.textInput.setEditable(true);
        if (shouldRequestFocus) {
            controller.view.textInput.requestFocusWithCursor();
        }
    }

    public void deactivate() {
        controller.view.textInput.deselect();
        controller.view.textInput.setEditable(false);
        // Reset validation
        controller.model.doResetValidation.set(true);
        controller.model.doResetValidation.set(false);
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
        private final OfferDraftWorkflow<?> offerDraftWorkflow;
        private final Optional<CreateOfferDraftWorkflow> createOfferDraftWorkflow;
        private final MarketPriceService marketPriceService;
        private final NumberValidator validator = new NumberValidator(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
        private Pin marketPricePin;
        private final Set<Subscription> subscriptions = new HashSet<>();
        private final Set<Pin> pins = new HashSet<>();

        private Controller(MarketPriceService marketPriceService, OfferDraftWorkflow<?> offerDraftWorkflow) {
            this.marketPriceService = marketPriceService;
            this.offerDraftWorkflow = offerDraftWorkflow;
            if (offerDraftWorkflow instanceof CreateOfferDraftWorkflow workflow) {
                createOfferDraftWorkflow = Optional.of(workflow);
            } else {
                createOfferDraftWorkflow = Optional.empty();
            }
            model = new Model();
            view = new View(model, this, validator);
        }

        public void setQuote(PriceQuote priceQuote) {
            model.priceString.set(priceQuote == null ? "" : PriceFormatter.format(priceQuote));
            //  model.priceQuote.set(priceQuote);
            createOfferDraftWorkflow.ifPresent(workflow -> workflow.setPriceQuote(priceQuote));
        }

        public void setPercentage(String percentage) {
            model.percentagePriceString.set(percentage);
        }

        private void updateFromMarketPrice() {
            Market market = offerDraftWorkflow.getMarket();
            if (market != null && model.description.get() == null) {
                model.description.set(Res.get("component.priceInput.description", market.getMarketCodes()));
                model.textInputCurrencyCodes.set(market.getMarketCodes());
            }
            if (model.isEditable) {
                setQuoteFromMarketPrice();
            }
        }

        @Override
        public void onActivate() {
            model.reset();
            updateFromMarketPrice();
            model.isPriceValid.set(true);
            updateFromMarketPrice();

            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() -> {
                // We only set it initially
               /* if (model.priceQuote.get() != null) {
                    return;
                }*/
                UIThread.run(this::setQuoteFromMarketPrice);
            });

            subscriptions.add(EasyBind.subscribe(model.priceString, this::onPriceInput));
            // quotePin = EasyBind.subscribe(model.priceQuote, this::onQuoteChanged);

            createOfferDraftWorkflow.map(workflow ->
                            workflow.priceQuoteObservable().addObserver(this::onQuoteChanged))
                    .ifPresent(pins::add);
        }

        @Override
        public void onDeactivate() {
            marketPricePin.unbind();
            subscriptions.forEach(Subscription::unsubscribe);
            subscriptions.clear();
            pins.forEach(Pin::unbind);
            pins.clear();
            model.description.set(null);
        }

        private void onPriceInput(String price) {
            if (model.isFocused || price == null || price.isEmpty()) {
                return;
            }

            boolean isValid = MathUtils.isValidDouble(price);
            model.isPriceValid.set(isValid);
            if (!isValid) {
                return;
            }

            try {
                PriceQuote priceQuote = PriceParser.parse(price, offerDraftWorkflow.getMarket());
                checkArgument(priceQuote.getValue() > 0);
                // model.priceQuote.set(priceQuote);
                createOfferDraftWorkflow.ifPresent(workflow -> workflow.setPriceQuote(priceQuote));
            } catch (Throwable ignore) {
                createOfferDraftWorkflow.map(CreateOfferDraftWorkflow::getPriceQuote)
                        .ifPresent(this::onQuoteChanged);
                // onQuoteChanged(model.priceQuote.get());
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
            marketPriceService.findMarketPrice(offerDraftWorkflow.getMarket())
                    .ifPresent(marketPrice -> {
                        PriceQuote priceQuote = marketPrice.getPriceQuote();
                        // model.priceQuote.set(priceQuote);
                        createOfferDraftWorkflow.ifPresent(workflow -> workflow.setPriceQuote(priceQuote));
                    });
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        //todo remove once old code is removed
        private final ObjectProperty<PriceQuote> priceQuote = new SimpleObjectProperty<>();

        private final StringProperty priceString = new SimpleStringProperty();

        private boolean isFocused;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty textInputCurrencyCodes = new SimpleStringProperty();
        private final StringProperty percentagePriceString = new SimpleStringProperty();
        private boolean isEditable = true;
        private final BooleanProperty isPriceValid = new SimpleBooleanProperty();
        private final BooleanProperty doResetValidation = new SimpleBooleanProperty();

        private Model() {
        }

        public void reset() {
            // priceQuote.set(null);
            priceString.set(null);
            isFocused = false;
            description.set(null);
            textInputCurrencyCodes.set(null);
            percentagePriceString.set(null);
            isEditable = true;
        }
    }

    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int WIDTH = 250;

        private final MuSigPriceInputBox textInput;
        private Subscription focusedPin, doResetValidationPin;

        private View(Model model, Controller controller, NumberValidator validator) {
            super(new VBox(), model, controller);

            textInput = new MuSigPriceInputBox(model.description.get(), Res.get("component.priceInput.prompt"),
                    BisqEasyViewUtils.POSITIVE_NUMERIC_WITH_DECIMAL_REGEX);
            textInput.setPrefWidth(WIDTH);
            textInput.setValidator(validator);
            textInput.conversionPriceSymbolTextProperty().set("%");

            root.getChildren().add(textInput);
        }

        @Override
        protected void onViewAttached() {
            textInput.descriptionProperty().bind(model.description);
            textInput.textInputSymbolTextProperty().bind(model.textInputCurrencyCodes);
            textInput.conversionPriceTextProperty().bind(model.percentagePriceString);
            textInput.textProperty().bindBidirectional(model.priceString);
            textInput.initialize();
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
            textInput.textInputSymbolTextProperty().unbind();
            textInput.conversionPriceTextProperty().unbind();
            textInput.textProperty().unbindBidirectional(model.priceString);
            textInput.resetValidation();
            textInput.dispose();
            focusedPin.unsubscribe();
            doResetValidationPin.unsubscribe();
        }
    }
}
