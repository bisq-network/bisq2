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

package bisq.desktop.main.content.bisq_easy.components;

import bisq.common.data.Triple;
import bisq.desktop.components.containers.Spacer;
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
import lombok.Getter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

public class ReviewDataDisplay {
    private final Controller controller;

    public ReviewDataDisplay() {
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

    public void setToSendAmount(String value) {
        controller.model.getToSendAmount().set(value);
    }

    public void setToSendCode(String value) {
        controller.model.getToSendCode().set(value);
    }

    public void setToReceiveAmountDescription(String value) {
        controller.model.getToReceiveAmountDescription().set(value);
    }

    public void setToReceiveAmount(String value) {
        controller.model.getToReceiveAmount().set(value);
    }

    public void setToReceiveCode(String value) {
        controller.model.getToReceiveCode().set(value);
    }

    public void setPaymentMethodDescription(String value) {
        controller.model.getPaymentMethodDescription().set(value);
    }

    public void setPaymentMethod(String value) {
        controller.model.getPaymentMethod().set(value);
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
        private final StringProperty toSendAmount = new SimpleStringProperty();
        private final StringProperty toSendCode = new SimpleStringProperty();
        private final StringProperty toReceiveAmountDescription = new SimpleStringProperty();
        private final StringProperty toReceiveAmount = new SimpleStringProperty();
        private final StringProperty toReceiveCode = new SimpleStringProperty();
        private final StringProperty paymentMethodDescription = new SimpleStringProperty();
        private final StringProperty paymentMethod = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private static final double HEIGHT = 61;

        private final Triple<Text, Label, VBox> direction, paymentMethod;
        private final Triple<Triple<Text, Text, Text>, HBox, VBox> toSend, toReceive;
        private final VBox rangeAmountVBox = new VBox(0);
        private Subscription isRangeAmountPin;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.TOP_LEFT);

            direction = getElements(Res.get("bisqEasy.tradeState.header.direction"));
            toSend = getAmountElements();
            toReceive = getAmountElements();
            paymentMethod = getElements();
        }

        @Override
        protected void onViewAttached() {
            direction.getSecond().textProperty().bind(model.getDirection());
            toSend.getFirst().getFirst().textProperty().bind(model.getToSendAmountDescription());
            toSend.getFirst().getSecond().textProperty().bind(model.getToSendAmount());
            toSend.getFirst().getThird().textProperty().bind(model.getToSendCode());
            toReceive.getFirst().getFirst().textProperty().bind(model.getToReceiveAmountDescription());
            toReceive.getFirst().getSecond().textProperty().bind(model.getToReceiveAmount());
            toReceive.getFirst().getThird().textProperty().bind(model.getToReceiveCode());
            paymentMethod.getFirst().textProperty().bind(model.getPaymentMethodDescription());
            paymentMethod.getSecond().textProperty().bind(model.getPaymentMethod());

            isRangeAmountPin = EasyBind.subscribe(model.getIsRangeAmount(), isRangeAmount -> {
                VBox toSendVBox = toSend.getThird();
                VBox toReceiveVBox = toReceive.getThird();
                rangeAmountVBox.getChildren().clear();
                rangeAmountVBox.setAlignment(Pos.TOP_LEFT);
                root.getChildren().clear();
                HBox.setMargin(paymentMethod.getThird(), new Insets(0, 10, 0, 0));

                if (isRangeAmount) {
                    VBox.setMargin(toReceiveVBox, new Insets(-10, 0, 0, 0));
                    rangeAmountVBox.getChildren().addAll(toSendVBox, toReceiveVBox);
                    root.getChildren().addAll(
                            direction.getThird(), Spacer.fillHBox(),
                            rangeAmountVBox, Spacer.fillHBox(),
                            paymentMethod.getThird());
                } else {
                    root.getChildren().addAll(
                            direction.getThird(), Spacer.fillHBox(),
                            toSendVBox, Spacer.fillHBox(),
                            toReceiveVBox, Spacer.fillHBox(),
                            paymentMethod.getThird());
                }
            });
        }

        @Override
        protected void onViewDetached() {
            direction.getSecond().textProperty().unbind();
            toSend.getFirst().getFirst().textProperty().unbind();
            toSend.getFirst().getSecond().textProperty().unbind();
            toSend.getFirst().getThird().textProperty().unbind();
            toReceive.getFirst().getFirst().textProperty().unbind();
            toReceive.getFirst().getSecond().textProperty().unbind();
            toReceive.getFirst().getThird().textProperty().unbind();
            paymentMethod.getFirst().textProperty().unbind();
            paymentMethod.getSecond().textProperty().unbind();

            isRangeAmountPin.unsubscribe();
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
            HBox hBox = new HBox(3, amount, code);
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