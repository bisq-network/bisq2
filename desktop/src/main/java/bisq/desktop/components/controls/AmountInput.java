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

package bisq.desktop.components.controls;

import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.parser.AmountParser;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
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
        public void onActivate() {
            if (model.isCreateOffer) {
                model.amount.set(null);
            }
            updateModel();
        }

        @Override
        public void onDeactivate() {
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
            model.prompt.set(Res.get("createOffer.volume.prompt", code));
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

    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int WIDTH = 250;
        private final static int CODE_LABEL_WIDTH = 60;
        private final ChangeListener<String> textInputListener;
        private final ChangeListener<Boolean> focusListener;
        private final ChangeListener<Monetary> amountListener;
        private final Label rightLabel;
        private final TextInputBox textInputBox;

        private View(Model model, Controller controller, MonetaryValidator validator) {
            super(new Pane(), model, controller);

            textInputBox = new TextInputBox(Res.get("satoshisquareapp.createOffer.maxAmount"),
                    Res.get("satoshisquareapp.setDefaultUserProfile.nickName.prompt"));
            textInputBox.setPrefWidth(WIDTH);
            textInputBox.setValidator(validator);

            rightLabel = new Label();
            rightLabel.setMinHeight(42);
            rightLabel.setAlignment(Pos.CENTER_RIGHT);
            rightLabel.setMinWidth(CODE_LABEL_WIDTH);
            rightLabel.setMaxWidth(CODE_LABEL_WIDTH);
            rightLabel.setLayoutX(WIDTH - CODE_LABEL_WIDTH - 13);
            rightLabel.setLayoutY(11);
            rightLabel.getStyleClass().add("bisq-amount-input-code-label");

            root.getChildren().addAll(textInputBox, rightLabel);

            //  Listeners on view component events
            focusListener = (o, old, newValue) -> {
                log.error("old {}, newValue={}", old, newValue);
                controller.onFocusChange(newValue);
                controller.onAmount(textInputBox.getText());
            };
            textInputListener = (o, old, newValue) -> controller.onAmount(textInputBox.getText());

            // Listeners on model change
            amountListener = (o, old, newValue) -> applyAmount(newValue);
        }

        @Override
        protected void onViewAttached() {
            if (model.isCreateOffer) {
                textInputBox.textProperty().addListener(textInputListener);
                textInputBox.inputTextFieldFocusedProperty().addListener(focusListener);
            } else {
                // editable/disable changes style. setMouseTransparent is just for prototyping now
                textInputBox.setMouseTransparent(true);
            }
            textInputBox.promptTextProperty().bind(model.prompt);
            textInputBox.descriptionProperty().bind(model.description);
            rightLabel.textProperty().bind(model.code);
            model.amount.addListener(amountListener);
            applyAmount(model.amount.get());
        }

        @Override
        protected void onViewDetached() {
            if (model.isCreateOffer) {
                textInputBox.textProperty().removeListener(textInputListener);
                textInputBox.inputTextFieldFocusedProperty().removeListener(focusListener);
            }
            textInputBox.promptTextProperty().unbind();
            textInputBox.descriptionProperty().unbind();

            rightLabel.textProperty().unbind();
            model.amount.removeListener(amountListener);
        }

        private void applyAmount(Monetary newValue) {
            textInputBox.setText(newValue == null ? "" : AmountFormatter.formatAmount(newValue, true));
        }

    }
}