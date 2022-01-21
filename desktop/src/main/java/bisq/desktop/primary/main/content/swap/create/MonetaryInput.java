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

package bisq.desktop.primary.main.content.swap.create;

import bisq.common.currency.BisqCurrency;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
public class MonetaryInput {
    public static class MonetaryController implements Controller {
        private final MonetaryModel model;
        @Getter
        private final MonetaryView view;
        private final MonetaryValidator validator = new MonetaryValidator();

        public MonetaryController(ObservableList<BisqCurrency> currencies,
                                  ObjectProperty<Monetary> amount,
                                  ObjectProperty<BisqCurrency> selectedCurrency,
                                  String description,
                                  String prompt) {
            this.model = new MonetaryModel(currencies, amount, selectedCurrency);

            view = new MonetaryView(model, this, validator, description,prompt);
        }

        public void onSelectCurrency(BisqCurrency selectedItem) {
            model.selectedCurrency.set(selectedItem);
        }

        public void onMonetaryInput(String value) {
            if (!model.hasFocus && validator.validate(value).isValid && model.selectedCurrency.get() != null) {
                model.amount.set(AmountParser.parse(value, model.selectedCurrency.get().getCode()));
            }
        }

        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }
    }

    @Getter
    public static class MonetaryModel implements Model {
        private final ObservableList<BisqCurrency> currencies;
        private final ObjectProperty<Monetary> amount;
        private final ObjectProperty<BisqCurrency> selectedCurrency;
        public boolean hasFocus;

        public MonetaryModel(ObservableList<BisqCurrency> currencies,
                             ObjectProperty<Monetary> amount,
                             ObjectProperty<BisqCurrency> selectedCurrency) {
            this.currencies = currencies;
            this.amount = amount;
            this.selectedCurrency = selectedCurrency;
        }
    }

    public static class MonetaryView extends View<VBox, MonetaryModel, MonetaryController> {
        private final BisqInputTextField textInput;
        private final BisqComboBox<BisqCurrency> comboBox;
        private final ChangeListener<String> inputTextListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> monetaryListener;
        private final ChangeListener<BisqCurrency> selectedCurrencyListener;

        public MonetaryView(MonetaryModel model,
                            MonetaryController controller,
                            MonetaryValidator validator,
                            String description,
                            String prompt) {
            super(new VBox(), model, controller);

            textInput = new BisqInputTextField(60);
            textInput.setPromptText(prompt);
            textInput.setMaxWidth(Double.MAX_VALUE);
            textInput.setValidator(validator);

            comboBox = new BisqComboBox<>();
            comboBox.setMinHeight(42);
            comboBox.setItems(model.getCurrencies());
            comboBox.setMaxWidth(80); // Hack to not get full  width of list displayed. There should be a better solution 
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable BisqCurrency currencyItem) {
                    return currencyItem != null ? currencyItem.getCode() : "";
                }

                @Override
                public BisqCurrency fromString(String value) {
                    return comboBox.getItems().stream()
                            .filter(currencyItem -> currencyItem.getCode().equals(value))
                            .findAny()
                            .orElse(null);
                }
            });
            comboBox.setCellFactory(getCellFactory());
            comboBox.setVisibleRowCount(10);
            comboBox.getEditor().getStyleClass().add("combo-box-editor-bold");
            comboBox.setOnAction(e -> {
                controller.onSelectCurrency(comboBox.getSelectionModel().getSelectedItem());
                controller.onMonetaryInput(textInput.getText());
            });

            HBox hBox = new HBox();
            hBox.getStyleClass().add("input-with-border");
            HBox.setHgrow(textInput, Priority.ALWAYS);
            hBox.getChildren().addAll(textInput, comboBox);

            BisqLabel descriptionLabel = new BisqLabel(description);
            descriptionLabel.setId("input-description-label");
            descriptionLabel.setPrefWidth(190);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(descriptionLabel, hBox);

            //  Listeners on view component events
            focusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onMonetaryInput(textInput.getText());
            };
            inputTextListener = (o, old, newValue) -> controller.onMonetaryInput(textInput.getText());

            // Listeners on model change
            selectedCurrencyListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
            monetaryListener = (o, old, newValue) -> textInput.setText(AmountFormatter.formatAmount(newValue));
        }

        public void onViewAttached() {
            textInput.textProperty().addListener(inputTextListener);
            textInput.focusedProperty().addListener(focusListener);
            comboBox.setOnAction(e -> {
                controller.onSelectCurrency(comboBox.getSelectionModel().getSelectedItem());
                controller.onMonetaryInput(textInput.getText());
            });
            model.selectedCurrency.addListener(selectedCurrencyListener);
            model.amount.addListener(monetaryListener);
        }

        public void onViewDetached() {
            textInput.textProperty().removeListener(inputTextListener);
            textInput.focusedProperty().removeListener(focusListener);
            comboBox.setOnAction(null);

            model.selectedCurrency.removeListener(selectedCurrencyListener);
            model.amount.removeListener(monetaryListener);
        }

        private Callback<ListView<BisqCurrency>, ListCell<BisqCurrency>> getCellFactory() {
            return listView -> new ListCell<>() {
                @Override
                protected void updateItem(BisqCurrency item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        HBox box = new HBox();
                        box.setSpacing(20);
                        Label currencyType = new BisqLabel(item.isFiat() ?
                                Res.common.get("fiat") :
                                Res.common.get("crypto"));
                        currencyType.getStyleClass().add("currency-label-small");
                        Label currency = new BisqLabel(item.getCodeAndName());
                        currency.getStyleClass().add("currency-label");
                        box.getChildren().addAll(currencyType, currency);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            };
        }
    }
}