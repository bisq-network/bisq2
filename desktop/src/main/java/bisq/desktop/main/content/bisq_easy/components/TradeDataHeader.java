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

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Triple;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;

public class TradeDataHeader {
    private final Controller controller;

    public TradeDataHeader(ServiceProvider serviceProvider, String peerDescription) {
        controller = new Controller(serviceProvider, peerDescription);
    }

    public void setSelectedChannel(@Nullable BisqEasyOpenTradeChannel channel) {
        controller.setSelectedChannel(channel);
    }

    public HBox getRoot() {
        return controller.view.getRoot();
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
        private Subscription channelPin;

        private Controller(ServiceProvider serviceProvider, String peerDescription) {
            this.serviceProvider = serviceProvider;
            reputationService = serviceProvider.getUserService().getReputationService();

            model = new Model(peerDescription);
            view = new View(model, this);
        }

        private void setSelectedChannel(@Nullable BisqEasyOpenTradeChannel channel) {
            model.getChannel().set(channel);
        }

        @Override
        public void onActivate() {
            channelPin = EasyBind.subscribe(model.getChannel(), channel -> {
                if (channel == null) {
                    model.reset();
                    return;
                }
                Optional<BisqEasyTrade> optionalBisqEasyTrade = BisqEasyServiceUtil.findTradeFromChannel(serviceProvider, channel);
                if (optionalBisqEasyTrade.isEmpty()) {
                    model.reset();
                    return;
                }

                BisqEasyTrade bisqEasyTrade = optionalBisqEasyTrade.get();
                model.getBisqEasyTrade().set(bisqEasyTrade);

                UserProfile peerUserProfile = channel.getPeer();
                model.getReputationScore().set(reputationService.findReputationScore(peerUserProfile).orElse(ReputationScore.NONE));
                model.getUserProfile().set(peerUserProfile);
                model.getTradeId().set(bisqEasyTrade.getShortId());

                long baseSideAmount = bisqEasyTrade.getContract().getBaseSideAmount();
                long quoteSideAmount = bisqEasyTrade.getContract().getQuoteSideAmount();
                Coin baseAmount = Coin.asBtcFromValue(baseSideAmount);
                String baseAmountString = AmountFormatter.formatAmountWithCode(baseAmount, false);
                Monetary quoteAmount = Fiat.from(quoteSideAmount, bisqEasyTrade.getOffer().getMarket().getQuoteCurrencyCode());
                String quoteAmountString = AmountFormatter.formatAmountWithCode(quoteAmount);
                if (bisqEasyTrade.isSeller()) {
                    model.getDirection().set(Res.get("offer.sell").toUpperCase());
                    model.getLeftAmountDescription().set(Res.get("bisqEasy.tradeState.header.send").toUpperCase());
                    model.getLeftAmount().set(baseAmountString);
                    model.getRightAmountDescription().set(Res.get("bisqEasy.tradeState.header.receive").toUpperCase());
                    model.getRightAmount().set(quoteAmountString);
                } else {
                    model.getDirection().set(Res.get("offer.buy").toUpperCase());
                    model.getLeftAmountDescription().set(Res.get("bisqEasy.tradeState.header.pay").toUpperCase());
                    model.getLeftAmount().set(quoteAmountString);
                    model.getRightAmountDescription().set(Res.get("bisqEasy.tradeState.header.receive").toUpperCase());
                    model.getRightAmount().set(baseAmountString);
                }
            });
        }

        @Override
        public void onDeactivate() {
            channelPin.unsubscribe();
            model.reset();
        }


    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final String peerDescription;

        private final ObjectProperty<BisqEasyOpenTradeChannel> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<BisqEasyTrade> bisqEasyTrade = new SimpleObjectProperty<>();
        private final ObjectProperty<UserProfile> userProfile = new SimpleObjectProperty<>();
        private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
        private final StringProperty direction = new SimpleStringProperty();
        private final StringProperty leftAmount = new SimpleStringProperty();
        private final StringProperty leftAmountDescription = new SimpleStringProperty();
        private final StringProperty rightAmount = new SimpleStringProperty();
        private final StringProperty rightAmountDescription = new SimpleStringProperty();
        private final StringProperty tradeId = new SimpleStringProperty();

        public Model(String peerDescription) {
            this.peerDescription = peerDescription;
        }

        void reset() {
            channel.set(null);
            bisqEasyTrade.set(null);
            userProfile.set(null);
            reputationScore.set(null);
            direction.set(null);
            leftAmount.set(null);
            leftAmountDescription.set(null);
            rightAmount.set(null);
            rightAmountDescription.set(null);
            tradeId.set(null);
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final static double HEIGHT = 61;

        private final Triple<Text, Text, VBox> direction, leftAmount, rightAmount, tradeId;
        private final UserProfileDisplay peersUserProfileDisplay;
        private final Label peerDescription;
        private Subscription userProfilePin, reputationScorePin;

        private View(Model model, Controller controller) {
            super(new HBox(40), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(0, 30, 0, 30));

            peerDescription = new Label();
            peerDescription.getStyleClass().add("bisq-easy-open-trades-header-description");
            peersUserProfileDisplay = new UserProfileDisplay(25);
            peersUserProfileDisplay.setPadding(new Insets(0, -15, 0, 0));
            peersUserProfileDisplay.setMinWidth(140);
            peersUserProfileDisplay.setMaxWidth(140);
            VBox.setMargin(peerDescription, new Insets(2, 0, 3, 0));
            VBox peerVBox = new VBox(0, peerDescription, peersUserProfileDisplay);
            peerVBox.setAlignment(Pos.CENTER_LEFT);

            direction = getElements(Res.get("bisqEasy.tradeState.header.direction"));
            leftAmount = getElements();
            rightAmount = getElements();
            tradeId = getElements(Res.get("bisqEasy.tradeState.header.tradeId"));

            root.getChildren().addAll(peerVBox,
                    direction.getThird(),
                    leftAmount.getThird(),
                    rightAmount.getThird(),
                    tradeId.getThird());
        }

        @Override
        protected void onViewAttached() {
            peerDescription.setText(model.getPeerDescription());
            direction.getSecond().textProperty().bind(model.getDirection());
            leftAmount.getFirst().textProperty().bind(model.getLeftAmountDescription());
            leftAmount.getSecond().textProperty().bind(model.getLeftAmount());
            rightAmount.getFirst().textProperty().bind(model.getRightAmountDescription());
            rightAmount.getSecond().textProperty().bind(model.getRightAmount());
            tradeId.getSecond().textProperty().bind(model.getTradeId());

            userProfilePin = EasyBind.subscribe(model.getUserProfile(), peersUserProfileDisplay::setUserProfile);
            reputationScorePin = EasyBind.subscribe(model.getReputationScore(), peersUserProfileDisplay::setReputationScore);
        }

        @Override
        protected void onViewDetached() {
            direction.getSecond().textProperty().unbind();
            leftAmount.getFirst().textProperty().unbind();
            leftAmount.getSecond().textProperty().unbind();
            rightAmount.getFirst().textProperty().unbind();
            rightAmount.getSecond().textProperty().unbind();
            tradeId.getSecond().textProperty().unbind();

            userProfilePin.unsubscribe();
            reputationScorePin.unsubscribe();
        }

        private Triple<Text, Text, VBox> getElements() {
            return getElements(null);
        }

        private Triple<Text, Text, VBox> getElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
            Text valueLabel = new Text();
            valueLabel.getStyleClass().add("bisq-easy-open-trades-header-value");
            VBox.setMargin(descriptionLabel, new Insets(2, 0, 0, 0));
            VBox vBox = new VBox(2, descriptionLabel, valueLabel);
            vBox.setAlignment(Pos.CENTER_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(descriptionLabel, valueLabel, vBox);
        }

    }
}