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

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
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

            model.getSendBtcAddressButtonDisabled().bind(model.getBtcAddress().isEmpty());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getSendBtcAddressButtonDisabled().unbind();
        }

        private void onSendBtcAddress() {
            //sendSystemMessage(Res.get("bisqEasy.tradeState.info.buyer.phase1a.systemMessage", model.getBtcAddress().get()));
            try {
                bisqEasyTradeService.buyerSendBtcAddress(model.getBisqEasyTrade(), model.getBtcAddress().get());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }

        void onOpenWalletHelp() {
            Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty btcAddress = new SimpleStringProperty();
        private final BooleanProperty sendBtcAddressButtonDisabled = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button sendBtcAddressButton, walletInfoButton;
        private final MaterialTextField btcAddress;

        private View(Model model, Controller controller) {
            super(model, controller);

            WrappingText btcAddressHeadline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.buyer.phase1a.btcAddress.headline"));
            VBox.setMargin(btcAddressHeadline, new Insets(5, 0, 0, 0));

            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase1a.btcAddress"), "", true);
            btcAddress.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase1a.btcAddress.prompt"));
            btcAddress.setHelpText(Res.get("bisqEasy.tradeState.info.buyer.phase1a.btcAddress.help"));

            sendBtcAddressButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.sendBtcAddress"));
            sendBtcAddressButton.setDefaultButton(true);
            walletInfoButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton"));
            walletInfoButton.getStyleClass().add("outlined-button");
            HBox buttons = new HBox(10, sendBtcAddressButton, Spacer.fillHBox(), walletInfoButton);

            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(btcAddressHeadline, btcAddress, buttons);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            btcAddress.textProperty().bindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().bind(model.getSendBtcAddressButtonDisabled());
            sendBtcAddressButton.setOnAction(e -> controller.onSendBtcAddress());
            walletInfoButton.setOnAction(e -> controller.onOpenWalletHelp());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            btcAddress.textProperty().unbindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().unbind();
            sendBtcAddressButton.setOnAction(null);
            walletInfoButton.setOnAction(null);
        }
    }
}
