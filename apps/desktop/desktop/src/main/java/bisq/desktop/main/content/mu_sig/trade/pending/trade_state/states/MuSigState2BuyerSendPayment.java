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
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.TradeExceptionHandler;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.SettableErrorValidator;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.support.moderator.ModerationRequestService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.user.profile.UserProfile;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class MuSigState2BuyerSendPayment extends MuSigBaseState {
    private final Controller controller;

    public MuSigState2BuyerSendPayment(ServiceProvider serviceProvider,
                                       MuSigTrade trade,
                                       MuSigOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, trade, channel);
    }

    public VBox getRoot() {
        return controller.getView().getRoot();
    }

    private static class Controller extends MuSigBaseState.Controller<Model, View> {
        private final ModerationRequestService moderationRequestService;

        private Controller(ServiceProvider serviceProvider,
                           MuSigTrade trade,
                           MuSigOpenTradeChannel channel) {
            super(serviceProvider, trade, channel);

            moderationRequestService = serviceProvider.getSupportService().getModerationRequestService();
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

            AccountPayload<?> peersAccountPayload = trade.getPeer().getAccountPayload().orElseThrow();
            if (muSigService.isAccountDataBanned(peersAccountPayload)) {
                model.getConfirmFiatSentButtonDisabled().set(true);
                model.getAccountDataBannedValidator().setIsInvalid(true);

                UserProfile peer = model.getChannel().getPeer();
                String peerUserName = peer.getUserName();

                // Report to moderator
                String message = "Account data of " + peerUserName + " is banned: " + peersAccountPayload;
                moderationRequestService.reportUserProfile(peer, message);

                new Popup().warning(Res.get("muSig.trade.state.phase2a.accountDataBanned.popup.warning.buyer")).show();
            } else {
                model.getConfirmFiatSentButtonDisabled().set(false);
                model.getAccountDataBannedValidator().setIsInvalid(false);
            }

            model.setSellersAccountData(peersAccountPayload.getAccountDataDisplayString());

            AccountPayload<?> myAccountPayload = trade.getMyself().getAccountPayload().orElseThrow();
            PaymentRail paymentRail = myAccountPayload.getPaymentMethod().getPaymentRail();
            Set<Account<? extends PaymentMethod<?>, ?>> accountsWithSamePaymentRail = accountService.findAccountsForPaymentRail(paymentRail);
            Optional<Account<? extends PaymentMethod<?>, ?>> myAccount = accountService.findAccount(myAccountPayload);
            if (accountsWithSamePaymentRail.size() > 1) {
                String myAccountName = myAccount
                        .map(Account::getAccountName)
                        .orElse(Res.get("data.na"));
                model.setMyAccountName(Optional.of(myAccountName));
            } else {
                model.setMyAccountName(Optional.empty());
            }

            model.setPaymentReasonVisible(AccountUtils.showReasonForPayment(myAccountPayload));

            String formattedNonBtcAmount = model.getFormattedNonBtcAmount();
            if (model.getMarket().isBaseCurrencyBitcoin()) {
                String paymentMethodName = trade.getContract().getNonBtcSidePaymentMethodSpec().getShortDisplayString();
                model.setHeadline(Res.get("muSig.trade.state.phase2a.headline.fiat",
                        formattedNonBtcAmount, paymentMethodName));
                model.setPeersAccountDataDescription(Res.get("muSig.trade.state.phase2a.sellersAccount.fiat"));
            } else {
                String nonBtcCurrencyCode = model.getNonBtcCurrencyCode();
                model.setHeadline(Res.get("muSig.trade.state.phase2a.headline.crypto",
                        formattedNonBtcAmount, nonBtcCurrencyCode));
                model.setPeersAccountDataDescription(Res.get("muSig.trade.state.phase2a.sellersAccount.crypto",
                        nonBtcCurrencyCode));
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onConfirmFiatSent() {
            if (TradeExceptionHandler.run(() -> muSigTradeService.paymentInitiated(model.getTrade()))) {
                sendTradeLogMessage(Res.encode("muSig.trade.state.phase2a.tradeLogMessage.buyer",
                        model.getChannel().getMyUserIdentity().getUserName(), model.getQuoteCode()));
            }
        }
    }

    @Getter
    private static class Model extends MuSigBaseState.Model {
        private final BooleanProperty confirmFiatSentButtonDisabled = new SimpleBooleanProperty();
        private final SettableErrorValidator accountDataBannedValidator = new SettableErrorValidator(Res.get("muSig.trade.state.phase2a.accountDataBannedError.buyer"));
        @Setter
        private String sellersAccountData;
        @Setter
        private Optional<String> myAccountName = Optional.empty();
        @Setter
        private boolean paymentReasonVisible;
        @Setter
        private String peersAccountDataDescription;
        @Setter
        private String headline;

        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
        }
    }

    public static class View extends MuSigBaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton;
        private final MaterialTextArea sellersAccountData;
        private final MaterialTextField quoteAmount, myAccountName;
        private final WrappingText headline;
        private final HBox paymentReasonHbox;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = MuSigFormUtils.getHeadline();

            quoteAmount = MuSigFormUtils.getTextField(Res.get("muSig.trade.state.phase2a.quoteAmount.buyer"), "", false);
            myAccountName = MuSigFormUtils.getTextField(Res.get("muSig.trade.state.phase2a.myAccountName.buyer"), "", false);
            sellersAccountData = MuSigFormUtils.addTextArea("", "", false);
            sellersAccountData.setValidator(model.getAccountDataBannedValidator());

            Label paymentReason = new Label(Res.get("muSig.trade.state.phase2a.paymentReason"));
            paymentReason.setWrapText(true);
            paymentReason.getStyleClass().add("text-fill-grey-dimmed");

            Label warningIcon = new Label();
            warningIcon.getStyleClass().add("text-fill-grey-dimmed");
            Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "0.85em");

            HBox.setMargin(warningIcon, new Insets(2.5, 0, 0, 0));
            paymentReasonHbox = new HBox(7.5, warningIcon, paymentReason);

            confirmFiatSentButton = new Button();
            confirmFiatSentButton.setDefaultButton(true);

            VBox.setMargin(confirmFiatSentButton, new Insets(10, 0, 5, 0));
            root.getChildren().addAll(
                    headline,
                    quoteAmount,
                    myAccountName,
                    sellersAccountData,
                    paymentReasonHbox,
                    confirmFiatSentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(model.getHeadline());
            quoteAmount.setText(model.getFormattedNonBtcAmount());
            quoteAmount.getIconButton().setOnAction(e -> ClipboardUtil.copyToClipboard(model.getNonBtcAmount()));
            model.getMyAccountName().ifPresent(myAccountName::setText);
            myAccountName.setVisible(model.getMyAccountName().isPresent());
            myAccountName.setManaged(myAccountName.isVisible());
            sellersAccountData.setDescription(model.getPeersAccountDataDescription());
            sellersAccountData.setText(model.getSellersAccountData());
            sellersAccountData.validate();
            paymentReasonHbox.setVisible(model.isPaymentReasonVisible());
            paymentReasonHbox.setManaged(model.isPaymentReasonVisible());
            confirmFiatSentButton.setText(Res.get("muSig.trade.state.phase2a.confirmFiatSent"));
            confirmFiatSentButton.setOnAction(e -> controller.onConfirmFiatSent());
            confirmFiatSentButton.disableProperty().bind(model.getConfirmFiatSentButtonDisabled());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmFiatSentButton.disableProperty().unbind();
            confirmFiatSentButton.setOnAction(null);
            quoteAmount.getIconButton().setOnAction(null);
        }
    }
}
