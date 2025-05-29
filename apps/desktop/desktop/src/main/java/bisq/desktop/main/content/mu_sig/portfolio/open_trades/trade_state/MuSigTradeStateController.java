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

package bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state;

import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_details.MuSigTradeDetailsController;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State2BuyerSendPayment;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State3BuyerWaitForSellersPaymentReceiptConfirmation;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State2SellerWaitForPayment;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State3aSellerConfirmPaymentReceipt;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State3bSellerWaitForBuyerToCloseTrade;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State1aSetupDepositTx;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State1bWaitForDepositTxConfirmation;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.State4TradeClosed;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.settings.DontShowAgainService;
import bisq.support.mediation.MediationRequest;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class MuSigTradeStateController implements Controller {
    @Getter
    private final MuSigTradeStateView view;
    private final MuSigTradeStateModel model;
    private final ServiceProvider serviceProvider;
    private final MuSigTradePhaseBox muSigTradePhaseBox;
    private final MuSigTradeDataHeader muSigTradeDataHeader;
    private final NetworkService networkService;
    private final MuSigTradeService tradeService;
    private final MuSigService muSigService;
    private final MuSigOpenTradeChannelService openTradeChannelService;
    private final MediationRequestService mediationRequestService;
    private final DontShowAgainService dontShowAgainService;
    private final Optional<ResendMessageService> resendMessageService;
    private Pin tradeStatePin, errorMessagePin, peersErrorMessagePin, isInMediationPin,
            requestMediationDeliveryStatusPin, messageDeliveryStatusByMessageIdPin;
    private Subscription channelPin;

    public MuSigTradeStateController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        tradeService = serviceProvider.getTradeService().getMuSigTradeService();
        muSigService = serviceProvider.getMuSigService();
        ChatService chatService = serviceProvider.getChatService();
        openTradeChannelService = chatService.getMuSigOpenTradeChannelService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();
        resendMessageService = serviceProvider.getNetworkService().getResendMessageService();

        muSigTradePhaseBox = new MuSigTradePhaseBox(serviceProvider);
        muSigTradeDataHeader = new MuSigTradeDataHeader(serviceProvider, Res.get("bisqEasy.tradeState.header.peer").toUpperCase());
        model = new MuSigTradeStateModel();
        view = new MuSigTradeStateView(model, this,
                muSigTradePhaseBox.getRoot(),
                muSigTradeDataHeader.getRoot());
    }

    public void setSelectedChannel(MuSigOpenTradeChannel channel) {
        model.getChannel().set(channel);
    }

    @Override
    public void onActivate() {
        channelPin = EasyBind.subscribe(model.getChannel(), channel -> {
            muSigTradeDataHeader.setSelectedChannel(channel);
            muSigTradePhaseBox.setSelectedChannel(channel);

            removeChannelRelatedBindings();

            if (channel == null) {
                model.resetAll();
                return;
            }

            Optional<MuSigTrade> optionalMuSigTrade = tradeService.findTrade(channel.getTradeId());
            if (optionalMuSigTrade.isEmpty()) {
                model.resetAll();
                return;
            }

            model.reset();


            isInMediationPin = FxBindings.bind(model.getIsInMediation()).to(channel.isInMediationObservable());

            MuSigTrade trade = optionalMuSigTrade.get();
            model.getTrade().set(trade);

            muSigTradePhaseBox.setMuSigTrade(trade);

            tradeStatePin = trade.tradeStateObservable().addObserver(state ->
                    UIThread.run(() -> handleStateChange(state)));

            errorMessagePin = trade.errorMessageObservable().addObserver(errorMessage -> {
                        if (errorMessage != null) {
                            String key = "errorMessage_" + model.getTrade().get().getId();
                            if (dontShowAgainService.showAgain(key)) {
                                UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failed.popup",
                                                errorMessage,
                                                StringUtils.truncate(trade.getErrorStackTrace(), 2000)))
                                        .dontShowAgainId(key)
                                        .show());
                            }
                        }
                    }
            );
            peersErrorMessagePin = trade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            String key = "peersErrorMessage_" + model.getTrade().get().getId();
                            if (dontShowAgainService.showAgain(key)) {
                                UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.popup",
                                                peersErrorMessage,
                                                StringUtils.truncate(trade.getPeersErrorStackTrace(), 2000)))
                                        .dontShowAgainId(key)
                                        .show());
                            }
                        }
                    }
            );

            messageDeliveryStatusByMessageIdPin = networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
                @Override
                public void put(String messageId, Observable<MessageDeliveryStatus> value) {
                    handleNewMessageDeliveryStatus(messageId, value);
                }
            });
        });
    }

    @Override
    public void onDeactivate() {
        channelPin.unsubscribe();
        removeChannelRelatedBindings();
        if (requestMediationDeliveryStatusPin != null) {
            requestMediationDeliveryStatusPin.unbind();
            requestMediationDeliveryStatusPin = null;
        }
        model.resetAll();
    }

    void onShowTradeDetails() {
        MuSigOpenTradeChannel channel = model.getChannel().get();
        Optional<MuSigTrade> optionalMuSigTrade = tradeService.findTrade(channel.getTradeId());
        if (optionalMuSigTrade.isEmpty()) {
            model.resetAll();
            return;
        }

        MuSigTrade trade = optionalMuSigTrade.get();
        Navigation.navigateTo(NavigationTarget.MU_SIG_TRADE_DETAILS,
                new MuSigTradeDetailsController.InitData(trade, channel));
    }

    void onCloseTrade() {
        new Popup().warning(Res.get("bisqEasy.openTrades.closeTrade.warning.interrupted"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(this::doCloseTrade)
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    private void doCloseTrade() {
        muSigService.closeTrade(model.getTrade().get(), model.getChannel().get());
    }

    void onExportTrade() {
        MuSigOpenTradesUtils.exportTrade(model.getTrade().get(), getView().getRoot().getScene());
    }

    void onRequestMediation() {
        MuSigOpenTradesUtils.requestMediation(model.getChannel().get(),
                model.getTrade().get().getContract(),
                mediationRequestService, openTradeChannelService);
    }

    public void onResendMediationRequest() {
        MuSigTrade trade = model.getTrade().get();
        if (trade != null) {
            String mediationRequestId = MediationRequest.createMessageId(trade.getId());
            resendMessageService.ifPresent(service -> service.manuallyResendMessage(mediationRequestId));
        }
    }

    public boolean canManuallyResendMessage(String messageId) {
        return resendMessageService.map(service -> service.canManuallyResendMessage(messageId)).orElse(false);
    }

    private void handleNewMessageDeliveryStatus(String messageId, Observable<MessageDeliveryStatus> observableStatus) {
        MuSigTrade trade = model.getTrade().get();
        if (trade == null) {
            return;
        }
        String mediationRequestId = MediationRequest.createMessageId(trade.getId());
        if (!mediationRequestId.equals(messageId)) {
            return;
        }
        if (requestMediationDeliveryStatusPin != null) {
            requestMediationDeliveryStatusPin.unbind();
        }
        requestMediationDeliveryStatusPin = observableStatus.addObserver(status -> UIThread.run(() -> {
            model.getRequestMediationDeliveryStatus().set(status);

            if (status == MessageDeliveryStatus.FAILED) {
                model.getShouldShowTryRequestMediationAgain().set(resendMessageService
                        .map(service -> service.canManuallyResendMessage(messageId))
                        .orElse(false));
            } else {
                model.getShouldShowTryRequestMediationAgain().set(false);
            }
        }));
    }

    private void handleStateChange(@Nullable MuSigTradeState state) {
        if (state == null) {
            model.getStateInfoVBox().set(null);
            return;
        }

        MuSigTrade trade = checkNotNull(model.getTrade().get());
        MuSigOpenTradeChannel channel = checkNotNull(model.getChannel().get());
        boolean isSeller = trade.isSeller();

        model.getPhaseAndInfoVisible().set(true);
        model.getError().set(false);
        model.getIsTradeCompleted().set(state.isFinalState());
        switch (state) {
            case INIT -> {
            }

            // Deposit tx setup phase
            case TAKER_INITIALIZED_TRADE,
                 MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES,
                 TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES -> {
                model.getStateInfoVBox().set(new State1aSetupDepositTx(serviceProvider, trade, channel).getRoot());
            }

            // Deposit tx published
            case MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX,
                 TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX -> {
                model.getStateInfoVBox().set(new State1bWaitForDepositTxConfirmation(serviceProvider, trade, channel).getRoot());
            }
            // Deposit tx confirmed, settlement phase starts
            case DEPOSIT_TX_CONFIRMED -> {
                if (isSeller) {
                    model.getStateInfoVBox().set(new State2SellerWaitForPayment(serviceProvider, trade, channel).getRoot());
                } else {
                    model.getStateInfoVBox().set(new State2BuyerSendPayment(serviceProvider, trade, channel).getRoot());
                }
            }

            case BUYER_INITIATED_PAYMENT -> {
                model.getStateInfoVBox().set(new State3BuyerWaitForSellersPaymentReceiptConfirmation(serviceProvider, trade, channel).getRoot());
            }
            case SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE -> {
                model.getStateInfoVBox().set(new State3aSellerConfirmPaymentReceipt(serviceProvider, trade, channel).getRoot());
            }
            case SELLER_CONFIRMED_PAYMENT_RECEIPT -> {
                model.getStateInfoVBox().set(new State3bSellerWaitForBuyerToCloseTrade(serviceProvider, trade, channel).getRoot());
            }

            case BUYER_CLOSED_TRADE,
                 SELLER_CLOSED_TRADE,
                 BUYER_FORCE_CLOSED_TRADE,
                 SELLER_FORCE_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new State4TradeClosed(serviceProvider, trade, channel).getRoot());
            }
            case FAILED -> {
                model.getPhaseAndInfoVisible().set(false);
                model.getError().set(true);
                model.getShowReportToMediatorButton().set(false);
                model.getErrorMessage().set(Res.get("bisqEasy.openTrades.failed",
                        model.getTrade().get().getErrorMessage()));
            }
            case FAILED_AT_PEER -> {
                model.getPhaseAndInfoVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getError().set(true);
                model.getErrorMessage().set(Res.get("bisqEasy.openTrades.failedAtPeer",
                        model.getTrade().get().getPeersErrorMessage()));
            }

            default -> log.error("State {} not handled", state.name());
        }
    }

    private void removeChannelRelatedBindings() {
        if (tradeStatePin != null) {
            tradeStatePin.unbind();
            tradeStatePin = null;
        }
        if (errorMessagePin != null) {
            errorMessagePin.unbind();
            errorMessagePin = null;
        }
        if (peersErrorMessagePin != null) {
            peersErrorMessagePin.unbind();
            peersErrorMessagePin = null;
        }
        if (isInMediationPin != null) {
            isInMediationPin.unbind();
            isInMediationPin = null;
        }
        if (messageDeliveryStatusByMessageIdPin != null) {
            messageDeliveryStatusByMessageIdPin.unbind();
            messageDeliveryStatusByMessageIdPin = null;
        }
        if (requestMediationDeliveryStatusPin != null) {
            requestMediationDeliveryStatusPin.unbind();
            requestMediationDeliveryStatusPin = null;
        }
    }
}
