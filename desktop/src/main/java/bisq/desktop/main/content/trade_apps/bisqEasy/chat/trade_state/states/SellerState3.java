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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SellerState3 extends BaseState {
    private final Controller controller;

    public SellerState3(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
            model.setBtcAddress(model.getBisqEasyTrade().getBtcAddress().get());
            model.getButtonDisabled().bind(model.getTxId().isEmpty().or(model.getFiatReceived().not()));

            model.getFiatReceived().set(model.getBisqEasyTrade().getState() == BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            model.getButtonDisabled().unbind();
        }

        private void onConfirmFiatReceipt() {
            model.getFiatReceived().set(true);
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3a.chatBotMessage", model.getFormattedQuoteAmount()));
            try {
                bisqEasyTradeService.sellerConfirmFiatReceipt(model.getBisqEasyTrade());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }

        private void onBtcSent() {
            String txId = model.getTxId().get();
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3b.chatBotMessage", txId));
            try {
                bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), txId);
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        private final StringProperty txId = new SimpleStringProperty();
        private final BooleanProperty buttonDisabled = new SimpleBooleanProperty();
        private final BooleanProperty fiatReceived = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button fiatReceivedButton, btcSentButton;
        private final MaterialTextField txId;
        private final BisqText infoHeadline;
        private final Label sendBtcLabel;
        private final MaterialTextField baseAmount;
        private final MaterialTextField btcAddress;
        private final CheckBox fiatPaymentReceivedCheckBox;
        private Subscription fiatReceivedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            infoHeadline = new BisqText();
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            fiatReceivedButton = new Button();
            fiatReceivedButton.setDefaultButton(true);

            fiatPaymentReceivedCheckBox = new CheckBox();
            fiatPaymentReceivedCheckBox.setMouseTransparent(true);
            fiatPaymentReceivedCheckBox.setSelected(true);

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.txId"), "", true);
            txId.textProperty().bindBidirectional(model.getTxId());
            txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3.txId.prompt"));

            btcSentButton = new Button();
            btcSentButton.setDefaultButton(true);

            sendBtcLabel = FormUtils.getLabel("");
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.baseAmount"), "", false);
            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.btcAddress"), "", false);
            VBox.setMargin(fiatPaymentReceivedCheckBox, new Insets(5, 0, 10, 0));
            VBox.setMargin(fiatReceivedButton, new Insets(10, 0, 10, 0));
            VBox.setMargin(btcSentButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    infoHeadline,
                    fiatReceivedButton, fiatPaymentReceivedCheckBox,
                    sendBtcLabel,
                    baseAmount,
                    btcAddress,
                    txId,
                    btcSentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            infoHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.headline", model.getFormattedQuoteAmount()));

            baseAmount.setText(model.getFormattedBaseAmount());
            btcAddress.setText(model.getBtcAddress());
            fiatReceivedButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.fiatReceivedButton", model.getFormattedQuoteAmount()));
            btcSentButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.btcSentButton", model.getFormattedBaseAmount()));

            txId.textProperty().bindBidirectional(model.getTxId());

            fiatReceivedPin = EasyBind.subscribe(model.getFiatReceived(), fiatPaymentConfirmed -> {
                infoHeadline.setVisible(!fiatPaymentConfirmed);
                infoHeadline.setManaged(!fiatPaymentConfirmed);
                fiatReceivedButton.setVisible(!fiatPaymentConfirmed);
                fiatReceivedButton.setManaged(!fiatPaymentConfirmed);
                fiatPaymentReceivedCheckBox.setVisible(fiatPaymentConfirmed);
                fiatPaymentReceivedCheckBox.setManaged(fiatPaymentConfirmed);

                baseAmount.setDisable(!fiatPaymentConfirmed);
                btcAddress.setDisable(!fiatPaymentConfirmed);
                txId.setDisable(!fiatPaymentConfirmed);

                double dimmed = 0.15;
                if (fiatPaymentConfirmed) {
                    fiatPaymentReceivedCheckBox.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.fiatPaymentReceivedCheckBox", model.getFormattedQuoteAmount()));

                    VBox.setMargin(sendBtcLabel, new Insets(0, 0, 5, 0));
                    sendBtcLabel.setOpacity(1);
                    sendBtcLabel.setText(Res.get("bisqEasy.tradeState.info.seller.phase3b.sendBtc", model.getFormattedBaseAmount()));
                    sendBtcLabel.getStyleClass().remove("bisq-easy-trade-state-info-text");
                    sendBtcLabel.getStyleClass().add("bisq-easy-trade-state-info-headline");

                } else {
                    sendBtcLabel.setOpacity(dimmed);
                    sendBtcLabel.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.sendBtc", model.getQuoteCode(), model.getFormattedBaseAmount()));
                    sendBtcLabel.getStyleClass().add("bisq-easy-trade-state-info-text");
                    sendBtcLabel.getStyleClass().remove("bisq-easy-trade-state-info-headline");
                }
            });

            btcSentButton.disableProperty().bind(model.getButtonDisabled());

            fiatReceivedButton.setOnAction(e -> controller.onConfirmFiatReceipt());
            btcSentButton.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            txId.textProperty().unbindBidirectional(model.getTxId());

            fiatReceivedPin.unsubscribe();

            btcSentButton.disableProperty().unbind();

            fiatReceivedButton.setOnAction(null);
            btcSentButton.setOnAction(null);
        }
    }
}