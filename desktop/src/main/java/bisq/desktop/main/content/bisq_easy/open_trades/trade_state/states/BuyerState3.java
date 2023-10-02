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
public class BuyerState3 extends BaseState {
    private final Controller controller;

    public BuyerState3(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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
            model.getFiatReceiptConfirmed().set(model.getBisqEasyTrade().getState() == BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final BooleanProperty fiatReceiptConfirmed = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final WrappingText headline, info;
        private final WaitingAnimation waitingAnimation;
        private final WrappingText fiatReceiptConfirmed;
        private final HBox fiatReceiptConfirmedHBox;
        private Subscription fiatReceiptConfirmedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatReceiptConfirmed = confirmPair.getFirst();
            fiatReceiptConfirmedHBox = confirmPair.getSecond();

            VBox.setMargin(fiatReceiptConfirmedHBox, new Insets(0, 0, 5, 0));

            waitingAnimation = new WaitingAnimation();
            headline = FormUtils.getHeadline();
            info = FormUtils.getInfo();
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            root.getChildren().addAll(fiatReceiptConfirmedHBox, waitingInfo);
            root.setSpacing(20);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            fiatReceiptConfirmedPin = EasyBind.subscribe(model.getFiatReceiptConfirmed(), fiatPaymentConfirmed -> {
                fiatReceiptConfirmedHBox.setVisible(fiatPaymentConfirmed);
                fiatReceiptConfirmedHBox.setManaged(fiatPaymentConfirmed);
                if (fiatPaymentConfirmed) {
                    headline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3b.headline"));
                    info.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3b.info"));
                    fiatReceiptConfirmed.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3.fiatReceiptConfirmedCheckBox", model.getFormattedQuoteAmount()));
                    waitingAnimation.setState(WaitingState.BITCOIN_PAYMENT);
                } else {
                    headline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3a.headline"));
                    info.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3a.info", model.getFormattedQuoteAmount()));
                    waitingAnimation.setState(WaitingState.FIAT_PAYMENT_CONFIRMATION);
                }
            });

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
            waitingAnimation.stop();

            fiatReceiptConfirmedPin.unsubscribe();
        }
    }
}