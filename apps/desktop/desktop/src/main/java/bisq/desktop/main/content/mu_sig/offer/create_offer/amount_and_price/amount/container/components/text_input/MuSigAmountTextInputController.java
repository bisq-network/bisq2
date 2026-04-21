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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.components.text_input;

import bisq.common.monetary.Fiat;
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
                String formatted = formatAmount(amount);
                model.getInputText().set(formatted);
                return formatted;
            }

            @Override
            public Monetary fromString(String inputText) {
                model.getInputText().set(inputText);
                return parseOrFallback(inputText);
            }
        };

        TextFormatter<Monetary> textFormatter = new TextFormatter<>(stringConverter,
                null,
                change -> {
                    // Check if added string is valid. Can be a number or the local specific decimal separator.
                    // change.getText() is freshly added string
                    String changeText = change.getText();
                    if (!changeText.isEmpty() && !NumberValidation.isValidNumberInputToken(changeText)) {
                        return null;
                    }
                    // change.getControlNewText() is full string in input field
                    String controlNewText = change.getControlNewText();
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
        subscriptions.add(EasyBind.subscribe(model.getAmount(), amount -> {
            if (amount != null) {
                String code = amount.getCode();
                model.getCode().set(code);

                if (!model.getFocusedProperty().get()) {
                    model.getTextFormatter().setValue(amount);
                }
            }
        }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setAmount(Monetary value) {
        model.getAmount().set(value);
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

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return model.getAmount();
    }

    public ReadOnlyStringProperty inputTextProperty() {
        return model.getInputText();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return model.getFocusedProperty();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private String formatAmount(Monetary amount) {
        if (amount != null) {
            // XMR has precision of 12, but we only show 8 decimal places
            int precision = amount instanceof Fiat ? amount.getLowPrecision() : 8;
            return AmountFormatter.formatAmount(amount, precision);
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
            model.getInputText().set(inputText);
            model.getAmount().set(amount);
        } catch (Exception ignore) {
        }
    }
}
