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
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

public class OfferDataHeader {
    private final Controller controller;

    public OfferDataHeader(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public void setBisqEasyOffer(@Nullable BisqEasyOffer bisqEasyOffer) {
        controller.setBisqEasyOffer(bisqEasyOffer);
    }

    public HBox getRoot() {
        return controller.view.getRoot();
    }

    public Model getModel() {
        return controller.model;
    }

    public void reset() {
        controller.model.reset();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final ReputationService reputationService;
        private final ServiceProvider serviceProvider;
        private Subscription bisqEasyOfferPin;

        private Controller(ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
            reputationService = serviceProvider.getUserService().getReputationService();

            model = new Model();
            view = new View(model, this);
        }

        private void setBisqEasyOffer(@Nullable BisqEasyOffer bisqEasyOffer) {
            model.getBisqEasyOffer().set(bisqEasyOffer);
        }

        @Override
        public void onActivate() {
            bisqEasyOfferPin = EasyBind.subscribe(model.getBisqEasyOffer(), bisqEasyOffer -> {
                if (bisqEasyOffer == null) {
                    model.reset();
                    return;
                }
            });
        }

        @Override
        public void onDeactivate() {
            bisqEasyOfferPin.unsubscribe();
            model.reset();
        }
    }

    @Slf4j
    @Getter
    public static class Model implements bisq.desktop.common.view.Model {

        @Setter
        private String directionHeadline;
        private final StringProperty directionHeadlineWithMethod = new SimpleStringProperty();
        @Setter
        private String minAmountsHeadline;
        @Setter
        private String maxAmountsHeadline;
        @Setter
        private String fixAmountsHeadline;
        @Setter
        private String detailsHeadline;
        @Setter
        private String toSendAmountDescription;
        @Setter
        private String toSendAmount;
        @Setter
        private String toSendCode;
        @Setter
        private String toReceiveAmountDescription;
        @Setter
        private String toReceiveAmount;
        @Setter
        private String toReceiveCode;
        @Setter
        private String paymentMethodDescription;
        @Setter
        private String paymentMethod;
        @Setter
        private boolean isRangeAmount;

        private final ObjectProperty<BisqEasyOffer> BisqEasyOffer = new SimpleObjectProperty<>();
        private final StringProperty direction = new SimpleStringProperty();
        private final StringProperty leftAmount = new SimpleStringProperty();
        private final StringProperty leftAmountDescription = new SimpleStringProperty();
        private final StringProperty rightAmount = new SimpleStringProperty();
        private final StringProperty rightAmountDescription = new SimpleStringProperty();

        public Model() {
        }

        void reset() {
            BisqEasyOffer.set(null);
            direction.set(null);
            leftAmount.set(null);
            leftAmountDescription.set(null);
            rightAmount.set(null);
            rightAmountDescription.set(null);
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final static double HEIGHT = 61;

        private final Triple<Text, Label, VBox> direction, paymentMethod;
        private final Triple<Triple<Text, Text, Text>, HBox, VBox> toSend, toReceive;
        private final VBox rangeAmountVBox = new VBox(0);

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
            direction.getSecond().setText(model.getDirectionHeadline());
            toSend.getFirst().getFirst().setText(model.getToSendAmountDescription());
            toSend.getFirst().getSecond().setText(model.getToSendAmount());
            toSend.getFirst().getThird().setText(model.getToSendCode());
            toReceive.getFirst().getFirst().setText(model.getToReceiveAmountDescription());
            toReceive.getFirst().getSecond().setText(model.getToReceiveAmount());
            toReceive.getFirst().getThird().setText(model.getToReceiveCode());
            paymentMethod.getFirst().setText(model.getPaymentMethodDescription());
            paymentMethod.getSecond().setText(model.getPaymentMethod());

            VBox toSendVBox = toSend.getThird();
            VBox toReceiveVBox = toReceive.getThird();
            rangeAmountVBox.getChildren().clear();
            rangeAmountVBox.setAlignment(Pos.TOP_LEFT);
            root.getChildren().clear();
            HBox.setMargin(paymentMethod.getThird(), new Insets(0, 10, 0, 0));
            if (model.isRangeAmount) {
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
        }

        @Override
        protected void onViewDetached() {
        }

        private Triple<Text, Label, VBox> getElements() {
            return getElements(null);
        }

        private Triple<Text, Label, VBox> getElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
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
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
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