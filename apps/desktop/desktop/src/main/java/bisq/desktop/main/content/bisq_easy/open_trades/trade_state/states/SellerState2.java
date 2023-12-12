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
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SellerState2 extends BaseState {
    private final Controller controller;

    public SellerState2(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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

            model.getFiatSendConfirmationReceived().set(model.getBisqEasyTrade().getState() == BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final BooleanProperty fiatSendConfirmationReceived = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {

        private final WrappingText fiatSentConfirmed, headline, info;
        private final WaitingAnimation waitingAnimation;
        private final HBox fiatSentConfirmedHBox;
        private Subscription fiatSendConfirmationReceivedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatSentConfirmed = confirmPair.getFirst();
            fiatSentConfirmedHBox = confirmPair.getSecond();

            VBox.setMargin(fiatSentConfirmedHBox, new Insets(0, 0, 10, 0));

            waitingAnimation = new WaitingAnimation();
            headline = FormUtils.getHeadline();
            info = FormUtils.getInfo();
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            root.getChildren().addAll(fiatSentConfirmedHBox, waitingInfo);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            fiatSendConfirmationReceivedPin = EasyBind.subscribe(model.getFiatSendConfirmationReceived(), fiatPaymentConfirmed -> {
                fiatSentConfirmedHBox.setVisible(fiatPaymentConfirmed);
                fiatSentConfirmedHBox.setManaged(fiatPaymentConfirmed);
                if (fiatPaymentConfirmed) {
                    VBox.setMargin(waitingAnimation, new Insets(20, 0, 10, 40));
                    headline.setText(Res.get("bisqEasy.tradeState.info.seller.phase2.waitForBtcAddress.headline"));
                    info.setText(Res.get("bisqEasy.tradeState.info.seller.phase2.waitForBtcAddress.info"));
                    fiatSentConfirmed.setText(Res.get("bisqEasy.tradeState.info.seller.phase2.fiatPaymentSentCheckBox", model.getFormattedQuoteAmount()));
                    waitingAnimation.setState(WaitingState.BITCOIN_ADDRESS);
                } else {
                    VBox.setMargin(waitingAnimation, new Insets(30, 0, 10, 40));
                    headline.setText(Res.get("bisqEasy.tradeState.info.seller.phase2.waitForPayment.headline", model.getQuoteCode()));
                    info.setText(Res.get("bisqEasy.tradeState.info.seller.phase2.waitForPayment.info", model.getFormattedQuoteAmount()));
                    waitingAnimation.setState(WaitingState.FIAT_PAYMENT);
                }
            });

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
            fiatSendConfirmationReceivedPin.unsubscribe();
            waitingAnimation.stop();
        }
    }
}