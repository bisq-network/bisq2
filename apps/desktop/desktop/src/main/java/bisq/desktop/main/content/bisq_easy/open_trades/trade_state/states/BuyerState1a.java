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
import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
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

@Slf4j
public class BuyerState1a extends BaseState {
    private final Controller controller;

    public BuyerState1a(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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
            model.setBitcoinPaymentHeadline(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.headline." + name));
            model.setBitcoinPaymentDescription(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.description." + name));
            model.setBitcoinPaymentPrompt(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.prompt." + name));
            if (paymentRail == BitcoinPaymentRail.MAIN_CHAIN) {
                model.setBitcoinPaymentHelp(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp"));
            }
            model.getSendButtonDisabled().bind(model.getBitcoinPaymentData().isEmpty());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getSendButtonDisabled().unbind();
        }

        private void onSend() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.buyer.phase1a.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getBitcoinPaymentData().get()));
            bisqEasyTradeService.buyerSendBitcoinPaymentData(model.getBisqEasyTrade(), model.getBitcoinPaymentData().get());
        }

        void onOpenWalletHelp() {
            Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        private String bitcoinPaymentHeadline;
        @Setter
        private String bitcoinPaymentDescription;
        @Setter
        private String bitcoinPaymentPrompt;
        @Setter
        private String bitcoinPaymentHelp;
        private final StringProperty bitcoinPaymentData = new SimpleStringProperty();
        private final BooleanProperty sendButtonDisabled = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button sendButton, walletInfoButton;
        private final MaterialTextField bitcoinPayment;
        private final WrappingText bitcoinPaymentHeadline;

        private View(Model model, Controller controller) {
            super(model, controller);

            bitcoinPaymentHeadline = FormUtils.getHeadline();
            VBox.setMargin(bitcoinPaymentHeadline, new Insets(5, 0, 0, 0));

            bitcoinPayment = FormUtils.getTextField("", "", true);

            sendButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.send"));
            sendButton.setDefaultButton(true);
            walletInfoButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton"));
            walletInfoButton.getStyleClass().add("outlined-button");
            HBox buttons = new HBox(10, sendButton, Spacer.fillHBox(), walletInfoButton);

            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(bitcoinPaymentHeadline, bitcoinPayment, buttons);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            bitcoinPaymentHeadline.setText(model.getBitcoinPaymentHeadline());
            bitcoinPayment.setDescription(model.getBitcoinPaymentDescription());
            bitcoinPayment.setPromptText(model.getBitcoinPaymentPrompt());
            bitcoinPayment.setHelpText(model.getBitcoinPaymentHelp());

            bitcoinPayment.textProperty().bindBidirectional(model.getBitcoinPaymentData());

            bitcoinPayment.validate();
            sendButton.disableProperty().bind(model.getSendButtonDisabled());
            sendButton.setOnAction(e -> controller.onSend());
            walletInfoButton.setOnAction(e -> controller.onOpenWalletHelp());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            bitcoinPayment.textProperty().unbindBidirectional(model.getBitcoinPaymentData());
            sendButton.disableProperty().unbind();
            sendButton.setOnAction(null);
            walletInfoButton.setOnAction(null);
        }
    }
}
