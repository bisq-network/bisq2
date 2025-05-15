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
import bisq.chat.priv.LeavePrivateChatManager;
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
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.BuyerState1AwaitDepositConfirmation;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.BuyerState2SendQuoteSideAsset;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.BuyerState2b;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.BuyerState4;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.SellerState1AwaitDepositConfirmation;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.SellerState2AwaitQuoteSideAsset;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.SellerState3a;
import bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states.SellerState4;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.offer.price.spec.PriceSpecFormatter;
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
    private final MuSigOpenTradeChannelService openTradeChannelService;
    private final MediationRequestService mediationRequestService;
    private final DontShowAgainService dontShowAgainService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private final Optional<ResendMessageService> resendMessageService;
    private Pin tradeStatePin, errorMessagePin, peersErrorMessagePin, isInMediationPin,
            requestMediationDeliveryStatusPin, messageDeliveryStatusByMessageIdPin;
    private Subscription channelPin;

    public MuSigTradeStateController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        tradeService = serviceProvider.getTradeService().getMuSigTradeService();
        ChatService chatService = serviceProvider.getChatService();
        openTradeChannelService = chatService.getMuSigOpenTradeChannelService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();
        resendMessageService = serviceProvider.getNetworkService().getResendMessageService();

        muSigTradePhaseBox = new MuSigTradePhaseBox(serviceProvider);
        muSigTradeDataHeader = new MuSigTradeDataHeader(serviceProvider, Res.get("bisqEasy.tradeState.header.peer").toUpperCase());
        model = new MuSigTradeStateModel();
        view = new MuSigTradeStateView(model, this,
                muSigTradePhaseBox.getView().getRoot(),
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

            model.getBuyerPriceDescriptionApprovalOverlay().set(
                    Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.buyersPrice",
                            PriceSpecFormatter.getFormattedPriceSpec(trade.getOffer().getPriceSpec())));
            model.getSellerPriceDescriptionApprovalOverlay().set(
                    Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.sellersPrice",
                            PriceSpecFormatter.getFormattedPriceSpec(trade.getContract().getPriceSpec())));

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
        // We need to pin the chatChannel to close as the one in the model would get updated after
        // muSigTradeService.removeTrade, and then we would close the wrong channel.
        MuSigOpenTradeChannel chatChannel = model.getChannel().get();
        tradeService.removeTrade(model.getTrade().get());
        leavePrivateChatManager.leaveChannel(chatChannel);
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
            case BUYER_AS_TAKER_INITIALIZED_TRADE,
                 BUYER_AS_TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES -> {
                // todo protocol started state
            }
            case BUYER_AS_TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX -> {
                model.getStateInfoVBox().set(new BuyerState1AwaitDepositConfirmation(serviceProvider, trade, channel).getView().getRoot());
            }
            case SELLER_AS_MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES,
                 SELLER_AS_MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX -> {
                // todo protocol started state

                //todo add state when deposit is seen in network
                model.getStateInfoVBox().set(new SellerState1AwaitDepositConfirmation(serviceProvider, trade, channel).getView().getRoot());
            }

            case DEPOSIT_TX_CONFIRMED -> {
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState2AwaitQuoteSideAsset(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState2SendQuoteSideAsset(serviceProvider, trade, channel).getView().getRoot());
                }
            }

            // Settlement phase
            case BUYER_AS_TAKER_INITIATED_PAYMENT -> {
                model.getStateInfoVBox().set(new BuyerState2b(serviceProvider, trade, channel).getView().getRoot());
            }
            case SELLER_AS_MAKER_RECEIVED_INITIATED_PAYMENT_MESSAGE -> {
                model.getStateInfoVBox().set(new SellerState2AwaitQuoteSideAsset(serviceProvider, trade, channel).getView().getRoot());
            }
            case SELLER_AS_MAKER_RECEIVED_PAYMENT -> {
                model.getStateInfoVBox().set(new SellerState3a(serviceProvider, trade, channel).getView().getRoot());
            }

            // Cooperative path
            case BUYER_AS_TAKER_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new BuyerState4(serviceProvider, trade, channel).getView().getRoot());
            }
            case SELLER_AS_MAKER_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new SellerState4(serviceProvider, trade, channel).getView().getRoot());
            }

            // Uncooperative path
            case BUYER_AS_TAKER_FORCE_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new BuyerState4(serviceProvider, trade, channel).getView().getRoot());
            }
            case SELLER_AS_MAKER_FORCE_CLOSED_TRADE -> {
                model.getStateInfoVBox().set(new SellerState4(serviceProvider, trade, channel).getView().getRoot());
            }
        }
       /* switch (state) {
            case INIT:
                break;
            case TAKER_SENT_TAKE_OFFER_REQUEST:

                // Seller
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_:
            case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                // Buyer
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState1(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState1a(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            // Seller
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_:
            case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                model.getStateInfoVBox().set(new SellerState2a(serviceProvider, trade, channel).getView().getRoot());
                break;
            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new SellerState2b(serviceProvider, trade, channel).getView().getRoot());
                break;
            case SELLER_CONFIRMED_FIAT_RECEIPT:
                model.getStateInfoVBox().set(new SellerState3a(serviceProvider, trade, channel).getView().getRoot());
                break;
            case SELLER_SENT_BTC_SENT_CONFIRMATION:
                if (isMainChain) {
                    model.getStateInfoVBox().set(new SellerStateMainChain3b(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new SellerStateLightning3b(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            // Buyer
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_:
            case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                model.getStateInfoVBox().set(new BuyerState1b(serviceProvider, trade, channel).getView().getRoot());
                break;
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                model.getStateInfoVBox().set(new BuyerState2a(serviceProvider, trade, channel).getView().getRoot());
                break;
            case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState2b(serviceProvider, trade, channel).getView().getRoot());
                break;
            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState3a(serviceProvider, trade, channel).getView().getRoot());
                break;
            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                if (isMainChain) {
                    model.getStateInfoVBox().set(new BuyerStateMainChain3b(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerStateLightning3b(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            case BTC_CONFIRMED:
                model.getInterruptTradeButtonVisible().set(false);
                model.getIsTradeCompleted().set(true);
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState4(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState4(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            case REJECTED:
            case PEER_REJECTED:
                model.getPhaseAndInfoVisible().set(false);
                model.getInterruptedTradeInfo().set(true);
                model.getInterruptTradeButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, false);
                break;

            case CANCELLED:
            case PEER_CANCELLED:
                model.getPhaseAndInfoVisible().set(false);
                model.getInterruptedTradeInfo().set(true);
                model.getInterruptTradeButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, true);
                break;

            case FAILED:
                model.getPhaseAndInfoVisible().set(false);
                model.getError().set(true);
                model.getInterruptTradeButtonVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getErrorMessage().set(Res.get("muSig.openTrades.failed",
                        model.getTrade().get().getErrorMessage()));
                break;
            case FAILED_AT_PEER:
                model.getPhaseAndInfoVisible().set(false);
                model.getInterruptTradeButtonVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getError().set(true);
                model.getErrorMessage().set(Res.get("muSig.openTrades.failedAtPeer",
                        model.getTrade().get().getPeersErrorMessage()));
                break;

            default:
                log.error("State {} not handled", state.name());
        }*/
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
    }
}
