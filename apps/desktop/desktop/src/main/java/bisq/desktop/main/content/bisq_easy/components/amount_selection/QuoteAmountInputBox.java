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

package bisq.desktop.main.content.bisq_easy.components.amount_selection;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class QuoteAmountInputBox {
    private final Controller controller;

    public QuoteAmountInputBox(boolean isBaseCurrency, boolean showCurrencyCode) {
        controller = new Controller(isBaseCurrency, showCurrencyCode);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return controller.model.amount;
    }

    public BooleanProperty isAmountValidProperty() {
        return controller.model.isAmountValid;
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setAmount(Monetary value) {
        controller.model.amount.set(value);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return controller.view.textInput.focusedProperty();
    }

    public ReadOnlyIntegerProperty lengthProperty() {
        return controller.view.textInput.lengthProperty();
    }

    public int getTextInputLength() {
        return controller.view.textInput.getLength();
    }

    public String getTextInput() {
        return controller.view.textInput.getText();
    }

    public void setTextInputPrefWidth(int prefWidth) {
        controller.view.textInput.setPrefWidth(prefWidth);
    }

    public void reset() {
        controller.model.reset();
    }

    public void requestFocus() {
        TextField textInput = controller.view.textInput;
        textInput.requestFocus();
        textInput.selectRange(textInput.getLength(), textInput.getLength());
    }

    public void setTextInputMaxCharCount(int maxCharCount) {
        controller.model.textInputMaxCharCount = Optional.of(maxCharCount);
    }

    public void deselect() {
        controller.view.textInput.deselect();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(boolean isBaseCurrency, boolean showCurrencyCode) {
            model = new Model(isBaseCurrency, showCurrencyCode);
            view = new View(model, this);
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            model.amount.set(null);
            updateModel();
        }

        @Override
        public void onActivate() {
            model.amount.set(null);
            updateModel();
        }

        @Override
        public void onDeactivate() {
        }

        private void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        private void onAmount(String value) {
            if (StringUtils.isEmpty(value) || StringUtils.isEmpty(model.code.get())) {
                setAmountInvalid();
                return;
            }
            if (value.isEmpty() || !MathUtils.isValidDouble(value)) {
                handleInvalidValue();
                return;
            }

            setAmountValid();
            updateAmountIfNotFocused(value);
        }


        private void handleInvalidValue() {
            if (!model.hasFocus) {
                model.amount.set(null);
            }
            setAmountInvalid();
        }

        private void setAmountInvalid() {
            model.isAmountValid.set(false);
        }

        private void setAmountValid() {
            model.isAmountValid.set(true);
        }

        private void updateAmountIfNotFocused(String value) {
            if (!model.hasFocus) {
                model.amount.set(AmountParser.parse(value, model.code.get()));
            }
        }

        private void updateModel() {
            if (model.selectedMarket == null) {
                model.code.set("");
                return;
            }
            model.code.set(model.isBaseCurrency
                    ? model.selectedMarket.getBaseCurrencyCode()
                    : model.selectedMarket.getQuoteCurrencyCode());
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final BooleanProperty isAmountValid = new SimpleBooleanProperty(true);
        private final boolean isBaseCurrency;
        private final boolean showCurrencyCode;
        private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        private final StringProperty code = new SimpleStringProperty();
        private Optional<Integer> textInputMaxCharCount = Optional.empty();
        private Market selectedMarket;
        private boolean hasFocus;

        private Model(boolean isBaseCurrency, boolean showCurrencyCode) {
            this.isBaseCurrency = isBaseCurrency;
            this.showCurrencyCode = showCurrencyCode;
        }

        void reset() {
            isAmountValid.set(true);
            amount.set(null);
            code.set(null);
            textInputMaxCharCount = Optional.empty();
            selectedMarket = null;
            hasFocus = false;
        }
    }

    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final ChangeListener<String> textListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final TextField textInput;
        private final Label codeLabel;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            textInput = new TextField();
            codeLabel = new Label();
            codeLabel.getStyleClass().add("currency-code");
            codeLabel.setVisible(model.showCurrencyCode);
            codeLabel.setManaged(model.showCurrencyCode);
            root.getChildren().addAll(textInput, codeLabel);
            root.setSpacing(10);
            root.getStyleClass().add("big-amount-input");

            textListener = this::onTextChanged;
            focusListener = this::onFocusChanged;
            amountListener = this::onAmountChanged;
        }

        @Override
        protected void onViewAttached() {
            codeLabel.textProperty().bind(model.code);

            textInput.textProperty().addListener(textListener);
            textInput.focusedProperty().addListener(focusListener);
            model.amount.addListener(amountListener);

            applyAmount(model.amount.get());
            textInput.requestFocus();
            textInput.selectRange(textInput.getLength(), textInput.getLength());
        }

        @Override
        protected void onViewDetached() {
            codeLabel.textProperty().unbind();

            textInput.textProperty().removeListener(textListener);
            textInput.focusedProperty().removeListener(focusListener);
            model.amount.removeListener(amountListener);
        }

        private void onTextChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (model.textInputMaxCharCount.isPresent()) {
                if (newValue.length() > model.textInputMaxCharCount.get()
                        || !newValue.matches(BisqEasyViewUtils.POSITIVE_NUMERIC_WITH_DECIMAL_REGEX)) {
                    textInput.setText(oldValue);
                }
            }
        }

        private void onFocusChanged(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            controller.onFocusChange(newValue);
            if (oldValue) {
                controller.onAmount(textInput.getText());
            }
        }

        private void onAmountChanged(ObservableValue<? extends Monetary> observable,
                                     Monetary oldValue,
                                     Monetary newValue) {
            applyAmount(newValue);
        }

        private void applyAmount(Monetary newValue) {
            textInput.setText(newValue == null ? "" : AmountFormatter.formatQuoteAmount(newValue));
            textInput.selectRange(textInput.getLength(), textInput.getLength());
        }
    }
}
