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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.contract.Role;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.components.TradeDataHeader;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_details.TradeDetailsController;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states.*;
import bisq.desktop.common.view.Navigation;
import bisq.bisq_easy.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.DontShowAgainService;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final ServiceProvider serviceProvider;
    private final TradePhaseBox tradePhaseBox;
    private final TradeDataHeader tradeDataHeader;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BisqEasyOpenTradeChannelService channelService;
    private final MediationRequestService mediationRequestService;
    private final DontShowAgainService dontShowAgainService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private Pin bisqEasyTradeStatePin, errorMessagePin, peersErrorMessagePin, isInMediationPin;
    private Subscription channelPin, hasBuyerAcceptedPriceSpecPin;

    public TradeStateController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        ChatService chatService = serviceProvider.getChatService();
        channelService = chatService.getBisqEasyOpenTradeChannelService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();

        tradePhaseBox = new TradePhaseBox(serviceProvider);
        tradeDataHeader = new TradeDataHeader(serviceProvider, Res.get("bisqEasy.tradeState.header.peer").toUpperCase());
        model = new TradeStateModel();
        view = new TradeStateView(model, this,
                tradePhaseBox.getView().getRoot(),
                tradeDataHeader.getRoot());
    }

    public void setSelectedChannel(BisqEasyOpenTradeChannel channel) {
        model.getChannel().set(channel);
    }

    @Override
    public void onActivate() {
        channelPin = EasyBind.subscribe(model.getChannel(), channel -> {
            tradeDataHeader.setSelectedChannel(channel);
            tradePhaseBox.setSelectedChannel(channel);

            if (bisqEasyTradeStatePin != null) {
                bisqEasyTradeStatePin.unbind();
            }

            if (channel == null) {
                model.resetAll();
                return;
            }

            Optional<BisqEasyTrade> optionalBisqEasyTrade = BisqEasyServiceUtil.findTradeFromChannel(serviceProvider, channel);
            if (optionalBisqEasyTrade.isEmpty()) {
                model.resetAll();
                return;
            }

            model.reset();

            if (isInMediationPin != null) {
                isInMediationPin.unbind();
            }
            isInMediationPin = FxBindings.bind(model.getIsInMediation()).to(channel.isInMediationObservable());

            BisqEasyTrade bisqEasyTrade = optionalBisqEasyTrade.get();
            model.getBisqEasyTrade().set(bisqEasyTrade);

            tradePhaseBox.setBisqEasyTrade(bisqEasyTrade);

            bisqEasyTradeStatePin = bisqEasyTrade.tradeStateObservable().addObserver(state ->
                    UIThread.run(() -> {
                        applyStateInfoVBox(state);
                        updateShouldShowSellerPriceApprovalOverlay();
                    }));

            errorMessagePin = bisqEasyTrade.errorMessageObservable().addObserver(errorMessage -> {
                if (errorMessage != null) {
                    String key = "errorMessage_" + model.getBisqEasyTrade().get().getId();
                    if (dontShowAgainService.showAgain(key)) {
                        UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failed.popup",
                                        errorMessage,
                                        StringUtils.truncate(bisqEasyTrade.getErrorStackTrace(), 500)))
                                .dontShowAgainId(key)
                                .show());
                    }
                        }
                    }
            );
            peersErrorMessagePin = bisqEasyTrade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            String key = "peersErrorMessage_" + model.getBisqEasyTrade().get().getId();
                            if (dontShowAgainService.showAgain(key)) {
                                UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.popup",
                                                peersErrorMessage,
                                                StringUtils.truncate(bisqEasyTrade.getPeersErrorStackTrace(), 500)))
                                        .dontShowAgainId(key)
                                        .show());
                            }
                        }
                    }
            );

            hasBuyerAcceptedPriceSpecPin = EasyBind.subscribe(model.getHasBuyerAcceptedSellersPriceSpec(),
                    hasAccepted -> updateShouldShowSellerPriceApprovalOverlay());

            model.getBuyerPriceDescriptionApprovalOverlay().set(
                    Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.buyersPrice",
                            BisqEasyServiceUtil.getFormattedPriceSpec(bisqEasyTrade.getOffer().getPriceSpec())));
            model.getSellerPriceDescriptionApprovalOverlay().set(
                    Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.sellersPrice",
                            BisqEasyServiceUtil.getFormattedPriceSpec(bisqEasyTrade.getContract().getAgreedPriceSpec())));
        });
    }

    @Override
    public void onDeactivate() {
        channelPin.unsubscribe();
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
            bisqEasyTradeStatePin = null;
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
        if (hasBuyerAcceptedPriceSpecPin != null) {
            hasBuyerAcceptedPriceSpecPin.unsubscribe();
            hasBuyerAcceptedPriceSpecPin = null;
        }
        model.resetAll();
    }

    void onInterruptTrade() {
        BisqEasyTrade trade = model.getBisqEasyTrade().get();
        String message = switch (model.getTradeCloseType()) {
            case REJECT -> Res.get("bisqEasy.openTrades.rejectTrade.warning");
            case CANCEL -> {
                String part2 = Res.get("bisqEasy.openTrades.cancelTrade.warning.part2");
                yield trade.isSeller()
                        ? Res.get("bisqEasy.openTrades.cancelTrade.warning.seller", part2)
                        : Res.get("bisqEasy.openTrades.cancelTrade.warning.buyer", part2);
            }
            // We hide close button at the top-pane at the complete screen
            default -> throw new RuntimeException("Unexpected TradeCloseType " + model.getTradeCloseType());
        };

        new Popup().warning(message)
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(this::doInterruptTrade)
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void onViewTradeDetails() {
        BisqEasyOpenTradeChannel channel = model.getChannel().get();
        Optional<BisqEasyTrade> optionalBisqEasyTrade = BisqEasyServiceUtil.findTradeFromChannel(serviceProvider,
                channel);
        if (optionalBisqEasyTrade.isEmpty()) {
            model.resetAll();
            return;
        }

        BisqEasyTrade bisqEasyTrade = optionalBisqEasyTrade.get();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_TRADE_DETAILS,
                new TradeDetailsController.InitData(bisqEasyTrade, channel));
    }

    void onRejectPrice() {
        doInterruptTrade();
    }

    private void doInterruptTrade() {
        BisqEasyTrade trade = model.getBisqEasyTrade().get();
        String encoded;
        BisqEasyOpenTradeChannel channel = model.getChannel().get();
        String userName = channel.getMyUserIdentity().getUserName();
        switch (model.getTradeCloseType()) {
            case REJECT:
                encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName);
                channelService.sendTradeLogMessage(encoded, channel);
                bisqEasyTradeService.rejectTrade(trade);
                break;
            case CANCEL:
                encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.cancelled", userName);
                channelService.sendTradeLogMessage(encoded, channel);
                bisqEasyTradeService.cancelTrade(trade);
                break;
            case COMPLETED:
            default:
        }
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
        // bisqEasyTradeService.removeTrade, and then we would close the wrong channel.
        BisqEasyOpenTradeChannel chatChannel = model.getChannel().get();
        bisqEasyTradeService.removeTrade(model.getBisqEasyTrade().get());
        leavePrivateChatManager.leaveChannel(chatChannel);
    }

    void onExportTrade() {
        OpenTradesUtils.exportTrade(model.getBisqEasyTrade().get(), getView().getRoot().getScene());
    }

    void onReportToMediator() {
        OpenTradesUtils.reportToMediator(model.getChannel().get(),
                model.getBisqEasyTrade().get().getContract(),
                mediationRequestService, channelService);
    }

    void onAcceptSellersPriceButton() {
        model.getHasBuyerAcceptedSellersPriceSpec().set(true);
    }

    private void applyStateInfoVBox(@Nullable BisqEasyTradeState state) {
        applyCloseTradeReason(state);

        if (state == null) {
            model.getStateInfoVBox().set(null);
            return;
        }

        BisqEasyTrade trade = checkNotNull(model.getBisqEasyTrade().get());
        BisqEasyOpenTradeChannel channel = checkNotNull(model.getChannel().get());
        boolean isSeller = trade.isSeller();

        model.getPhaseAndInfoVisible().set(true);
        model.getError().set(false);
        model.getCancelled().set(false);
        model.getCancelButtonVisible().set(true);
        model.getIsTradeCompleted().set(false);

        switch (state) {
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
                if (trade.getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN) {
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
                if (trade.getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN) {
                    model.getStateInfoVBox().set(new BuyerStateMainChain3b(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerStateLightning3b(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            case BTC_CONFIRMED:
                model.getCancelButtonVisible().set(false);
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
                model.getCancelled().set(true);
                model.getCancelButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, false);
                break;

            case CANCELLED:
            case PEER_CANCELLED:
                model.getPhaseAndInfoVisible().set(false);
                model.getCancelled().set(true);
                model.getCancelButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, true);
                break;

            case FAILED:
                model.getPhaseAndInfoVisible().set(false);
                model.getError().set(true);
                model.getCancelButtonVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getErrorMessage().set(Res.get("bisqEasy.openTrades.failed",
                        model.getBisqEasyTrade().get().getErrorMessage()));
                break;
            case FAILED_AT_PEER:
                model.getPhaseAndInfoVisible().set(false);
                model.getCancelButtonVisible().set(false);
                model.getShowReportToMediatorButton().set(false);
                model.getError().set(true);
                model.getErrorMessage().set(Res.get("bisqEasy.openTrades.failedAtPeer",
                        model.getBisqEasyTrade().get().getPeersErrorMessage()));
                break;

            default:
                log.error("State {} not handled", state.name());
        }
    }

    private void applyTradeInterruptedInfo(BisqEasyTrade trade, boolean isCancelled) {
        boolean isMaker = trade.isMaker();
        boolean makerInterruptedTrade = trade.getInterruptTradeInitiator().get() == Role.MAKER;
        boolean selfInitiated = (makerInterruptedTrade && isMaker) ||
                (!makerInterruptedTrade && !isMaker);
        if (isCancelled) {
            model.getTradeInterruptedInfo().set(selfInitiated ?
                    Res.get("bisqEasy.openTrades.cancelled.self") :
                    Res.get("bisqEasy.openTrades.cancelled.peer"));
            model.getShowReportToMediatorButton().set(!selfInitiated);
        } else {
            model.getTradeInterruptedInfo().set(selfInitiated ?
                    Res.get("bisqEasy.openTrades.rejected.self") :
                    Res.get("bisqEasy.openTrades.rejected.peer"));
        }
    }

    private void applyCloseTradeReason(@Nullable BisqEasyTradeState state) {
        if (state == null) {
            model.setTradeCloseType(null);
            model.getInterruptTradeButtonText().set(null);
            return;
        }

        switch (state) {
            case INIT:
            case TAKER_SENT_TAKE_OFFER_REQUEST:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.REJECT);
                model.getInterruptTradeButtonText().set(Res.get("bisqEasy.openTrades.rejectTrade"));
                model.getCancelButtonVisible().set(true);
                break;
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
            case SELLER_CONFIRMED_FIAT_RECEIPT:
            case SELLER_SENT_BTC_SENT_CONFIRMATION:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_:
            case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
            case BUYER_SENT_FIAT_SENT_CONFIRMATION:
            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.CANCEL);
                model.getInterruptTradeButtonText().set(Res.get("bisqEasy.openTrades.cancelTrade"));
                model.getCancelButtonVisible().set(true);
                break;
            case BTC_CONFIRMED:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.COMPLETED);
                model.getCancelButtonVisible().set(false);
                break;
            case REJECTED:
            case PEER_REJECTED:
            case CANCELLED:
            case PEER_CANCELLED:
            case FAILED:
            case FAILED_AT_PEER:
                model.getCancelButtonVisible().set(false);
                break;

            default:
                log.error("State {} not handled", state.name());
        }
    }

    private void updateShouldShowSellerPriceApprovalOverlay() {
        model.getShouldShowSellerPriceApprovalOverlay().set(
                model.getBisqEasyTrade().get() != null
                        && model.getBisqEasyTrade().get().isBuyer()
                        && model.getBisqEasyTrade().get().isMaker()
                        && tradePhaseBox.getPhaseIndex() == 0
                        && requiresSellerPriceAcceptance()
                        && !model.getHasBuyerAcceptedSellersPriceSpec().get()
        );
    }

    private boolean requiresSellerPriceAcceptance() {
        PriceSpec buyerPriceSpec = model.getBisqEasyTrade().get().getOffer().getPriceSpec();
        PriceSpec sellerPriceSpec = model.getBisqEasyTrade().get().getContract().getAgreedPriceSpec();
        boolean priceSpecChanged = !buyerPriceSpec.equals(sellerPriceSpec);

        Set<BisqEasyTradeState> validStatesToRejectPrice = Set.of(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
        );
        boolean isInValidStateToRejectPrice = validStatesToRejectPrice.contains(model.getBisqEasyTrade().get().tradeStateObservable().get());
        return priceSpecChanged && isInValidStateToRejectPrice;
    }
}
