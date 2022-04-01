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
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountInput {
    private final Controller controller;

    public AmountInput(boolean isBaseCurrency) {
        controller = new Controller(isBaseCurrency);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return controller.model.amount;
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setDirection(Direction direction) {
        controller.setDirection(direction);
    }

    public void setAmount(Monetary value) {
        controller.model.amount.set(value);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MonetaryValidator validator = new MonetaryValidator();

        private Controller(boolean isBaseCurrency) {
            model = new Model(isBaseCurrency);
            view = new View(model, this, validator);
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            model.amount.set(null);
            updateModel();
        }

        private void setDirection(Direction direction) {
            model.direction = direction;
            updateModel();
        }

        @Override
        public void onViewAttached() {
            if (model.isCreateOffer) {
                model.amount.set(null);
            }
            updateModel();
        }

        @Override
        public void onViewDetached() {
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
            if (model.selectedMarket == null) {
                model.code.set("");
                model.prompt.set("");
                model.description.set("");
                return;
            }

            model.code.set(model.isBaseCurrency ? model.selectedMarket.baseCurrencyCode() : model.selectedMarket.quoteCurrencyCode());
            String code = model.code.get();
            model.prompt.set(Res.get("createOffer.amount.prompt", code));
            String dir;
            if (model.isBaseCurrency) {
                dir = model.direction == Direction.BUY ? Res.get("buy") : Res.get("sell");
            } else {
                dir = model.direction == Direction.BUY ? Res.get("spend") : Res.get("receive");
            }
            model.description.set(Res.get("createOffer.amount.description", code, dir));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        private final boolean isBaseCurrency;
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty prompt = new SimpleStringProperty();
        private final StringProperty code = new SimpleStringProperty();
        private Market selectedMarket;
        private Direction direction;
        public boolean hasFocus;
        private boolean isCreateOffer = true;

        private Model(boolean isBaseCurrency) {
            this.isBaseCurrency = isBaseCurrency;
        }
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqInputTextField textInput;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final BisqLabel code;
        private final BisqLabel descriptionLabel;

        private View(Model model,
                     Controller controller,
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
        protected void onViewAttached() {
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
        protected void onViewDetached() {
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