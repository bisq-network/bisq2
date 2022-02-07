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

package bisq.desktop.primary.main.content.trade.components;

import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountInput {
    private final AmountInput.AmountController controller;

    public AmountInput(ReadOnlyObjectProperty<Market> selectedMarket,
                       ReadOnlyObjectProperty<Direction> direction,
                       boolean isBaseCurrency) {
        controller = new AmountInput.AmountController(selectedMarket, direction, isBaseCurrency);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return controller.model.amount;
    }

    public void setAmount(Monetary value) {
        controller.model.amount.set(value);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    public AmountView getView() {
        return controller.view;
    }

    public static class AmountController implements Controller {
        private final AmountModel model;
        @Getter
        private final AmountView view;
        private final MonetaryValidator validator = new MonetaryValidator();
        private final ChangeListener<Market> selectedMarketListener;
        private final ChangeListener<Direction> directionListener;

        private AmountController(ReadOnlyObjectProperty<Market> selectedMarket,
                                 ReadOnlyObjectProperty<Direction> direction,
                                 boolean isBaseCurrency) {
            model = new AmountModel(selectedMarket, direction, isBaseCurrency);
            view = new AmountView(model, this, validator);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                model.amount.set(null);
                updateModel();
            };
            directionListener = (observable, oldValue, newValue) -> updateModel();
        }

        @Override
        public void onViewAttached() {
            model.selectedMarket.addListener(selectedMarketListener);
            model.direction.addListener(directionListener);
            if (model.isCreateOffer) {
                model.amount.set(null);
            }
            updateModel();
        }

        @Override
        public void onViewDetached() {
            model.selectedMarket.removeListener(selectedMarketListener);
            model.direction.removeListener(directionListener);
        }

        // View events
        private void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        private void onAmount(String value) {
            if (value == null) return;
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
            if (market == null) {
                model.code.set("");
                model.prompt.set("");
                model.description.set("");
                return;
            }

            model.code.set(model.isBaseCurrency ? market.baseCurrencyCode() : market.quoteCurrencyCode());
            String code = model.code.get();
            model.prompt.set(Res.get("createOffer.amount.prompt", code));
            String dir;
            Direction direction = model.direction.get();
            if (model.isBaseCurrency) {
                dir = direction == Direction.BUY ? Res.get("buy") : Res.get("sell");
            } else {
                dir = direction == Direction.BUY ? Res.get("spend") : Res.get("receive");
            }
            model.description.set(Res.get("createOffer.amount.description", code, dir));
        }
    }

    private static class AmountModel implements Model {
        private ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        private final ReadOnlyObjectProperty<Market> selectedMarket;
        private final ReadOnlyObjectProperty<Direction> direction;
        private final boolean isBaseCurrency;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty prompt = new SimpleStringProperty();
        private final StringProperty code = new SimpleStringProperty();
        public boolean hasFocus;
        private boolean isCreateOffer = true;

        private AmountModel(ReadOnlyObjectProperty<Market> selectedMarket,
                            ReadOnlyObjectProperty<Direction> direction,
                            boolean isBaseCurrency) {
            this.selectedMarket = selectedMarket;
            this.direction = direction;
            this.isBaseCurrency = isBaseCurrency;
        }
    }

    public static class AmountView extends View<VBox, AmountModel, AmountController> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final BisqLabel code;
        private final BisqLabel descriptionLabel;

        private AmountView(AmountModel model,
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
            textInputListener = (o, old, newValue) -> controller.onAmount(textInput.getText());

            // Listeners on model change
            amountListener = (o, old, newValue) -> applyAmount(newValue);
        }

        private void applyAmount(Monetary newValue) {
            textInput.setText(newValue == null ? "" : AmountFormatter.formatAmount(newValue));
        }

        @Override
        public void onViewAttached() {
            if (model.isCreateOffer) {
                textInput.textProperty().addListener(textInputListener);
                textInput.focusedProperty().addListener(focusListener);
            } else {
                // editable/disable changes style. setMouseTransparent is just for prototyping now
                textInput.setMouseTransparent(true);
            }
            textInput.promptTextProperty().bind(model.prompt);
            descriptionLabel.textProperty().bind(model.description);
            code.textProperty().bind(model.code);
            model.amount.addListener(amountListener);
            applyAmount(model.amount.get());
        }

        @Override
        public void onViewDetached() {
            if (model.isCreateOffer) {
                textInput.textProperty().removeListener(textInputListener);
                textInput.focusedProperty().removeListener(focusListener);
            }
            textInput.promptTextProperty().unbind();
            descriptionLabel.textProperty().unbind();
            code.textProperty().unbind();
            model.amount.removeListener(amountListener);
        }
    }
}