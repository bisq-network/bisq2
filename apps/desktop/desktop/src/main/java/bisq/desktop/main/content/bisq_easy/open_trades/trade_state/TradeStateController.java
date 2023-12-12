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

import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeSelectionService;
import bisq.common.observable.Pin;
import bisq.contract.Role;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.components.TradeDataHeader;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states.*;
import bisq.i18n.Res;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;

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
    private final BisqEasyOpenTradeSelectionService selectionService;
    private final MediationRequestService mediationRequestService;
    private Pin bisqEasyTradeStatePin, isInMediationPin;
    private Subscription channelPin;

    public TradeStateController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        ChatService chatService = serviceProvider.getChatService();
        channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesSelectionService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();

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
                    UIThread.run(() -> applyStateInfoVBox(state)));
        });
    }

    @Override
    public void onDeactivate() {
        channelPin.unsubscribe();
        if (isInMediationPin != null) {
            isInMediationPin.unbind();
            isInMediationPin = null;
        }
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
            bisqEasyTradeStatePin = null;
        }

        model.resetAll();
    }

    void onInterruptTrade() {
        BisqEasyTrade trade = model.getBisqEasyTrade().get();
        boolean isSeller = trade.isSeller();
        String part2;
        String message;
        switch (model.getTradeCloseType()) {
            case REJECT:
                part2 = Res.get("bisqEasy.openTrades.rejectTrade.warning.part2");
                message = isSeller ?
                        Res.get("bisqEasy.openTrades.rejectTrade.warning.seller", part2) :
                        Res.get("bisqEasy.openTrades.rejectTrade.warning.buyer", part2);
                break;
            case CANCEL:
                part2 = Res.get("bisqEasy.openTrades.cancelTrade.warning.part2");
                message = isSeller ?
                        Res.get("bisqEasy.openTrades.cancelTrade.warning.seller", part2) :
                        Res.get("bisqEasy.openTrades.cancelTrade.warning.buyer", part2);
                break;
            case COMPLETED:
                // We hide close button at the top-pane at the complete screen
            default:
                throw new RuntimeException("Unexpected TradeCloseType " + model.getTradeCloseType());
        }

        new Popup().warning(message)
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> {
                    try {
                        switch (model.getTradeCloseType()) {
                            case REJECT:
                                model.getTradeInterrupted().set(true);
                                model.getInterruptTradeButtonVisible().set(false);
                                applyTradeInterruptedInfo(trade, false);
                                channelService.sendSystemMessage(Res.get("bisqEasy.openTrades.systemMessage.rejected"),
                                        model.getChannel().get());
                                bisqEasyTradeService.rejectTrade(trade);
                                break;
                            case CANCEL:
                                model.getTradeInterrupted().set(true);
                                model.getInterruptTradeButtonVisible().set(false);
                                applyTradeInterruptedInfo(trade, true);
                                channelService.sendSystemMessage(Res.get("bisqEasy.openTrades.systemMessage.cancelled"),
                                        model.getChannel().get());
                                bisqEasyTradeService.cancelTrade(trade);
                                break;
                            case COMPLETED:
                            default:
                        }
                    } catch (TradeException e) {
                        new Popup().error(e).show();
                    }
                })
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void onCloseTrade() {
        new Popup().warning(Res.get("bisqEasy.openTrades.closeTrade.warning.interrupted"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> {
                    BisqEasyTrade trade = model.getBisqEasyTrade().get();
                    BisqEasyOpenTradeChannel channel = model.getChannel().get();
                    bisqEasyTradeService.removeTrade(trade);
                    channelService.leaveChannel(channel);
                    selectionService.getSelectedChannel().set(null);
                })
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void onExportTrade() {
        OpenTradesUtils.exportTrade(model.getBisqEasyTrade().get(), getView().getRoot().getScene());
    }

    void onReportToMediator() {
        OpenTradesUtils.reportToMediator(model.getChannel().get(),
                model.getBisqEasyTrade().get().getContract(),
                mediationRequestService);
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
        switch (state) {
            case INIT:
                break;
            case TAKER_SENT_TAKE_OFFER_REQUEST:
            case MAKER_SENT_TAKE_OFFER_RESPONSE:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState1(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState1(serviceProvider, trade, channel).getView().getRoot());
                }
                break;
            case SELLER_SENT_ACCOUNT_DATA:
            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new SellerState2(serviceProvider, trade, channel).getView().getRoot());
                break;

            case BUYER_RECEIVED_ACCOUNT_DATA:
            case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState2(serviceProvider, trade, channel).getView().getRoot());
                break;

            case BUYER_SENT_BTC_ADDRESS:
            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState3(serviceProvider, trade, channel).getView().getRoot());
                break;

            case SELLER_RECEIVED_BTC_ADDRESS:
            case SELLER_CONFIRMED_FIAT_RECEIPT:
                model.getStateInfoVBox().set(new SellerState3(serviceProvider, trade, channel).getView().getRoot());
                break;
            case SELLER_SENT_BTC_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new SellerState4(serviceProvider, trade, channel).getView().getRoot());
                break;
            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState4(serviceProvider, trade, channel).getView().getRoot());
                break;
            case BTC_CONFIRMED:
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState5(serviceProvider, trade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState5(serviceProvider, trade, channel).getView().getRoot());
                }
                break;

            case REJECTED:
                model.getTradeInterrupted().set(true);
                model.getInterruptTradeButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, false);
                break;
            case CANCELLED:
                model.getTradeInterrupted().set(true);
                model.getInterruptTradeButtonVisible().set(false);
                applyTradeInterruptedInfo(trade, true);
                break;
            default:
                log.error(state.name());
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
            case MAKER_SENT_TAKE_OFFER_RESPONSE:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.REJECT);
                model.getInterruptTradeButtonText().set(Res.get("bisqEasy.openTrades.rejectTrade"));
                model.getInterruptTradeButtonVisible().set(true);
                break;
            case SELLER_SENT_ACCOUNT_DATA:
            case BUYER_RECEIVED_ACCOUNT_DATA:
            case BUYER_SENT_FIAT_SENT_CONFIRMATION:
            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
            case BUYER_SENT_BTC_ADDRESS:
            case SELLER_RECEIVED_BTC_ADDRESS:
            case SELLER_CONFIRMED_FIAT_RECEIPT:
            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
            case SELLER_SENT_BTC_SENT_CONFIRMATION:
            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.CANCEL);
                model.getInterruptTradeButtonText().set(Res.get("bisqEasy.openTrades.cancelTrade"));
                model.getInterruptTradeButtonVisible().set(true);
                break;
            case BTC_CONFIRMED:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.COMPLETED);
                model.getInterruptTradeButtonVisible().set(false);
                break;

            case REJECTED:
            case CANCELLED:
                model.getInterruptTradeButtonVisible().set(false);
                break;
        }
    }
}
