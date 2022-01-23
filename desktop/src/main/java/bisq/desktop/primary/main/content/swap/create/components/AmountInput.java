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

package bisq.desktop.primary.main.content.swap.create.components;

import bisq.offer.Direction;
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
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
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
        private final ChangeListener<Direction> directionListener;

        public AmountController(OfferPreparationModel offerPreparationModel, boolean isBaseCurrency) {
            model = new AmountModel(offerPreparationModel, isBaseCurrency);
            view = new MonetaryView(model, this, validator);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                model.setAmount(null);
                updateModel();
            };
            directionListener = (observable, oldValue, newValue) -> updateModel();
        }

        public void onViewAttached() {
            model.selectedMarketProperty().addListener(selectedMarketListener);
            model.directionProperty().addListener(directionListener);
        }

        public void onViewDetached() {
            model.selectedMarketProperty().removeListener(selectedMarketListener);
            model.directionProperty().removeListener(directionListener);
        }

        // View events
        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        public void onAmount(String value) {
            if (value == null) return;
            if (model.hasFocus) return;
            if (value.isEmpty()) {
                model.setAmount(null);
                return;
            }
            if (!validator.validate(value).isValid) {
                model.setAmount(null);
                return;
            }
            if (model.code.get() == null) return;
            model.setAmount(AmountParser.parse(value, model.code.get()));

        }

        private void updateModel() {
            Market market = model.getSelectedMarket();
            if (market == null) {
                model.code.set("");
                model.prompt.set("");
                model.description.set("");
                return;
            }

            model.code.set(model.isBaseCurrency ? market.baseCurrencyCode() : market.quoteCurrencyCode());
            String code = model.code.get();
            model.prompt.set(Res.offerbook.get("createOffer.amount.prompt", code));
            String dir;
            Direction direction = model.getDirection();
            if (model.isBaseCurrency) {
                dir = direction == Direction.BUY ? Res.offerbook.get("buy") : Res.offerbook.get("sell");
            } else {
                dir = direction == Direction.BUY ? Res.offerbook.get("spend") : Res.offerbook.get("receive");
            }
            model.description.set(Res.offerbook.get("createOffer.amount.description", code, dir));
        }
    }

    @Getter
    public static class AmountModel implements Model {
        @Delegate
        private final OfferPreparationModel offerPreparationModel;
        private final boolean isBaseCurrency;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty prompt = new SimpleStringProperty();
        private final StringProperty code = new SimpleStringProperty();
        public boolean hasFocus;

        public AmountModel(OfferPreparationModel offerPreparationModel, boolean isBaseCurrency) {
            this.offerPreparationModel = offerPreparationModel;
            this.isBaseCurrency = isBaseCurrency;
        }

        private ReadOnlyObjectProperty<Monetary> amountProperty() {
            return isBaseCurrency ? baseSideAmountProperty() : quoteSideAmountProperty();
        }

        private void setAmount(Monetary amount) {
            if (isBaseCurrency) {
                setBaseSideAmount(amount);
            } else {
                setQuoteSideAmount(amount);
            }
        }
    }

    public static class MonetaryView extends View<VBox, AmountModel, AmountController> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> textInputListener;
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
            textInputListener = (o, old, newValue) -> controller.onAmount(textInput.getText());

            // Listeners on model change
            amountListener = (o, old, newValue) -> textInput.setText(newValue == null ? "" : AmountFormatter.formatAmount(newValue));
        }

        public void onViewAttached() {
            descriptionLabel.textProperty().bind(model.description);
            textInput.promptTextProperty().bind(model.prompt);
            textInput.textProperty().addListener(textInputListener);
            textInput.focusedProperty().addListener(focusListener);
            code.textProperty().bind(model.code);
            model.amountProperty().addListener(amountListener);
        }

        public void onViewDetached() {
            descriptionLabel.textProperty().unbind();
            textInput.promptTextProperty().unbind();
            textInput.textProperty().removeListener(textInputListener);
            textInput.focusedProperty().removeListener(focusListener);
            code.textProperty().unbind();
            model.amountProperty().removeListener(amountListener);
        }
    }
}