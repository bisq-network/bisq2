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

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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

            BitcoinPaymentRail paymentRail = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            String name = paymentRail.name();
            model.setBitcoinPaymentDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.bitcoinPayment.description." + name));
            model.setPaymentProofDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.description." + name));
            model.setPaymentProofPrompt(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.prompt." + name));

            model.setBitcoinPaymentData(model.getBisqEasyTrade().getBitcoinPaymentData().get());
            if (paymentRail == BitcoinPaymentRail.ONCHAIN) {
                model.getBtcSentButtonDisabled().bind(model.getPaymentProof().isEmpty());
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getBtcSentButtonDisabled().unbind();
        }

        private void onBtcSent() {
            String name = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail().name();
            String proof = Res.get("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.paymentProof." + name);
            String userName = model.getChannel().getMyUserIdentity().getUserName();
            if (model.getPaymentProof().get() == null) {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.noProofProvided", userName));
            } else {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage",
                        userName, proof, model.getPaymentProof().get()));
            }
            bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), Optional.ofNullable(model.getPaymentProof().get()));
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String bitcoinPaymentData;
        private final StringProperty paymentProof = new SimpleStringProperty();
        private final BooleanProperty btcSentButtonDisabled = new SimpleBooleanProperty();
        @Setter
        private String bitcoinPaymentDescription;
        @Setter
        private String paymentProofDescription;
        @Setter
        private String paymentProofPrompt;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button sentButton;
        private final MaterialTextField paymentProof;
        private final WrappingText sendBtcHeadline, fiatReceiptConfirmed;
        private final MaterialTextField baseAmount;
        private final MaterialTextField bitcoinPayment;

        private View(Model model, Controller controller) {
            super(model, controller);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatReceiptConfirmed = confirmPair.getFirst();
            HBox fiatReceiptConfirmedHBox = confirmPair.getSecond();

            sendBtcHeadline = FormUtils.getHeadline();
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.baseAmount"), "", false);
            bitcoinPayment = FormUtils.getTextField("", "", false);
            paymentProof = FormUtils.getTextField("", "", true);
            sentButton = new Button();
            sentButton.setDefaultButton(true);

            VBox.setMargin(fiatReceiptConfirmedHBox, new Insets(0, 0, 5, 0));
            VBox.setMargin(sentButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    fiatReceiptConfirmedHBox,
                    sendBtcHeadline,
                    baseAmount,
                    bitcoinPayment,
                    paymentProof,
                    sentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            baseAmount.setText(model.getFormattedBaseAmount());
            baseAmount.getIconButton().setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBaseAmount()));
            bitcoinPayment.setDescription(model.getBitcoinPaymentDescription());
            bitcoinPayment.setText(model.getBitcoinPaymentData());
            sentButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.btcSentButton", model.getFormattedBaseAmount()));
            paymentProof.textProperty().bindBidirectional(model.getPaymentProof());
            paymentProof.setDescription(model.getPaymentProofDescription());
            paymentProof.setPromptText(model.getPaymentProofPrompt());
            sendBtcHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.sendBtc", model.getFormattedBaseAmount()));
            fiatReceiptConfirmed.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.fiatPaymentReceivedCheckBox", model.getFormattedQuoteAmount()));
            sentButton.setOnAction(e -> controller.onBtcSent());
            sentButton.disableProperty().bind(model.getBtcSentButtonDisabled());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            paymentProof.textProperty().unbindBidirectional(model.getPaymentProof());
            sentButton.setOnAction(null);
            baseAmount.getIconButton().setOnAction(null);
            sentButton.disableProperty().unbind();
        }
    }
}
