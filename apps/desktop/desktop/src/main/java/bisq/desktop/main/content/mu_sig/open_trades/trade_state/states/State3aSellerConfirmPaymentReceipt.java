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

package bisq.desktop.main.content.mu_sig.open_trades.trade_state.states;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.mu_sig.MuSigTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class State3aSellerConfirmPaymentReceipt extends BaseState {
    private final Controller controller;

    public State3aSellerConfirmPaymentReceipt(ServiceProvider serviceProvider, MuSigTrade trade, MuSigOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, trade, channel);
    }

    public VBox getRoot() {
        return controller.getView().getRoot();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(serviceProvider, trade, channel);
        }

        @Override
        protected Model createModel(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            return new Model(trade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onPaymentReceiptConfirmed() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase2b.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getFormattedQuoteAmount()));
            muSigTradeService.paymentReceiptConfirmed(model.getTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final WrappingText headline;
        private final Button confirmPaymentReceiptButton;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = MuSigFormUtils.getHeadline();
            WrappingText info = MuSigFormUtils.getInfo(Res.get("bisqEasy.tradeState.info.seller.phase2b.info"));
            confirmPaymentReceiptButton = new Button();
            confirmPaymentReceiptButton.setDefaultButton(true);
            VBox.setMargin(confirmPaymentReceiptButton, new Insets(5, 0, 10, 0));
            root.getChildren().addAll(headline, info, confirmPaymentReceiptButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("bisqEasy.tradeState.info.seller.phase2b.headline", model.getFormattedQuoteAmount(), model.getTrade().getShortId()));
            confirmPaymentReceiptButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase2b.fiatReceivedButton", model.getFormattedQuoteAmount()));
            confirmPaymentReceiptButton.setOnAction(e -> controller.onPaymentReceiptConfirmed());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmPaymentReceiptButton.setOnAction(null);
        }
    }
}
