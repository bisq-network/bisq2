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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.TradeException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState3 extends BaseState {
    private final Controller controller;

    public SellerState3(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, NetworkId takerNetworkId, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(applicationService, bisqEasyOffer, takerNetworkId, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, NetworkId takerNetworkId, BisqEasyPrivateTradeChatChannel channel) {
            super(applicationService, bisqEasyOffer, takerNetworkId, channel);
        }

        @Override
        protected Model createModel() {
            return new Model();
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
            model.setBtcAddress(model.getBisqEasyTradeModel().getBuyer().getBtcAddress().get());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onBtcSent() {
            String txId = model.getTxId().get();
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3.chatBotMessage", txId));
            try {
                bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTradeModel(), txId);
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
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField txId;
        private final BisqText infoHeadline;
        private final Label sendBtcLabel;
        private final MaterialTextField baseAmount;
        private final MaterialTextField btcAddress;

        private View(Model model, Controller controller) {
            super(model, controller);

            infoHeadline = new BisqText();
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.txId"), "", true);
            txId.textProperty().bindBidirectional(model.getTxId());
            txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3.txId.prompt"));

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase3.buttonText"));
            button.setDefaultButton(true);

            sendBtcLabel = FormUtils.getLabel("");
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.baseAmount"), "", false);
            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.btcAddress"), "", false);
            VBox.setMargin(button, new Insets(5, 0, 0, 0));
            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    sendBtcLabel,
                    baseAmount,
                    btcAddress,
                    txId,
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            infoHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.headline", model.getFormattedQuoteAmount()));
            sendBtcLabel.setText(Res.get("bisqEasy.tradeState.info.seller.phase3.sendBtc", model.getQuoteCode(), model.getFormattedBaseAmount()));
            baseAmount.setText(model.getFormattedBaseAmount());
            btcAddress.setText(model.getBtcAddress());

            txId.textProperty().bindBidirectional(model.getTxId());
            button.disableProperty().bind(txId.textProperty().isEmpty());
            button.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            txId.textProperty().unbindBidirectional(model.getTxId());
            button.disableProperty().unbind();
            button.setOnAction(null);
        }
    }
}