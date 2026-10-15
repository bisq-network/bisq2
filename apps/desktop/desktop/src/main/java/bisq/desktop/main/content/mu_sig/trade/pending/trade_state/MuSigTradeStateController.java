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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_state;

import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.TradeExceptionHandler;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsController;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState1aSetupDepositTx;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState1bWaitForDepositTxConfirmation;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState2BuyerSendPayment;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState2SellerWaitForPayment;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState3BuyerWaitForSellersPaymentReceiptConfirmation;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState3aSellerConfirmPaymentReceipt;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState3bSellerWaitForBuyerToCloseTrade;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_state.states.MuSigState4TradeClosed;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.settings.DontShowAgainService;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
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
    private final DontShowAgainService dontShowAgainService;
    private final Optional<ResendMessageService> resendMessageService;
    private Pin tradeStatePin, errorMessagePin, peersErrorMessagePin, disputeStatePin,
            requestMediationDeliveryStatusPin, requestArbitrationDeliveryStatusPin,
            messageDeliveryStatusByMessageIdPin, myMediationResultRejectedPin, peerMediationResultRejectedPin;
    private Subscription channelPin;
    private long tradeSelectionVersion;

    public MuSigTradeStateController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        tradeService = serviceProvider.getTradeService().getMuSigTradeService();
        muSigService = serviceProvider.getMuSigService();
        ChatService chatService = serviceProvider.getChatService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();
        resendMessageService = serviceProvider.getNetworkService().getResendMessageService();

        muSigTradePhaseBox = new MuSigTradePhaseBox(serviceProvider);
        muSigTradeDataHeader = new MuSigTradeDataHeader(serviceProvider, Res.get("muSig.trade.pending.header.peer").toUpperCase());
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

            MuSigTrade trade = optionalMuSigTrade.get();
            model.getTrade().set(trade);
            updateDisputePresentation();
            long selectionVersion = tradeSelectionVersion;

            disputeStatePin = trade.getTradeDispute().disputeStateObservable().addObserver(disputeState -> {
                MuSigMediationResult result = trade.getTradeDispute().getMuSigMediationResult().orElse(null);
                UIThread.run(() -> {
                    if (!isSelectedTrade(trade, selectionVersion)) {
                        return;
                    }
                    model.getMediationResult().set(result);
                    model.getDisputeState().set(disputeState);
                    model.getIsInMediation().set(shouldShowMediationBanner(disputeState));
                    model.getIsInArbitration().set(shouldShowArbitrationBanner(disputeState));
                    updateDisputePresentation();
                });
            });

            muSigTradePhaseBox.setMuSigTrade(trade);

            tradeStatePin = trade.tradeStateObservable().addObserver(state -> {
                boolean myCustomPayoutSigned = trade.getMyself().getCustomPayoutData().isPresent();
                UIThread.run(() -> {
                    if (!isSelectedTrade(trade, selectionVersion)) {
                        return;
                    }
                    model.getTradeState().set(state);
                    if (myCustomPayoutSigned) {
                        model.getMyMediationResultDecisionMade().set(true);
                    }
                    handleStateChange(state);
                    updateDisputePresentation();
                });
            });
            myMediationResultRejectedPin = trade.getMyself().mediationResultRejectedObservable().addObserver(rejected ->
                    UIThread.run(() -> {
                        if (!isSelectedTrade(trade, selectionVersion)) {
                            return;
                        }
                        // The local decision also covers custom payout signing, so false must not clear it.
                        // The peer flag below only tracks rejection.
                        if (rejected) {
                            model.getMyMediationResultDecisionMade().set(true);
                        }
                        updateDisputePresentation();
                    }));
            peerMediationResultRejectedPin = trade.getPeer().mediationResultRejectedObservable().addObserver(rejected ->
                    UIThread.run(() -> {
                        if (!isSelectedTrade(trade, selectionVersion)) {
                            return;
                        }
                        model.getPeerMediationResultRejected().set(rejected);
                        updateDisputePresentation();
                    }));

            errorMessagePin = trade.errorMessageObservable().addObserver(errorMessage -> {
                        if (errorMessage != null) {
                            String key = "errorMessage_" + trade.getId();
                            if (dontShowAgainService.showAgain(key)) {
                                UIThread.run(() -> {
                                    if (trade.getTradeProtocolFailure() == null || trade.getTradeProtocolFailure().isUnexpected()) {
                                        String errorStackTrace = trade.getErrorStackTrace() != null ? StringUtils.truncate(trade.getErrorStackTrace(), 2000) : "";
                                        new Popup().error(Res.get("muSig.trade.pending.failed.errorPopup.message",
                                                        errorMessage,
                                                        errorStackTrace))
                                                .dontShowAgainId(key)
                                                .show();
                                    } else {
                                        new Popup().headline(Res.get("muSig.trade.pending.failure.popup.headline"))
                                                .failure(Res.get("muSig.trade.pending.failure.popup.message.header"),
                                                        errorMessage,
                                                        Res.get("muSig.trade.pending.failure.popup.message.footer"))
                                                .dontShowAgainId(key)
                                                .show();
                                    }
                                });
                            }
                        }
                    }
            );
            peersErrorMessagePin = trade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            String key = "peersErrorMessage_" + trade.getId();
                            if (dontShowAgainService.showAgain(key)) {
                                UIThread.run(() -> {
                                    if (trade.getPeersTradeProtocolFailure() == null || trade.getPeersTradeProtocolFailure().isUnexpected()) {
                                        String errorStackTrace = trade.getPeersErrorStackTrace() != null ? StringUtils.truncate(trade.getPeersErrorStackTrace(), 2000) : "";
                                        new Popup().error(Res.get("muSig.trade.pending.failedAtPeer.errorPopup.message",
                                                        peersErrorMessage,
                                                        errorStackTrace))
                                                .dontShowAgainId(key)
                                                .show();
                                    } else {
                                        new Popup().headline(Res.get("muSig.trade.pending.failure.popup.headline.atPeer"))
                                                .failure(Res.get("muSig.trade.pending.failure.popup.message.header"),
                                                        peersErrorMessage,
                                                        Res.get("muSig.trade.pending.failure.popup.message.footer"))
                                                .dontShowAgainId(key)
                                                .show();
                                    }
                                });
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
        new Popup().information(Res.get("muSig.trade.closeTrade.info"))
                .actionButtonText(Res.get("muSig.trade.closeTrade.info.actionButton"))
                .onAction(this::doCloseTrade)
                .closeButtonText(Res.get("action.cancel"))
                .show();
    }

    private void doCloseTrade() {
        muSigService.closeTrade(model.getTrade().get(), model.getChannel().get());
        goToTradeHistory();
    }

    private void goToTradeHistory() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_HISTORY);
    }

    void onExportTrade() {
        MuSigPendingTTradesUtils.exportTrade(model.getTrade().get(), getView().getRoot().getScene());
    }

    void onRequestMediation() {
        MuSigPendingTTradesUtils.requestMediation(model.getTrade().get(), tradeService);
    }

    public void onResendMediationRequest() {
        MuSigTrade trade = model.getTrade().get();
        if (trade != null) {
            String mediationRequestId = MuSigMediationRequest.createMessageId(trade.getId());
            resendMessageService.ifPresent(service -> service.manuallyResendMessage(mediationRequestId));
        }
    }

    public void onAcceptMediationResult() {
        MuSigTrade trade = model.getTrade().get();
        if (trade != null && !model.getIsTradeCompleted().get() &&
                model.getMediationResultAcceptanceAvailable().get()) {
            TradeExceptionHandler.run(() -> tradeService.acceptMediationResult(trade));
        }
    }

    public void onRejectMediationResult() {
        MuSigTrade trade = model.getTrade().get();
        if (trade != null && !model.getIsTradeCompleted().get() &&
                model.getDisputeState().get() == MuSigDisputeState.MEDIATION_CLOSED) {
            MuSigPendingTTradesUtils.rejectMediationResultAndRequestArbitration(trade, tradeService);
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
        String mediationRequestId = MuSigMediationRequest.createMessageId(trade.getId());
        String arbitrationRequestId = MuSigArbitrationRequest.createMessageId(trade.getId());
        if (!mediationRequestId.equals(messageId) && !arbitrationRequestId.equals(messageId)) {
            return;
        }
        if (mediationRequestId.equals(messageId)) {
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
                updateMediationBannerText();
            }));
        } else {
            if (requestArbitrationDeliveryStatusPin != null) {
                requestArbitrationDeliveryStatusPin.unbind();
            }
            requestArbitrationDeliveryStatusPin = observableStatus.addObserver(status -> UIThread.run(() -> {
                model.getRequestArbitrationDeliveryStatus().set(status);
                updateArbitrationBannerText();
            }));
        }
    }

    private void updateDisputePresentation() {
        updateMediationResultControls(model);
        updateMediationBannerText();
        updateArbitrationBannerText();
    }

    static void updateMediationResultControls(MuSigTradeStateModel model) {
        boolean canDecide = model.getDisputeState().get() == MuSigDisputeState.MEDIATION_CLOSED &&
                hasPayoutResult(model.getMediationResult().get()) && !model.getMyMediationResultDecisionMade().get();
        model.getShowMediationResultDecisionButtons().set(model.getTrade().get() != null && canDecide);

        // Presentation only: accepting the result still requires the service's full signing validation.
        MuSigTradeState state = model.getTradeState().get();
        boolean signingState = state == MuSigTradeState.DEPOSIT_TX_CONFIRMED ||
                state == MuSigTradeState.BUYER_INITIATED_PAYMENT ||
                state == MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE;
        model.getMediationResultAcceptanceAvailable().set(signingState && canDecide &&
                !model.getPeerMediationResultRejected().get());
    }

    private static boolean hasPayoutResult(@Nullable MuSigMediationResult result) {
        return result != null && result.getMediationPayoutDistributionType() != MediationPayoutDistributionType.NO_PAYOUT;
    }

    private void updateMediationBannerText() {
        MuSigTrade trade = model.getTrade().get();
        if (trade != null) {
            MuSigDisputeState disputeState = model.getDisputeState().get();
            MuSigMediationResult result = model.getMediationResult().get();
            if (disputeState == MuSigDisputeState.MEDIATION_CLOSED) {
                String details = result != null ? getMediationResultDetailsText(trade, result) : Res.get("data.na");
                model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.closed", details));
                return;
            } else if (disputeState == MuSigDisputeState.MEDIATION_RE_OPENED) {
                String details = result != null ? getMediationResultDetailsText(trade, result) : Res.get("data.na");
                model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.reOpened", details));
                return;
            } else if (disputeState == MuSigDisputeState.MEDIATION_OPEN) {
                model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.info"));
                return;
            } else if (disputeState != MuSigDisputeState.MEDIATION_REQUESTED) {
                model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.info"));
                return;
            }
        }

        // In MEDIATION_REQUESTED we reflect transport status of the request message.
        // If the peer had sent the request we do not get any requestMediationDeliveryStatus; status is null.
        MessageDeliveryStatus status = model.getRequestMediationDeliveryStatus().get();
        if (status == null || status == MessageDeliveryStatus.ACK_RECEIVED || status == MessageDeliveryStatus.MAILBOX_MSG_RECEIVED) {
            model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.requested"));
        } else {
            String deliveryStatus = getMessageDeliveryStatusDisplayString(status);
            if (status == MessageDeliveryStatus.FAILED) {
                String resendRequest = model.getShouldShowTryRequestMediationAgain().get()
                        ? " " + Res.get("muSig.trade.requestMediation.resendRequest")
                        : "";
                model.getMediationBannerText().set(deliveryStatus + resendRequest);
            } else {
                model.getMediationBannerText().set(Res.get("muSig.trade.pending.inMediation.requestSent", deliveryStatus));
            }
        }
    }

    private void updateArbitrationBannerText() {
        MuSigTrade trade = model.getTrade().get();
        if (trade == null) {
            model.getArbitrationBannerText().set("");
            return;
        }

        MuSigDisputeState disputeState = model.getDisputeState().get();
        if (disputeState == MuSigDisputeState.ARBITRATION_OPEN) {
            model.getArbitrationBannerText().set(Res.get("muSig.trade.pending.inArbitration.open"));
            return;
        } else if (disputeState == MuSigDisputeState.ARBITRATION_CLOSED) {
            model.getArbitrationBannerText().set(Res.get("muSig.trade.pending.inArbitration.closed"));
            return;
        } else if (disputeState != MuSigDisputeState.ARBITRATION_REQUESTED) {
            model.getArbitrationBannerText().set("");
            return;
        }

        MessageDeliveryStatus status = model.getRequestArbitrationDeliveryStatus().get();
        if (status == null || status == MessageDeliveryStatus.ACK_RECEIVED || status == MessageDeliveryStatus.MAILBOX_MSG_RECEIVED) {
            model.getArbitrationBannerText().set(Res.get("muSig.trade.pending.inArbitration.requested"));
        } else {
            String deliveryStatus = getMessageDeliveryStatusDisplayString(status);
            model.getArbitrationBannerText().set(Res.get("muSig.trade.pending.inArbitration.requestSent", deliveryStatus));
        }
    }

    private static String getMessageDeliveryStatusDisplayString(MessageDeliveryStatus status) {
        return switch (status) {
            case CONNECTING -> Res.get("muSig.trade.requestMediation.deliveryState.CONNECTING");
            case SENT -> Res.get("muSig.trade.requestMediation.deliveryState.SENT");
            case ACK_RECEIVED -> Res.get("muSig.trade.requestMediation.deliveryState.ACK_RECEIVED");
            case TRY_ADD_TO_MAILBOX -> Res.get("muSig.trade.requestMediation.deliveryState.TRY_ADD_TO_MAILBOX");
            case ADDED_TO_MAILBOX -> Res.get("muSig.trade.requestMediation.deliveryState.ADDED_TO_MAILBOX");
            case MAILBOX_MSG_RECEIVED -> Res.get("muSig.trade.requestMediation.deliveryState.MAILBOX_MSG_RECEIVED");
            case FAILED -> Res.get("muSig.trade.requestMediation.deliveryState.FAILED");
        };
    }

    private static String getMediationResultDetailsText(MuSigTrade trade, MuSigMediationResult result) {
        if (result.getProposedBuyerPayoutAmount().isEmpty() || result.getProposedSellerPayoutAmount().isEmpty()) {
            if (result.getMediationPayoutDistributionType() == MediationPayoutDistributionType.NO_PAYOUT) {
                return Res.get("muSig.trade.pending.inMediation.resultDetails.noPayout");
            }
            return Res.get("muSig.trade.pending.inMediation.resultDetails", Res.get("data.na"), Res.get("data.na"));
        }

        String myPayoutAmount = trade.isBuyer()
                ? AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedBuyerPayoutAmount().orElseThrow()))
                : AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedSellerPayoutAmount().orElseThrow()));
        String peerPayoutAmount = trade.isBuyer()
                ? AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedSellerPayoutAmount().orElseThrow()))
                : AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedBuyerPayoutAmount().orElseThrow()));
        return Res.get("muSig.trade.pending.inMediation.resultDetails", myPayoutAmount, peerPayoutAmount);
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
                 TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES,
                 MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX,
                 TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX -> {
                model.getStateInfoVBox().set(new MuSigState1aSetupDepositTx(serviceProvider, trade, channel).getRoot());
            }

            // Deposit tx published and account payload exchanged
            case MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                 TAKER_RECEIVED_ACCOUNT_PAYLOAD -> {
                model.getStateInfoVBox().set(new MuSigState1bWaitForDepositTxConfirmation(serviceProvider, trade, channel).getRoot());
            }

            // Deposit tx confirmed, settlement phase starts
            case DEPOSIT_TX_CONFIRMED -> {
                if (isSeller) {
                    model.getStateInfoVBox().set(new MuSigState2SellerWaitForPayment(serviceProvider, trade, channel).getRoot());
                } else {
                    model.getStateInfoVBox().set(new MuSigState2BuyerSendPayment(serviceProvider, trade, channel).getRoot());
                }
            }

            case BUYER_INITIATED_PAYMENT -> {
                model.getStateInfoVBox().set(new MuSigState3BuyerWaitForSellersPaymentReceiptConfirmation(serviceProvider, trade, channel).getRoot());
            }
            case SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE -> {
                model.getStateInfoVBox().set(new MuSigState3aSellerConfirmPaymentReceipt(serviceProvider, trade, channel).getRoot());
            }
            case SELLER_CONFIRMED_PAYMENT_RECEIPT -> {
                model.getStateInfoVBox().set(new MuSigState3bSellerWaitForBuyerToCloseTrade(serviceProvider, trade, channel).getRoot());
            }
            case CUSTOM_PAYOUT_SIGNED -> model.getPhaseAndInfoVisible().set(false);

            case BUYER_CLOSED_TRADE,
                 SELLER_CLOSED_TRADE,
                 BUYER_FORCE_CLOSED_TRADE,
                 SELLER_FORCE_CLOSED_TRADE,
                 CUSTOM_PAYOUT_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new MuSigState4TradeClosed(serviceProvider, trade, channel).getRoot());
            }
            case FAILED -> {
                model.getPhaseAndInfoVisible().set(false);
                model.getError().set(true);
                model.getShowReportToMediatorButton().set(false);
                model.getErrorMessage().set(Res.get("muSig.trade.pending.failed.errorMessage",
                        model.getTrade().get().getErrorMessage()));
            }
            case FAILED_AT_PEER -> {
                model.getPhaseAndInfoVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getError().set(true);
                model.getErrorMessage().set(Res.get("muSig.trade.pending.failedAtPeer.errorMessage",
                        model.getTrade().get().getPeersErrorMessage()));
            }

            default -> log.error("State {} not handled", state.name());
        }
    }

    private void removeChannelRelatedBindings() {
        // Ignore queued observer callbacks from a previous selection.
        tradeSelectionVersion++;
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
        if (disputeStatePin != null) {
            disputeStatePin.unbind();
            disputeStatePin = null;
        }
        if (myMediationResultRejectedPin != null) {
            myMediationResultRejectedPin.unbind();
            myMediationResultRejectedPin = null;
        }
        if (peerMediationResultRejectedPin != null) {
            peerMediationResultRejectedPin.unbind();
            peerMediationResultRejectedPin = null;
        }
        if (messageDeliveryStatusByMessageIdPin != null) {
            messageDeliveryStatusByMessageIdPin.unbind();
            messageDeliveryStatusByMessageIdPin = null;
        }
        if (requestMediationDeliveryStatusPin != null) {
            requestMediationDeliveryStatusPin.unbind();
            requestMediationDeliveryStatusPin = null;
        }
        if (requestArbitrationDeliveryStatusPin != null) {
            requestArbitrationDeliveryStatusPin.unbind();
            requestArbitrationDeliveryStatusPin = null;
        }
    }

    private static boolean shouldShowMediationBanner(MuSigDisputeState disputeState) {
        return MuSigDisputeState.isMediationState(disputeState);
    }

    private boolean isSelectedTrade(MuSigTrade trade, long selectionVersion) {
        return tradeSelectionVersion == selectionVersion && model.getTrade().get() == trade;
    }

    private static boolean shouldShowArbitrationBanner(MuSigDisputeState disputeState) {
        return MuSigDisputeState.isArbitrationState(disputeState);
    }
}
