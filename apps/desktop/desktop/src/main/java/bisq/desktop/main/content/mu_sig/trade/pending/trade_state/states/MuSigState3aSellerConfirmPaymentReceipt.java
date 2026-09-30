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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.TradeExceptionHandler;
import bisq.desktop.components.controls.MaterialTextArea;
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
public class MuSigState3aSellerConfirmPaymentReceipt extends MuSigBaseState {
    private final Controller controller;

    public MuSigState3aSellerConfirmPaymentReceipt(ServiceProvider serviceProvider,
                                                   MuSigTrade trade,
                                                   MuSigOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, trade, channel);
    }

    public VBox getRoot() {
        return controller.getView().getRoot();
    }

    private static class Controller extends MuSigBaseState.Controller<Model, View> {
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

            AccountPayload<?> peersAccountPayload = trade.getPeer().getAccountPayload().orElseThrow();
            if (peersAccountPayload instanceof CryptoAssetAccountPayload cryptoAssetAccountPayload) {
                model.setVerifyReceiptInfo(Res.get("muSig.trade.state.phase3a.verifyReceipt.account.crypto",
                        model.getNonBtcCurrencyCode(), cryptoAssetAccountPayload.getAddress()));
                model.setPeersAccountDataDescription("");
                model.setBuyersAccountData("");
                model.setFiatAccount(false);
            } else {
                String accountName = account
                        .map(Account::getAccountName)
                        .orElse(Res.get("data.na"));
                model.setVerifyReceiptInfo(Res.get("muSig.trade.state.phase3a.verifyReceipt.account.fiat", accountName));
                model.setPeersAccountDataDescription(Res.get("muSig.trade.state.phase3a.buyersAccount.fiat"));
                model.setBuyersAccountData(peersAccountPayload.getAccountDataDisplayString());
                model.setFiatAccount(true);
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onPaymentReceiptConfirmed() {
            if (TradeExceptionHandler.run(() -> muSigTradeService.paymentReceiptConfirmed(model.getTrade()))) {
                sendTradeLogMessage(Res.encode("muSig.trade.state.phase3a.logMessage",
                        model.getChannel().getMyUserIdentity().getUserName(), model.getFormattedNonBtcAmount()));
            }
        }
    }

    @Getter
    private static class Model extends MuSigBaseState.Model {
        @Setter
        private String verifyReceiptInfo;
        @Setter
        private String buyersAccountData;
        @Setter
        private String peersAccountDataDescription;
        @Setter
        private boolean isFiatAccount;

        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
        }
    }

    public static class View extends MuSigBaseState.View<Model, Controller> {
        private final WrappingText headline;
        private final Button confirmPaymentReceiptButton;
        private final WrappingText verifyBuyersAccountInfo, verifyReceiptInfo;
        private final MaterialTextArea buyersAccountData;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = MuSigFormUtils.getHeadline();
            verifyBuyersAccountInfo = MuSigFormUtils.getInfo(Res.get("muSig.trade.state.phase3a.verifyBuyersAccount.fiat"));
            verifyReceiptInfo = MuSigFormUtils.getInfo();
            buyersAccountData = MuSigFormUtils.addTextArea("", "", false);

            confirmPaymentReceiptButton = new Button();
            confirmPaymentReceiptButton.setDefaultButton(true);

            VBox.setMargin(verifyBuyersAccountInfo, new Insets(10, 0, 0, 0));
            VBox.setMargin(verifyReceiptInfo, new Insets(10, 0, 0, 0));
            VBox.setMargin(confirmPaymentReceiptButton, new Insets(5, 0, 10, 0));
            root.getChildren().addAll(headline, verifyBuyersAccountInfo, buyersAccountData, verifyReceiptInfo, confirmPaymentReceiptButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            boolean fiatAccount = model.isFiatAccount();
            verifyBuyersAccountInfo.setVisible(fiatAccount);
            verifyBuyersAccountInfo.setManaged(fiatAccount);
            buyersAccountData.setVisible(fiatAccount);
            buyersAccountData.setManaged(fiatAccount);

            headline.setText(Res.get("muSig.trade.state.phase3a.headline", model.getFormattedNonBtcAmount()));
            verifyReceiptInfo.setText(model.getVerifyReceiptInfo());
            buyersAccountData.setText(model.getBuyersAccountData());
            buyersAccountData.setDescription(model.getPeersAccountDataDescription());
            confirmPaymentReceiptButton.setText(Res.get("muSig.trade.state.phase3a.fiatReceivedButton"));
            confirmPaymentReceiptButton.setOnAction(e -> controller.onPaymentReceiptConfirmed());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmPaymentReceiptButton.setOnAction(null);
        }
    }
}
