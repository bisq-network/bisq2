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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.input;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.validation.NumberValidation;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigAmountTextInputController implements Controller {
    private final MuSigAmountTextInputModel model;
    @Getter
    private final MuSigAmountTextInputView view;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigAmountTextInputController(ServiceProvider serviceProvider,
                                          boolean isFixedAmount,
                                          boolean isLeftSideRangeAmount) {
        StringConverter<Monetary> stringConverter = new StringConverter<>() {
            @Override
            public String toString(Monetary amount) {
                String string = formatAmount(amount);
                log.error("toString");
                model.getTextInput().set(string);
                return string;
            }

            @Override
            public Monetary fromString(String inputText) {
                log.error("fromString");
                model.getTextInput().set(inputText);
                return parseOrFallback(inputText);
            }
        };

        TextFormatter<Monetary> textFormatter = new TextFormatter<>(stringConverter,
                null,
                change -> {
                    // Check if added string is valid. Can be a number or the local specific decimal separator.
                    // change.getText() is freshly added string
                    if (!NumberValidation.isValidNumberInputToken(change.getText())) {
                        return null;
                    }
                    // change.getControlNewText() is full string in input field
                    String controlNewText = change.getControlNewText();
                    log.error("parseAndApplyAmount");
                    parseAndApplyAmount(controlNewText);
                    return change;
                });

        model = new MuSigAmountTextInputModel(textFormatter, isFixedAmount, isLeftSideRangeAmount);
        view = new MuSigAmountTextInputView(model, this);
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        subscriptions.add(EasyBind.subscribe(model.getMarket(), market -> {
            //reset();
            applyCode();
        }));
        subscriptions.add(EasyBind.subscribe(model.getIsBaseCurrency(), isBaseCurrency -> {
            //reset();
            applyCode();
        }));
        subscriptions.add(EasyBind.subscribe(model.getAmount(), amount -> {
            Monetary amountFromInputField = model.getTextFormatter().getValue();
            if (model.getFocusedProperty().get()) {
                return;
            }
            model.getTextFormatter().setValue(amount);
        }));
    }


    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        model.reset();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setMarket(Market market) {
        model.getMarket().set(market);
    }

    public void setAmount(Monetary value) {
        model.getAmount().set(value);
    }

    public void setIsBaseCurrency(boolean value) {
        model.getIsBaseCurrency().set(value);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return model.getAmount();
    }

    public ReadOnlyStringProperty textInputProperty() {
        return model.getTextInput();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return model.getFocusedProperty();
    }

    public void setSumOfNumChars(int value) {
        model.getSumOfNumChars().set(value);
    }

    public void setAmountFieldWidth(double value) {
        model.getAmountFieldWidth().set(value);
    }

    public void setDashFieldWidth(double value) {
        model.getDashFieldWidth().set(value);
    }

    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyCode() {
        Market market = model.getMarket().get();
        if (market != null) {
            model.getCode().set(getCode(market));
        } else {
            model.getCode().set(null);
        }
    }

    private String formatAmount(Monetary amount) {
        if (amount != null) {
            return AmountFormatter.formatAmountByMonetaryType(amount);
        } else {
            return "";
        }
    }

    private Monetary parse(String inputText) {
        return AmountParser.parse(inputText, model.getCode().get());
    }

    private Monetary parseOrFallback(String inputText) {
        try {
            return parse(inputText);
        } catch (Exception e) {
            return model.getAmount().get();
        }
    }

    private void parseAndApplyAmount(String inputText) {
        try {
            Monetary amount = parse(inputText);
            model.getTextInput().set(inputText);
            model.getAmount().set(amount);
        } catch (Exception ignore) {
        }
    }

    private String getCode(Market market) {
        boolean isBaseCurrency = model.getIsBaseCurrency().get();
        return isBaseCurrency ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }

  /*  private void reset() {
        model.getAmount().set(null);
        model.getCode().set(null);
        model.getTextFormatter().setValue(null);
    }*/
}
