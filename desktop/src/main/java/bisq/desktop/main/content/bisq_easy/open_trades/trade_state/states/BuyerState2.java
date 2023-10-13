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
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BuyerState2 extends BaseState {
    private final Controller controller;

    public BuyerState2(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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

            model.getSendBtcAddressButtonDisabled().bind(model.getBtcAddress().isEmpty().or(model.getFiatPaymentConfirmed().not()));
            model.getFiatPaymentConfirmed().set(model.getBisqEasyTrade().getState() == BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getSendBtcAddressButtonDisabled().unbind();
        }

        private void onConfirmFiatSent() {
            model.getFiatPaymentConfirmed().set(true);
            sendSystemMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2a.systemMessage", model.getQuoteCode()));
            try {
                bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTrade());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }

        private void onSendBtcAddress() {
            sendSystemMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2b.systemMessage", model.getBtcAddress().get()));
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
        private final BooleanProperty fiatPaymentConfirmed = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton, sendBtcAddressButton, walletInfoButton;
        private final MaterialTextField btcAddress;
        private final MaterialTextArea account;
        private final MaterialTextField quoteAmount;
        private final WrappingText headline, btcAddressHeadline;
        private final WrappingText fiatSentConfirmed;
        private final HBox fiatSentConfirmedHBox, buttons;
        private Subscription fiatPaymentConfirmedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = FormUtils.getHeadline();

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatSentConfirmed = confirmPair.getFirst();
            fiatSentConfirmedHBox = confirmPair.getSecond();

            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.quoteAmount"), "", false);
            account = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2.sellersAccount"),
                    "", false);
            account.setHelpText(Res.get("bisqEasy.tradeState.info.buyer.phase2.reasonForPaymentInfo"));

            confirmFiatSentButton = new Button();
            confirmFiatSentButton.setDefaultButton(true);

            btcAddressHeadline = FormUtils.getHeadline();
            sendBtcAddressButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase2.sendBtcAddress"));
            sendBtcAddressButton.setDefaultButton(true);
            walletInfoButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase2.walletHelpButton"));
            walletInfoButton.getStyleClass().add("outlined-button");
            buttons = new HBox(10, sendBtcAddressButton, Spacer.fillHBox(), walletInfoButton);
            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress"), "", true);
            btcAddress.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.prompt"));
            btcAddress.setHelpText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.help"));

            VBox.setMargin(confirmFiatSentButton, new Insets(0, 0, 5, 0));
            VBox.setMargin(btcAddressHeadline, new Insets(5, 0, 0, 0));
            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    headline,
                    fiatSentConfirmedHBox,
                    quoteAmount,
                    account,
                    confirmFiatSentButton,
                    btcAddressHeadline, btcAddress, buttons);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.headline", model.getFormattedQuoteAmount()));
            quoteAmount.setText(model.getFormattedQuoteAmount());
            account.setText(model.getBisqEasyTrade().getPaymentAccountData().get());
            btcAddressHeadline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.headline"));
            confirmFiatSentButton.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.confirmFiatSent", model.getFormattedQuoteAmount()));

            btcAddress.textProperty().bindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().bind(model.getSendBtcAddressButtonDisabled());

            fiatPaymentConfirmedPin = EasyBind.subscribe(model.getFiatPaymentConfirmed(), fiatPaymentConfirmed -> {
                double dimmed = 0.15;
                if (fiatPaymentConfirmed) {
                    quoteAmount.setOpacity(2 * dimmed);
                    account.setOpacity(2 * dimmed);
                    btcAddressHeadline.setOpacity(1);
                    btcAddressHeadline.removeStyleClass("bisq-easy-trade-state-info");
                    btcAddressHeadline.addStyleClass("bisq-easy-trade-state-headline");
                    fiatSentConfirmed.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.fiatSentConfirmedCheckBox", model.getFormattedQuoteAmount()));
                } else {
                    btcAddressHeadline.setOpacity(dimmed);
                    btcAddressHeadline.removeStyleClass("bisq-easy-trade-state-headline");
                    btcAddressHeadline.addStyleClass("bisq-easy-trade-state-info");
                }
                fiatSentConfirmedHBox.setVisible(fiatPaymentConfirmed);
                fiatSentConfirmedHBox.setManaged(fiatPaymentConfirmed);
                headline.setVisible(!fiatPaymentConfirmed);
                headline.setManaged(!fiatPaymentConfirmed);
                confirmFiatSentButton.setVisible(!fiatPaymentConfirmed);
                confirmFiatSentButton.setManaged(!fiatPaymentConfirmed);
                btcAddress.setDisable(!fiatPaymentConfirmed);
            });

            confirmFiatSentButton.setOnAction(e -> controller.onConfirmFiatSent());
            sendBtcAddressButton.setOnAction(e -> controller.onSendBtcAddress());
            walletInfoButton.setOnAction(e -> controller.onOpenWalletHelp());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            btcAddress.textProperty().unbindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().unbind();

            fiatPaymentConfirmedPin.unsubscribe();

            confirmFiatSentButton.setOnAction(null);
            sendBtcAddressButton.setOnAction(null);
        }
    }
}