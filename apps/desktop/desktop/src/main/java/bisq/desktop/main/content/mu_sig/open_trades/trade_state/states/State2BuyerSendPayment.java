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
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ClipboardUtil;
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
public class State2BuyerSendPayment extends BaseState {
    private final Controller controller;

    public State2BuyerSendPayment(ServiceProvider serviceProvider,
                                  MuSigTrade trade,
                                  MuSigOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, trade, channel);
    }

    public VBox getRoot() {
        return controller.getView().getRoot();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
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

                new Popup().warning(Res.get("bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning")).show();
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
                model.setHeadline(Res.get("muSig.tradeState.info.fiat.phase2a.headline",
                        formattedNonBtcAmount, paymentMethodName));
                model.setPeersAccountDataDescription(Res.get("muSig.tradeState.info.fiat.phase2a.sellersAccount"));
            } else {
                String nonBtcCurrencyCode = model.getNonBtcCurrencyCode();
                model.setHeadline(Res.get("muSig.tradeState.info.crypto.phase2a.headline",
                        formattedNonBtcAmount, nonBtcCurrencyCode));
                model.setPeersAccountDataDescription(Res.get("muSig.tradeState.info.crypto.phase2a.sellersAccount",
                        nonBtcCurrencyCode));
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onConfirmFiatSent() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.buyer.phase2a.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getQuoteCode()));
            muSigTradeService.paymentInitiated(model.getTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final BooleanProperty confirmFiatSentButtonDisabled = new SimpleBooleanProperty();
        private final SettableErrorValidator accountDataBannedValidator = new SettableErrorValidator(Res.get("bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError"));
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

    public static class View extends BaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton;
        private final MaterialTextArea sellersAccountData;
        private final MaterialTextField quoteAmount, myAccountName;
        private final WrappingText headline;
        private final HBox paymentReasonHbox;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = MuSigFormUtils.getHeadline();

            quoteAmount = MuSigFormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2a.quoteAmount"), "", false);
            myAccountName = MuSigFormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2a.myAccountName"), "", false);
            sellersAccountData = MuSigFormUtils.addTextArea("", "", false);
            sellersAccountData.setValidator(model.getAccountDataBannedValidator());

            Label paymentReason = new Label(Res.get("muSig.tradeState.info.phase2a.paymentReason"));
            paymentReason.setWrapText(true);
            paymentReason.getStyleClass().add("text-fill-grey-dimmed");

            Label warningIcon = new Label();
            warningIcon.getStyleClass().add("text-fill-grey-dimmed");
            Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "0.85em");

            HBox.setMargin(warningIcon, new Insets(2.5, 0, 0, 0));
            paymentReasonHbox = new HBox(7.5, warningIcon, paymentReason);

            confirmFiatSentButton = new Button();
            confirmFiatSentButton.setDefaultButton(true);

            VBox.setMargin(paymentReasonHbox, new Insets(10, 0, 10, 0));
            VBox.setMargin(confirmFiatSentButton, new Insets(0, 0, 5, 0));
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
            confirmFiatSentButton.setText(Res.get("muSig.tradeState.info.phase2a.confirmFiatSent"));
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
