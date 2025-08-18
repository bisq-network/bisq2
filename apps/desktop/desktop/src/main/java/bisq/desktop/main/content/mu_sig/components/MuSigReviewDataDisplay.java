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

package bisq.desktop.main.content.mu_sig.components;

import bisq.common.asset.Asset;
import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

public class MuSigReviewDataDisplay {
    private final Controller controller;

    public MuSigReviewDataDisplay() {
        controller = new Controller();
    }

    public HBox getRoot() {
        return controller.view.getRoot();
    }

    public void setRangeAmount(boolean value) {
        controller.model.getIsRangeAmount().set(value);
    }

    public void setDirection(String value) {
        controller.model.getDirection().set(value);
    }

    public void setToSendAmountDescription(String value) {
        controller.model.getToSendAmountDescription().set(value);
    }

    public void setToSendCode(String value) {
        controller.model.getToSendCode().set(value);
        controller.model.getIsSendBtc().set(Asset.isBtc(value));
    }

    public void setToReceiveAmountDescription(String value) {
        controller.model.getToReceiveAmountDescription().set(value);
    }

    public void setToReceiveCode(String value) {
        controller.model.getToReceiveCode().set(value);
        controller.model.getIsReceiveBtc().set(Asset.isBtc(value));
    }

    public void setPaymentMethodDescription(String value) {
        controller.model.getPaymentMethodDescription().set(value);
    }

    public void setToSendMinAmount(String value) {
        controller.model.getToSendMinAmount().set(value);
    }

    public void setToSendMaxOrFixedAmount(String value) {
        controller.model.getToSendMaxOrFixedAmount().set(value);
    }

    public void setToReceiveMinAmount(String value) {
        controller.model.getToReceiveMinAmount().set(value);
    }

    public void setToReceiveMaxOrFixedAmount(String value) {
        controller.model.getToReceiveMaxOrFixedAmount().set(value);
    }

    public void setPaymentMethod(String value) {
        controller.model.getPaymentMethodDisplayString().set(value);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        private final BooleanProperty isRangeAmount = new SimpleBooleanProperty();
        private final StringProperty direction = new SimpleStringProperty();
        private final StringProperty toSendAmountDescription = new SimpleStringProperty();
        private final StringProperty toSendCode = new SimpleStringProperty();
        private final StringProperty toReceiveAmountDescription = new SimpleStringProperty();
        private final StringProperty toReceiveCode = new SimpleStringProperty();
        private final StringProperty paymentMethodDescription = new SimpleStringProperty();
        private final StringProperty paymentMethodDisplayString = new SimpleStringProperty();
        private final BooleanProperty isSendBtc = new SimpleBooleanProperty(false);
        private final BooleanProperty isReceiveBtc = new SimpleBooleanProperty(false);
        private final StringProperty toSendMinAmount = new SimpleStringProperty();
        private final StringProperty toSendMaxOrFixedAmount = new SimpleStringProperty();
        private final StringProperty toReceiveMinAmount = new SimpleStringProperty();
        private final StringProperty toReceiveMaxOrFixedAmount = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private static final double HEIGHT = 61;
        @SuppressWarnings("UnnecessaryUnicodeEscape")
        private static final String DASH_SYMBOL = "\u2013"; // Unicode for "–"

        private final Triple<Text, Label, VBox> direction, paymentMethod;
        private final Triple<Triple<Text, Text, Text>, HBox, VBox> toSendMaxOrFixedAmount, toReceiveMaxOrFixedAmount;
        private final Text toSendMinAmount, toReceiveMinAmount;

        private final VBox rangeAmountVBox = new VBox(0);
        private Subscription isRangeAmountPin, isSendBtcPin, isReceiveBtcPin;

        private final BitcoinAmountDisplay toSendBitcoinMinAmountDisplay = new BitcoinAmountDisplay("0", false);
        private final BitcoinAmountDisplay toSendBitcoinMaxOrFixedAmountDisplay = new BitcoinAmountDisplay("0", false);
        private final Label toSendDashLabel = new Label(DASH_SYMBOL);
        private final Label toReceiveDashLabel = new Label(DASH_SYMBOL);
        private final BitcoinAmountDisplay toReceiveBitcoinMinAmountDisplay = new BitcoinAmountDisplay("0", false);
        private final BitcoinAmountDisplay toReceiveBitcoinMaxOrFixedAmountDisplay = new BitcoinAmountDisplay("0", false);

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.TOP_LEFT);

            toSendDashLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-code");
            toSendDashLabel.setAlignment(Pos.CENTER);
            toReceiveDashLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-code");
            toReceiveDashLabel.setAlignment(Pos.CENTER);

            direction = getElements(Res.get("bisqEasy.tradeState.header.direction"));
            toSendMaxOrFixedAmount = getAmountElements();
            toReceiveMaxOrFixedAmount = getAmountElements();
            paymentMethod = getElements();

            toSendMinAmount = new Text();
            toSendMinAmount.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
            toReceiveMinAmount = new Text();
            toReceiveMinAmount.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
        }

        @Override
        protected void onViewAttached() {
            configureBitcoinAmountDisplay(toSendBitcoinMinAmountDisplay);
            configureBitcoinAmountDisplay(toSendBitcoinMaxOrFixedAmountDisplay);
            configureBitcoinAmountDisplay(toReceiveBitcoinMinAmountDisplay);
            configureBitcoinAmountDisplay(toReceiveBitcoinMaxOrFixedAmountDisplay);

            direction.getSecond().textProperty().bind(model.getDirection());
            toSendMaxOrFixedAmount.getFirst().getFirst().textProperty().bind(model.getToSendAmountDescription());
            toSendMaxOrFixedAmount.getFirst().getSecond().textProperty().bind(model.getToSendMaxOrFixedAmount());
            toSendMaxOrFixedAmount.getFirst().getThird().textProperty().bind(model.getToSendCode());
            toSendMinAmount.textProperty().bind(model.getToSendMinAmount());
            toReceiveMaxOrFixedAmount.getFirst().getFirst().textProperty().bind(model.getToReceiveAmountDescription());
            toReceiveMaxOrFixedAmount.getFirst().getSecond().textProperty().bind(model.getToReceiveMaxOrFixedAmount());
            toReceiveMaxOrFixedAmount.getFirst().getThird().textProperty().bind(model.getToReceiveCode());
            toReceiveMinAmount.textProperty().bind(model.getToReceiveMinAmount());
            paymentMethod.getFirst().textProperty().bind(model.getPaymentMethodDescription());
            paymentMethod.getSecond().textProperty().bind(model.getPaymentMethodDisplayString());
            if (model.getPaymentMethodDisplayString().get().length() > 20) {
                paymentMethod.getSecond().setTooltip(new BisqTooltip(model.getPaymentMethodDisplayString().get()));
            }

            toSendBitcoinMinAmountDisplay.getBtcAmount().bind(model.getToSendMinAmount());
            toSendBitcoinMaxOrFixedAmountDisplay.getBtcAmount().bind(model.getToSendMaxOrFixedAmount());
            toReceiveBitcoinMinAmountDisplay.getBtcAmount().bind(model.getToReceiveMinAmount());
            toReceiveBitcoinMaxOrFixedAmountDisplay.getBtcAmount().bind(model.getToReceiveMaxOrFixedAmount());

            isSendBtcPin = EasyBind.subscribe(EasyBind.combine(model.getIsSendBtc(), model.getIsRangeAmount(), Pair::new), combinedValues -> {
                boolean isSendBtc = combinedValues.getFirst();
                boolean isRangeAmount = combinedValues.getSecond();

                HBox amountHBox = toSendMaxOrFixedAmount.getSecond();
                amountHBox.getChildren().clear();
                VBox.setMargin(amountHBox, null);

                if (isSendBtc) {
                    if (isRangeAmount) {
                        amountHBox.getChildren().addAll(toSendBitcoinMinAmountDisplay, toSendDashLabel, toSendBitcoinMaxOrFixedAmountDisplay, toSendMaxOrFixedAmount.getFirst().getThird());
                        toSendDashLabel.setTranslateY(0);
                    } else {
                        amountHBox.getChildren().addAll(toSendBitcoinMaxOrFixedAmountDisplay, toSendMaxOrFixedAmount.getFirst().getThird());
                    }
                    amountHBox.setAlignment(Pos.CENTER_LEFT);
                    VBox.setMargin(amountHBox, new Insets(-15, 0, 0, 0));
                } else {
                    if (isRangeAmount) {
                        amountHBox.getChildren().addAll(toSendMinAmount, toSendDashLabel, toSendMaxOrFixedAmount.getFirst().getSecond(), toSendMaxOrFixedAmount.getFirst().getThird());
                        toSendDashLabel.setTranslateY(-2);
                    } else {
                        amountHBox.getChildren().addAll(toSendMaxOrFixedAmount.getFirst().getSecond(), toSendMaxOrFixedAmount.getFirst().getThird());
                    }
                    amountHBox.setAlignment(Pos.BASELINE_LEFT);
                }
            });

            isReceiveBtcPin = EasyBind.subscribe(EasyBind.combine(model.getIsReceiveBtc(), model.getIsRangeAmount(), Pair::new), combinedValues -> {
                boolean isReceiveBtc = combinedValues.getFirst();
                boolean isRangeAmount = combinedValues.getSecond();

                HBox amountHBox = toReceiveMaxOrFixedAmount.getSecond();
                amountHBox.getChildren().clear();
                VBox.setMargin(amountHBox, null);//reset Margin setting

                if (isReceiveBtc) {
                    if (isRangeAmount) {
                        amountHBox.getChildren().addAll(toReceiveBitcoinMinAmountDisplay, toReceiveDashLabel, toReceiveBitcoinMaxOrFixedAmountDisplay, toReceiveMaxOrFixedAmount.getFirst().getThird());
                        toReceiveDashLabel.setTranslateY(0);
                    } else {
                        amountHBox.getChildren().addAll(toReceiveBitcoinMaxOrFixedAmountDisplay, toReceiveMaxOrFixedAmount.getFirst().getThird());
                    }
                    amountHBox.setAlignment(Pos.CENTER_LEFT);
                    VBox.setMargin(amountHBox, new Insets(-15, 0, 0, 0));
                } else {
                    if (isRangeAmount) {
                        amountHBox.getChildren().addAll(toReceiveMinAmount, toReceiveDashLabel, toReceiveMaxOrFixedAmount.getFirst().getSecond(), toReceiveMaxOrFixedAmount.getFirst().getThird());
                        toReceiveDashLabel.setTranslateY(-2);
                    } else {
                        amountHBox.getChildren().addAll(toReceiveMaxOrFixedAmount.getFirst().getSecond(), toReceiveMaxOrFixedAmount.getFirst().getThird());
                    }
                    amountHBox.setAlignment(Pos.BASELINE_LEFT);
                }
            });

            isRangeAmountPin = EasyBind.subscribe(model.getIsRangeAmount(), isRangeAmount -> {
                VBox toSendVBox = toSendMaxOrFixedAmount.getThird();
                VBox toReceiveVBox = toReceiveMaxOrFixedAmount.getThird();
                rangeAmountVBox.getChildren().clear();
                rangeAmountVBox.setAlignment(Pos.TOP_LEFT);
                root.getChildren().clear();
                HBox.setMargin(paymentMethod.getThird(), new Insets(0, 10, 0, 0));

                if (isRangeAmount) {
                    VBox.setMargin(toReceiveVBox, new Insets(-10, 0, 0, 0));
                    rangeAmountVBox.getChildren().addAll(toSendVBox, toReceiveVBox);
                    root.getChildren().addAll(direction.getThird(), Spacer.fillHBox(), rangeAmountVBox, Spacer.fillHBox(), paymentMethod.getThird());
                } else {
                    root.getChildren().addAll(direction.getThird(), Spacer.fillHBox(), toSendVBox, Spacer.fillHBox(), toReceiveVBox, Spacer.fillHBox(), paymentMethod.getThird());
                }
            });
        }

        @Override
        protected void onViewDetached() {
            direction.getSecond().textProperty().unbind();
            toSendMaxOrFixedAmount.getFirst().getFirst().textProperty().unbind();
            toSendMaxOrFixedAmount.getFirst().getSecond().textProperty().unbind();
            toSendMaxOrFixedAmount.getFirst().getThird().textProperty().unbind();
            toSendMinAmount.textProperty().unbind();
            toReceiveMaxOrFixedAmount.getFirst().getFirst().textProperty().unbind();
            toReceiveMaxOrFixedAmount.getFirst().getSecond().textProperty().unbind();
            toReceiveMaxOrFixedAmount.getFirst().getThird().textProperty().unbind();
            toReceiveMinAmount.textProperty().unbind();
            paymentMethod.getFirst().textProperty().unbind();
            paymentMethod.getSecond().textProperty().unbind();
            paymentMethod.getSecond().setTooltip(null);

            isRangeAmountPin.unsubscribe();
            isSendBtcPin.unsubscribe();
            isReceiveBtcPin.unsubscribe();

            toSendBitcoinMinAmountDisplay.getBtcAmount().unbind();
            toSendBitcoinMaxOrFixedAmountDisplay.getBtcAmount().unbind();
            toReceiveBitcoinMinAmountDisplay.getBtcAmount().unbind();
            toReceiveBitcoinMaxOrFixedAmountDisplay.getBtcAmount().unbind();
        }

        private void configureBitcoinAmountDisplay(BitcoinAmountDisplay bitcoinAmountDisplay) {
            bitcoinAmountDisplay.applyMediumCompactConfig();
            bitcoinAmountDisplay.setTextAlignment(TextAlignment.LEFT);
            bitcoinAmountDisplay.setAlignment(Pos.CENTER_LEFT);
        }

        private Triple<Text, Label, VBox> getElements() {
            return getElements(null);
        }

        private Triple<Text, Label, VBox> getElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-description");
            Label valueLabel = new Label();
            valueLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
            valueLabel.setMaxWidth(250);

            VBox.setVgrow(valueLabel, Priority.ALWAYS);
            VBox.setMargin(valueLabel, new Insets(-2, 0, 0, 0));
            VBox vBox = new VBox(0, descriptionLabel, valueLabel);
            vBox.setFillWidth(true);
            vBox.setAlignment(Pos.TOP_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(descriptionLabel, valueLabel, vBox);
        }

        private Triple<Triple<Text, Text, Text>, HBox, VBox> getAmountElements() {
            Text descriptionLabel = new Text();
            descriptionLabel.getStyleClass().add("bisq-easy-trade-wizard-review-header-description");
            Text amount = new Text();
            amount.getStyleClass().add("bisq-easy-trade-wizard-review-header-value");
            Text code = new Text();
            code.getStyleClass().add("bisq-easy-trade-wizard-review-header-code");

            HBox.setMargin(amount, new Insets(0.5, 0, 0, 0));
            HBox hBox = new HBox(5, amount, code);
            hBox.setAlignment(Pos.BASELINE_LEFT);
            VBox.setMargin(hBox, new Insets(-2, 0, 0, 0));
            VBox.setVgrow(hBox, Priority.ALWAYS);
            VBox vBox = new VBox(0, descriptionLabel, hBox);
            vBox.setFillWidth(true);
            vBox.setAlignment(Pos.TOP_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(new Triple<>(descriptionLabel, amount, code), hBox, vBox);
        }
    }
}