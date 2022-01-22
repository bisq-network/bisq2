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
import bisq.common.monetary.Direction;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class AmountInput {
    public static class AmountController implements Controller {
        private final AmountModel model;
        @Getter
        private final MonetaryView view;
        private final MonetaryValidator validator = new MonetaryValidator();
        private final ChangeListener<Market> selectedMarketListener;
        private final ChangeListener<Direction> directionChangeListener;

        public AmountController(ObjectProperty<Market> selectedMarket,
                                ObjectProperty<Direction> direction,
                                boolean isBaseCurrency,
                                ObjectProperty<Monetary> amount) {
            this.model = new AmountModel(selectedMarket, direction, isBaseCurrency, amount);

            view = new MonetaryView(model, this, validator);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                model.amount.set(null);
                updateModel();
            };
            directionChangeListener = (observable, oldValue, newValue) -> updateModel();
        }

        public void onViewAttached() {
            model.selectedMarket.addListener(selectedMarketListener);
            model.direction.addListener(directionChangeListener);
        }

        public void onViewDetached() {
            model.selectedMarket.removeListener(selectedMarketListener);
            model.direction.removeListener(directionChangeListener);
        }

        // View events
        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        public void onAmount(String value) {
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.amount.set(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.amount.set(null);
                return;
            }
            if (model.code.get() == null) return;
            model.amount.set(AmountParser.parse(value, model.code.get()));

        }

        private void updateModel() {
            Market market = model.selectedMarket.get();
            if (market != null) {
                model.code.set(model.isBaseCurrency ? market.baseCurrencyCode() : market.quoteCurrencyCode());
                model.prompt.set(Res.offerbook.get("createOffer.amount.prompt", model.code.get()));
                String dir;
                if (model.isBaseCurrency) {
                    dir = model.direction.get() == Direction.BUY ? Res.offerbook.get("buy") : Res.offerbook.get("sell");
                } else {
                    dir = model.direction.get() == Direction.BUY ? Res.offerbook.get("spend") : Res.offerbook.get("receive");
                }
                model.description.set(Res.offerbook.get("createOffer.amount.description", model.code.get(), dir));
            } else {
                model.code.set("");
                model.prompt.set("");
                model.description.set("");
            }
        }
    }

    @Getter
    public static class AmountModel implements Model {
        private final ObjectProperty<Market> selectedMarket;
        private final ObjectProperty<Direction> direction;
        private final boolean isBaseCurrency;
        private final ObjectProperty<Monetary> amount;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty prompt = new SimpleStringProperty();
        private final StringProperty code = new SimpleStringProperty();

        public boolean hasFocus;

        public AmountModel(ObjectProperty<Market> selectedMarket,
                           ObjectProperty<Direction> direction,
                           boolean isBaseCurrency,
                           ObjectProperty<Monetary> amount) {
            this.selectedMarket = selectedMarket;
            this.direction = direction;
            this.isBaseCurrency = isBaseCurrency;
            this.amount = amount;
        }
    }

    public static class MonetaryView extends View<VBox, AmountModel, AmountController> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> inputTextListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final BisqLabel code;
        private final BisqLabel descriptionLabel;

        public MonetaryView(AmountModel model,
                            AmountController controller,
                            MonetaryValidator validator) {
            super(new VBox(), model, controller);

            textInput = new BisqInputTextField(60);
            textInput.setMaxWidth(Double.MAX_VALUE);
            textInput.setValidator(validator);

            code = new BisqLabel();
            code.setMinHeight(42);
            code.setFixWidth(60);
            code.setAlignment(Pos.CENTER);

            HBox hBox = new HBox();
            hBox.getStyleClass().add("input-with-border");
            HBox.setHgrow(textInput, Priority.ALWAYS);
            hBox.getChildren().addAll(textInput, code);

            descriptionLabel = new BisqLabel();
            descriptionLabel.setId("input-description-label");
            descriptionLabel.setPrefWidth(190);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(descriptionLabel, hBox);

            //  Listeners on view component events
            focusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onAmount(textInput.getText());
            };
            inputTextListener = (o, old, newValue) -> controller.onAmount(textInput.getText());

            // Listeners on model change
            amountListener = (o, old, newValue) -> textInput.setText(newValue == null ? "" : AmountFormatter.formatAmount(newValue));
        }

        public void onViewAttached() {
            descriptionLabel.textProperty().bind(model.description);
            textInput.promptTextProperty().bind(model.prompt);
            textInput.textProperty().addListener(inputTextListener);
            textInput.focusedProperty().addListener(focusListener);
            code.textProperty().bind(model.code);
            model.amount.addListener(amountListener);
        }

        public void onViewDetached() {
            descriptionLabel.textProperty().unbind();
            textInput.promptTextProperty().unbind();
            textInput.textProperty().removeListener(inputTextListener);
            textInput.focusedProperty().removeListener(focusListener);
            code.textProperty().unbind();
            model.amount.removeListener(amountListener);
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