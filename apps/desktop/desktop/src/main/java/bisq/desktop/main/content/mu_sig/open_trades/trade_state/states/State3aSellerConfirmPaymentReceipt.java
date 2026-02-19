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

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.mu_sig.MuSigTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class State3aSellerConfirmPaymentReceipt extends BaseState {
    private final Controller controller;

    public State3aSellerConfirmPaymentReceipt(ServiceProvider serviceProvider,
                                              MuSigTrade trade,
                                              MuSigOpenTradeChannel channel) {
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
            MuSigTrade trade = model.getTrade();

            Optional<AccountPayload<?>> accountPayload = trade.getMyself().getAccountPayload();
            Optional<Account<? extends PaymentMethod<?>, ?>> account = accountService.findAccount(accountPayload.orElseThrow());
            String accountName = account.orElseThrow().getAccountName();
            model.setMyAccountName(accountName);

            AccountPayload<?> peersAccountPayload = trade.getPeer().getAccountPayload().orElseThrow();
            model.setPaymentReason(peersAccountPayload.getReasonForPaymentString());

            if (model.getMarket().isBaseCurrencyBitcoin()) {
                String paymentReasonPart = model.getPaymentReason()
                        .map(e -> "\n" + Res.get("muSig.tradeState.info.phase3a.verifyReceipt.reasonForPayment", e))
                        .orElse("");
                model.setInfo(Res.get("muSig.tradeState.info.fiat.phase3a.verifyReceipt.account",
                        model.getMyAccountName(), paymentReasonPart));
            } else {
                model.setInfo(Res.get("muSig.tradeState.info.crypto.phase3a.verifyReceipt.account",
                        model.getNonBtcCurrencyCode(), peersAccountPayload.getAccountDataDisplayString()));
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onPaymentReceiptConfirmed() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase2b.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getFormattedNonBtcAmount()));
            muSigTradeService.paymentReceiptConfirmed(model.getTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        private String info;
        @Setter
        private String myAccountName;
        @Setter
        private Optional<String> paymentReason = Optional.empty();

        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final WrappingText headline;
        private final Button confirmPaymentReceiptButton;
        private final WrappingText info;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = MuSigFormUtils.getHeadline();
            info = MuSigFormUtils.getInfo();
            confirmPaymentReceiptButton = new Button();
            confirmPaymentReceiptButton.setDefaultButton(true);
            VBox.setMargin(confirmPaymentReceiptButton, new Insets(5, 0, 10, 0));
            root.getChildren().addAll(headline, info, confirmPaymentReceiptButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("muSig.tradeState.info.phase3a.headline", model.getFormattedNonBtcAmount()));
            info.setText(model.getInfo());
            confirmPaymentReceiptButton.setText(Res.get("muSig.tradeState.info.phase2b.fiatReceivedButton"));
            confirmPaymentReceiptButton.setOnAction(e -> controller.onPaymentReceiptConfirmed());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmPaymentReceiptButton.setOnAction(null);
        }
    }
}
