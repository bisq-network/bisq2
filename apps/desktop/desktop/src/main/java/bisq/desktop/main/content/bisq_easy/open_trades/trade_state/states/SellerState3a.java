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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeProtocolException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState3a extends BaseState {
    private final Controller controller;

    public SellerState3a(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onBtcSent() {
            String txId = model.getTxId().get();
            sendSystemMessage(Res.get("bisqEasy.tradeState.info.seller.phase3a.systemMessage", txId));
            try {
                bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), txId);
            } catch (TradeProtocolException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        private final StringProperty txId = new SimpleStringProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button btcSentButton;
        private final MaterialTextField txId;
        private final WrappingText sendBtcHeadline, fiatReceiptConfirmed;
        private final MaterialTextField baseAmount;
        private final MaterialTextField btcAddress;

        private View(Model model, Controller controller) {
            super(model, controller);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatReceiptConfirmed = confirmPair.getFirst();
            HBox fiatReceiptConfirmedHBox = confirmPair.getSecond();

            sendBtcHeadline = FormUtils.getHeadline();
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.baseAmount"), "", false);
            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.btcAddress"), "", false);
            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.txId"), "", true);
            txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3a.txId.prompt"));
            btcSentButton = new Button();
            btcSentButton.setDefaultButton(true);

            VBox.setMargin(fiatReceiptConfirmedHBox, new Insets(0, 0, 5, 0));
            VBox.setMargin(btcSentButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    fiatReceiptConfirmedHBox,
                    sendBtcHeadline,
                    baseAmount,
                    btcAddress,
                    txId,
                    btcSentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            baseAmount.setText(model.getFormattedBaseAmount());
            btcAddress.setText(model.getBtcAddress());
            btcSentButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.btcSentButton", model.getFormattedBaseAmount()));
            txId.textProperty().bindBidirectional(model.getTxId());
            sendBtcHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.sendBtc", model.getFormattedBaseAmount()));
            fiatReceiptConfirmed.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.fiatPaymentReceivedCheckBox", model.getFormattedQuoteAmount()));
            btcSentButton.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            txId.textProperty().unbindBidirectional(model.getTxId());
            btcSentButton.setOnAction(null);
        }
    }
}
