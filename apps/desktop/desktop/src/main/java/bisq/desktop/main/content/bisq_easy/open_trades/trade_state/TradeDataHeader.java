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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.TradeCurrency;
import bisq.common.data.Triple;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final ReputationService reputationService;
        private final UserIdentityService userIdentityService;
        private final BisqEasyTradeService bisqEasyTradeService;
        private Subscription channelPin;

        private Controller(ServiceProvider serviceProvider, String peerDescription) {
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
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
                    return;
                }
                Optional<BisqEasyTrade> optionalBisqEasyTrade = bisqEasyTradeService.findTrade(channel.getTradeId());
                if (optionalBisqEasyTrade.isEmpty()) {
                    return;
                }

                BisqEasyTrade bisqEasyTrade = optionalBisqEasyTrade.get();
                model.getBisqEasyTrade().set(bisqEasyTrade);

                UserProfile peerUserProfile = channel.getPeer();
                model.getReputationScore().set(reputationService.findReputationScore(peerUserProfile).orElse(ReputationScore.NONE));
                model.getPeersUserProfile().set(peerUserProfile);
                model.getTradeId().set(bisqEasyTrade.getShortId());

                long baseSideAmount = bisqEasyTrade.getContract().getBaseSideAmount();
                long quoteSideAmount = bisqEasyTrade.getContract().getQuoteSideAmount();
                Coin baseAmount = Coin.asBtcFromValue(baseSideAmount);
                String baseAmountString = AmountFormatter.formatBaseAmount(baseAmount);
                Monetary quoteAmount = Fiat.from(quoteSideAmount, bisqEasyTrade.getOffer().getMarket().getQuoteCurrencyCode());
                String quoteAmountString = AmountFormatter.formatQuoteAmount(quoteAmount);
                if (bisqEasyTrade.isSeller()) {
                    model.getDirection().set(Res.get("offer.sell").toUpperCase());
                    model.getLeftAmountDescription().set(Res.get("bisqEasy.tradeState.header.send").toUpperCase());
                    model.getLeftAmount().set(baseAmountString);
                    model.getLeftCode().set(baseAmount.getCode());
                    model.getRightAmountDescription().set(Res.get("bisqEasy.tradeState.header.receive").toUpperCase());
                    model.getRightAmount().set(quoteAmountString);
                    model.getRightCode().set(quoteAmount.getCode());
                } else {
                    model.getDirection().set(Res.get("offer.buy").toUpperCase());
                    model.getLeftAmountDescription().set(Res.get("bisqEasy.tradeState.header.pay").toUpperCase());
                    model.getLeftAmount().set(quoteAmountString);
                    model.getLeftCode().set(quoteAmount.getCode());
                    model.getRightAmountDescription().set(Res.get("bisqEasy.tradeState.header.receive").toUpperCase());
                    model.getRightAmount().set(baseAmountString);
                    model.getRightCode().set(baseAmount.getCode());
                }
            });
        }

        @Override
        public void onDeactivate() {
            channelPin.unsubscribe();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final String peerDescription;

        private final ObjectProperty<BisqEasyOpenTradeChannel> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<BisqEasyTrade> bisqEasyTrade = new SimpleObjectProperty<>();
        private final ObjectProperty<UserProfile> peersUserProfile = new SimpleObjectProperty<>();
        private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
        private final StringProperty direction = new SimpleStringProperty();
        private final StringProperty leftAmountDescription = new SimpleStringProperty();
        private final StringProperty leftAmount = new SimpleStringProperty();
        private final StringProperty leftCode = new SimpleStringProperty();
        private final StringProperty rightAmountDescription = new SimpleStringProperty();
        private final StringProperty rightAmount = new SimpleStringProperty();
        private final StringProperty rightCode = new SimpleStringProperty();
        private final StringProperty tradeId = new SimpleStringProperty();

        public Model(String peerDescription) {
            this.peerDescription = peerDescription;
        }
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final static double HEIGHT = 61;

        private final Triple<Text, Text, VBox> direction, tradeId;
        private final UserProfileDisplay peersUserProfileDisplay;
        private final Label peerDescription;
        private final Triple<Triple<Text, Node, Text>, HBox, VBox> leftAmount, rightAmount;
        private Subscription userProfilePin, reputationScorePin;
        private ChangeListener<Object> layoutBoundsListener;
        private final BitcoinAmountDisplay leftBtcText, rightBtcText;
        private final List<ChangeListener<String>> amountAndCodeListeners = new ArrayList<>();

        private View(Model model, Controller controller) {
            super(new HBox(40), model, controller);

            root.setMinHeight(HEIGHT);
            root.setMaxHeight(HEIGHT);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(0, 30, 0, 30));
            root.getStyleClass().add("chat-container-header");

            peerDescription = new Label();
            peerDescription.getStyleClass().add("bisq-easy-open-trades-header-description");
            peersUserProfileDisplay = new UserProfileDisplay(25);
            peersUserProfileDisplay.setPadding(new Insets(0, -15, 0, 0));
            peersUserProfileDisplay.setMinWidth(140);
            peersUserProfileDisplay.setMaxWidth(140);
            VBox peerVBox = new VBox(2, peerDescription, peersUserProfileDisplay);
            peerVBox.setAlignment(Pos.CENTER_LEFT);

            direction = getElements(Res.get("bisqEasy.tradeState.header.direction"));
            leftAmount = getAmountElements();
            rightAmount = getAmountElements();
            tradeId = getElements(Res.get("bisqEasy.tradeState.header.tradeId"));

            direction.getThird().setTranslateY(8);
            leftAmount.getThird().setTranslateY(8);
            rightAmount.getThird().setTranslateY(8);
            tradeId.getThird().setTranslateY(8);

            StackPane leftAmountPane = (StackPane) leftAmount.getFirst().getSecond();
            leftBtcText = (BitcoinAmountDisplay) leftAmountPane.getChildren().getFirst();
            configureBitcoinAmountDisplay(leftBtcText);

            StackPane rightAmountPane = (StackPane) rightAmount.getFirst().getSecond();
            rightBtcText = (BitcoinAmountDisplay) rightAmountPane.getChildren().getFirst();
            configureBitcoinAmountDisplay(rightBtcText);

            root.getChildren().addAll(peerVBox,
                    direction.getThird(),
                    leftAmount.getThird(),
                    rightAmount.getThird(),
                    tradeId.getThird());
        }

        private void configureBitcoinAmountDisplay(BitcoinAmountDisplay btcText) {
            btcText.setFontSize(18);
            btcText.setBtcCodeFontSize(15);
            btcText.setBaselineAlignment();
            btcText.setHeightConstraints(28, 28);
        }

        public void refreshBitcoinAmountDisplayComponents() {
            updateAmountDisplayWithCurrency(
                    leftAmount,
                    leftBtcText,
                    model.getLeftAmount().get(),
                    model.getLeftCode().get(),
                    false);

            updateAmountDisplayWithCurrency(
                    rightAmount,
                    rightBtcText,
                    model.getRightAmount().get(),
                    model.getRightCode().get(),
                    false);
        }

        private void updateAmountDisplayWithCurrency(
                Triple<Triple<Text, Node, Text>, HBox, VBox> amountComponents,
                BitcoinAmountDisplay btcText,
                String amount,
                String currencyCode,
                boolean updateCodeText) {

            boolean isBtc = TradeCurrency.isBtc(currencyCode);
            updateAmountDisplay(amountComponents, btcText, amount, isBtc);

            if (updateCodeText && !isBtc) {
                Text codeText = amountComponents.getFirst().getThird();
                codeText.setText(currencyCode);
            }
        }

        private void updateAmountDisplay(
                Triple<Triple<Text, Node, Text>, HBox, VBox> amountComponents,
                BitcoinAmountDisplay btcText,
                String amount,
                boolean isBtc) {

            StackPane amountPane = (StackPane) amountComponents.getFirst().getSecond();
            Text regularText = (Text) amountPane.getChildren().get(1);
            Text codeText = amountComponents.getFirst().getThird();
            HBox container = amountComponents.getSecond();

            btcText.setVisible(isBtc);
            btcText.setManaged(isBtc);
            regularText.setVisible(!isBtc);
            regularText.setManaged(!isBtc);
            codeText.setVisible(!isBtc);

            if (isBtc) {
                btcText.setBtcAmount(amount);
                configureBitcoinAmountDisplay(btcText);
                container.setTranslateY(0);
            } else {
                regularText.setText(amount);
                HBox.setMargin(codeText, new Insets(0, 0, 0, 5));
            }

            container.requestLayout();
        }

        @Override
        protected void onViewAttached() {
            peerDescription.setText(model.getPeerDescription());

            direction.getSecond().textProperty().bind(model.getDirection());

            tradeId.getSecond().textProperty().bind(model.getTradeId());

            setupAmountDisplay(
                    leftAmount,
                    leftBtcText,
                    model.getLeftAmountDescription(),
                    model.getLeftAmount(),
                    model.getLeftCode()
            );

            setupAmountDisplay(
                    rightAmount,
                    rightBtcText,
                    model.getRightAmountDescription(),
                    model.getRightAmount(),
                    model.getRightCode()
            );

            userProfilePin = EasyBind.subscribe(model.getPeersUserProfile(), peersUserProfileDisplay::setUserProfile);
            reputationScorePin = EasyBind.subscribe(model.getReputationScore(), peersUserProfileDisplay::setReputationScore);

            layoutBoundsListener =
                    (obs, old, newVal) -> refreshBitcoinAmountDisplayComponents();
            root.layoutBoundsProperty().addListener(layoutBoundsListener);

            refreshBitcoinAmountDisplayComponents();
        }

        private void setupAmountDisplay(
                Triple<Triple<Text, Node, Text>, HBox, VBox> amountComponents,
                BitcoinAmountDisplay btcText,
                StringProperty descriptionProperty,
                StringProperty amountProperty,
                StringProperty codeProperty) {

            Text descriptionLabel = amountComponents.getFirst().getFirst();
            descriptionLabel.textProperty().bind(descriptionProperty);

            ChangeListener<String> amountListener = (obs, old, newVal) -> {
                if (newVal != null) {
                    updateAmountDisplayWithCurrency(
                            amountComponents,
                            btcText,
                            newVal,
                            codeProperty.get(),
                            false);
                }
            };
            amountProperty.addListener(amountListener);
            amountAndCodeListeners.add(amountListener);

            ChangeListener<String> codeListener =
                    (obs, old, newVal) ->
                        updateAmountDisplayWithCurrency(
                            amountComponents,
                            btcText,
                            amountProperty.get(),
                            newVal,
                            true);
            codeProperty.addListener(codeListener);
            amountAndCodeListeners.add(codeListener);

            // Initial setup
            updateAmountDisplayWithCurrency(
                    amountComponents,
                    btcText,
                    amountProperty.get(),
                    codeProperty.get(),
                    true);
        }

        @Override
        protected void onViewDetached() {
            direction.getSecond().textProperty().unbind();
            leftAmount.getFirst().getFirst().textProperty().unbind();
            rightAmount.getFirst().getFirst().textProperty().unbind();
            tradeId.getSecond().textProperty().unbind();

            for (ChangeListener<String> listener : amountAndCodeListeners) {
                model.getLeftAmount().removeListener(listener);
                model.getRightAmount().removeListener(listener);
                model.getLeftCode().removeListener(listener);
                model.getRightCode().removeListener(listener);
            }
            amountAndCodeListeners.clear();

            userProfilePin.unsubscribe();
            reputationScorePin.unsubscribe();

            if (layoutBoundsListener != null) {
                root.layoutBoundsProperty().removeListener(layoutBoundsListener);
            }

            peersUserProfileDisplay.dispose();
        }

        private Triple<Text, Text, VBox> getElements() {
            return getElements(null);
        }

        private Triple<Text, Text, VBox> getElements(@Nullable String description) {
            Text descriptionLabel = description == null ? new Text() : new Text(description.toUpperCase());
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
            Text valueLabel = new Text();
            valueLabel.getStyleClass().add("bisq-easy-open-trades-header-value");
            VBox.setMargin(descriptionLabel, new Insets(0, 0, 5, 0));
            VBox vBox = new VBox(descriptionLabel, valueLabel);
            vBox.setAlignment(Pos.TOP_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(descriptionLabel, valueLabel, vBox);
        }

        private Triple<Triple<Text, Node, Text>, HBox, VBox> getAmountElements() {
            Text descriptionLabel = new Text();
            descriptionLabel.getStyleClass().add("bisq-easy-open-trades-header-description");
            BitcoinAmountDisplay btcAmountText = new BitcoinAmountDisplay("0");
            configureBitcoinAmountDisplay(btcAmountText);
            btcAmountText.setFocusTraversable(false);
            Text regularAmountText = new Text();
            regularAmountText.getStyleClass().add("bisq-easy-open-trades-header-value");
            regularAmountText.setFocusTraversable(false);
            StackPane amountPane = new StackPane(btcAmountText, regularAmountText);
            amountPane.setFocusTraversable(false);
            amountPane.setMouseTransparent(false);
            amountPane.setAlignment(Pos.BASELINE_LEFT);
            amountPane.setMinHeight(18);
            amountPane.setPrefHeight(18);
            amountPane.setMaxHeight(18);

            Text code = new Text();
            code.getStyleClass().add("bisq-easy-open-trades-header-code");
            code.setFocusTraversable(false);
            code.setMouseTransparent(true);

            HBox hBox = new HBox(amountPane, code);
            hBox.setAlignment(Pos.BASELINE_LEFT);

            VBox.setMargin(descriptionLabel, new Insets(0, 0, 5, 0));

            VBox vBox = new VBox(descriptionLabel, hBox);
            vBox.setFillWidth(true);
            vBox.setAlignment(Pos.TOP_LEFT);
            vBox.setMinHeight(HEIGHT);
            vBox.setMaxHeight(HEIGHT);
            return new Triple<>(new Triple<>(descriptionLabel, amountPane, code), hBox, vBox);
        }
    }
}