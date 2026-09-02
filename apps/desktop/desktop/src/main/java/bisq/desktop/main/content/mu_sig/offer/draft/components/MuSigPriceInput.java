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

package bisq.desktop.main.content.mu_sig.offer.draft.components;

import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import bisq.offer.mu_sig.use_case.DraftOfferUseCase;
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
import javafx.scene.control.TextInputControl;
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

    public MuSigPriceInput(DraftOfferUseCase draftOfferService) {
        controller = new Controller(draftOfferService);
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
        // Display only: the component projects domain state, it never writes it.
        controller.renderQuote(priceQuote);
    }

    public void setPercentage(String percentage) {
        controller.setPercentage(percentage);
    }

    public TextInputControl getTextInputControl() {
        return controller.view.textInput.getTextInputControl();
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reset() {
        controller.model.reset();
    }

    public void activate(boolean shouldRequestFocus) {
        // Activation starts from the authoritative domain state: an aborted invalid edit from a
        // previous activation must not survive, and the quote observer does not re-fire when the
        // stored quote is unchanged.
        controller.renderAuthoritativeQuote();
        controller.view.textInput.setEditable(true);
        if (shouldRequestFocus) {
            controller.view.textInput.requestFocusWithCursor();
        }
    }

    public void deactivate() {
        controller.view.textInput.deselect();
        controller.view.textInput.setEditable(false);
        // Reset validation; an inactive field is not invalid and must not block navigation.
        controller.model.doResetValidation.set(true);
        controller.model.doResetValidation.set(false);
        controller.model.isPriceValid.set(true);
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
        private final DraftOfferUseCase draftOfferUseCase;
        private final Optional<PriceSelection> priceUseCase;
        private final NumberValidator validator = new NumberValidator(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
        private final Set<Subscription> subscriptions = new HashSet<>();
        private boolean applyingDomainValue;
        private final Set<Pin> pins = new HashSet<>();

        private Controller(DraftOfferUseCase draftOfferUseCase) {
            this.draftOfferUseCase = draftOfferUseCase;
            if (draftOfferUseCase instanceof CreateOfferUseCase useCase) {
                priceUseCase = Optional.of(useCase.getPriceSelection());
            } else {
                priceUseCase = Optional.empty();
            }
            model = new Model();
            view = new View(model, this, validator);
        }

        private void renderQuote(PriceQuote priceQuote) {
            if (model.isFocused) {
                return;
            }
            // Guarded so the priceString subscription cannot mistake this projection for input.
            applyingDomainValue = true;
            try {
                model.priceString.set(priceQuote == null ? "" : PriceFormatter.format(priceQuote));
            } finally {
                applyingDomainValue = false;
            }
        }

        public void setPercentage(String percentage) {
            model.percentagePriceString.set(percentage);
        }

        private void updateDescription() {
            Market market = draftOfferUseCase.getMarket();
            if (market != null && model.description.get() == null) {
                model.description.set(Res.get("component.priceInput.description", market.getMarketCodes()));
                model.textInputCurrencyCodes.set(market.getMarketCodes());
            }
        }

        @Override
        public void onActivate() {
            model.reset();
            updateDescription();
            model.isPriceValid.set(true);

            subscriptions.add(EasyBind.subscribe(model.priceString, this::onPriceInput));

            // Domain quote updates can originate on the market data thread; marshal and re-read
            // the current value so a queued callback never renders a stale captured quote.
            priceUseCase.map(useCase ->
                            useCase.priceQuoteObservable().addObserver(quote ->
                                    UIThread.run(() -> renderQuote(useCase.getPriceQuote()))))
                    .ifPresent(pins::add);
        }

        @Override
        public void onDeactivate() {
            subscriptions.forEach(Subscription::unsubscribe);
            subscriptions.clear();
            pins.forEach(Pin::unbind);
            pins.clear();
            model.description.set(null);
        }

        private void onPriceInput(String price) {
            // Origin separation: only a genuine user commit reaches the domain. Programmatic
            // projections run under the guard and must not be converted back into input.
            if (applyingDomainValue || model.isFocused || price == null || price.isEmpty()) {
                return;
            }

            boolean isValid = MathUtils.isValidDouble(price);
            model.isPriceValid.set(isValid);
            if (!isValid) {
                return;
            }

            try {
                PriceQuote priceQuote = PriceParser.parse(price, draftOfferUseCase.getMarket());
                checkArgument(priceQuote.getValue() > 0);
                priceUseCase.ifPresent(priceSelection -> priceSelection.onSetFixedPriceQuote(priceQuote));
            } catch (Throwable ignore) {
                // Unparseable input falls through to the authoritative re-render below.
            }
            // Re-render the authoritative domain value: a clamped or ignored input can leave the
            // stored quote unchanged, in which case no observable event repaints the field.
            renderAuthoritativeQuote();
        }

        private void renderAuthoritativeQuote() {
            priceUseCase.ifPresent(priceSelection -> renderQuote(priceSelection.getPriceQuote()));
        }

        private void onFocusedChanged(boolean isFocused) {
            model.isFocused = isFocused;
            if (!isFocused) {
                onPriceInput(model.priceString.get());
            }
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
