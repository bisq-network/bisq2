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

package bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class AmountInput {
    protected final Controller controller;

    public AmountInput(boolean isBaseCurrency, boolean showCurrencyCode) {
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

    public void reset() {
        controller.model.reset();
    }

    public void requestFocus() {
        TextField textInput = controller.view.textInput;
        textInput.requestFocus();
        textInput.selectRange(textInput.getLength(), textInput.getLength());
    }

    public void setUseVerySmallText(boolean useVerySmallText) {
        controller.setUseVerySmallText(useVerySmallText);
    }

    protected static class Controller implements bisq.desktop.common.view.Controller {
        @Setter
        protected Model model;
        @Getter
        @Setter
        protected View view;
        protected final NumberValidator validator = new NumberValidator();

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

        private void setUseVerySmallText(boolean useVerySmallText) {
            model.useVerySmallText.set(useVerySmallText);
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

    protected static class Model implements bisq.desktop.common.view.Model {
        protected final boolean isBaseCurrency;
        protected final boolean showCurrencyCode;
        protected final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        protected final StringProperty code = new SimpleStringProperty();
        protected final BooleanProperty useVerySmallText = new SimpleBooleanProperty();
        protected Market selectedMarket;
        protected boolean hasFocus;
        @Setter
        protected boolean useLowPrecision = true;
        private final BooleanProperty isAmountValid = new SimpleBooleanProperty(true);

        protected Model(boolean isBaseCurrency, boolean showCurrencyCode) {
            this.isBaseCurrency = isBaseCurrency;
            this.showCurrencyCode = showCurrencyCode;
        }

        void reset() {
            amount.set(null);
            code.set(null);
            useVerySmallText.set(false);
            selectedMarket = null;
            hasFocus = false;
            isAmountValid.set(false);
        }
    }

    protected static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        protected final ChangeListener<Boolean> focusListener;
        protected final ChangeListener<Monetary> amountListener;
        protected final TextField textInput;
        protected final Label codeLabel;
        private Subscription useVerySmallTextPin;

        protected View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            textInput = createTextInput();
            codeLabel = createCodeLabel();
            codeLabel.setVisible(model.showCurrencyCode);
            codeLabel.setManaged(model.showCurrencyCode);
            root.getChildren().addAll(textInput, codeLabel);
            focusListener = this::onFocusChanged;
            amountListener = this::onAmountChanged;
            initView();
        }

        protected TextField createTextInput() {
            return new TextField();
        }

        protected Label createCodeLabel() {
            return new Label();
        }

        protected void initView() {
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

        protected void applyAmount(Monetary newValue) {
            textInput.setText(newValue == null ? "" : AmountFormatter.formatAmount(newValue, model.useLowPrecision));
            textInput.selectRange(textInput.getLength(), textInput.getLength());
            adjustTextFieldStyle();
        }


        protected void adjustTextFieldStyle() {
        }

        @Override
        protected void onViewAttached() {
            codeLabel.textProperty().bind(model.code);

            textInput.focusedProperty().addListener(focusListener);
            model.amount.addListener(amountListener);

            applyAmount(model.amount.get());
            textInput.requestFocus();
            textInput.selectRange(textInput.getLength(), textInput.getLength());

            useVerySmallTextPin = EasyBind.subscribe(model.useVerySmallText, useVerySmallText -> adjustTextFieldStyle());
        }

        @Override
        protected void onViewDetached() {
            codeLabel.textProperty().unbind();

            textInput.focusedProperty().removeListener(focusListener);
            model.amount.removeListener(amountListener);

            useVerySmallTextPin.unsubscribe();
        }
    }
}
