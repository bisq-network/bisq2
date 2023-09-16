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

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.bisq_easy.components.TradeDataHeader;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states.*;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final ServiceProvider serviceProvider;
    private final TradePhaseBox tradePhaseBox;
    private final BiConsumer<BisqEasyTrade, BisqEasyOpenTradeChannel> onTradeClosedHandler;
    private final TradeDataHeader tradeDataHeader;
    private Pin bisqEasyTradeStatePin;
    private Subscription channelPin;

    public TradeStateController(ServiceProvider serviceProvider,
                                BiConsumer<BisqEasyTrade, BisqEasyOpenTradeChannel> onTradeClosedHandler) {
        this.serviceProvider = serviceProvider;
        this.onTradeClosedHandler = onTradeClosedHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();

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
                model.reset();
                return;
            }
            Optional<BisqEasyTrade> optionalBisqEasyTrade = BisqEasyServiceUtil.findTradeFromChannel(serviceProvider, channel);
            if (optionalBisqEasyTrade.isEmpty()) {
                model.reset();
                return;
            }
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
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
            bisqEasyTradeStatePin = null;
        }
        model.reset();
    }

    void onCloseTrade() {
        boolean isSeller = model.getBisqEasyTrade().get().isSeller();
        String messageKey;
        switch (model.getTradeCloseType()) {
            case REJECT:
                messageKey = isSeller ? "bisqEasy.openTrades.closeTrade.warning.seller.reject" :
                        "bisqEasy.openTrades.closeTrade.warning.buyer.reject";
                break;
            case CANCEL:
                messageKey = isSeller ? "bisqEasy.openTrades.closeTrade.warning.seller.cancel" :
                        "bisqEasy.openTrades.closeTrade.warning.buyer.cancel";
                break;
            case COMPLETED:
                messageKey = "bisqEasy.openTrades.closeTrade.warning.completed";
                break;
            default:
                return;
        }
        new Popup().warning(Res.get(messageKey, Res.get("bisqEasy.openTrades.closeTrade.warning.dataDeleted")))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> {
                    BisqEasyOpenTradeChannel channel = model.getChannel().get();
                    BisqEasyTrade bisqEasyTrade = model.getBisqEasyTrade().get();
                    onTradeClosedHandler.accept(bisqEasyTrade, channel);
                })
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    private void applyStateInfoVBox(@Nullable BisqEasyTradeState state) {
        applyCloseTradeReason(state);

        if (state == null) {
            model.getStateInfoVBox().set(null);
            return;
        }

        BisqEasyTrade bisqEasyTrade = checkNotNull(model.getBisqEasyTrade().get());
        BisqEasyOpenTradeChannel channel = checkNotNull(model.getChannel().get());
        boolean isSeller = bisqEasyTrade.isSeller();
        switch (state) {
            case INIT:
                break;
            case TAKER_SENT_TAKE_OFFER_REQUEST:
            case MAKER_SENT_TAKE_OFFER_RESPONSE:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState1(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState1(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                }
                break;
            case SELLER_SENT_ACCOUNT_DATA:
            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new SellerState2(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;

            case BUYER_RECEIVED_ACCOUNT_DATA:
            case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState2(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;

            case BUYER_SENT_BTC_ADDRESS:
            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState3(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;

            case SELLER_RECEIVED_BTC_ADDRESS:
            case SELLER_CONFIRMED_FIAT_RECEIPT:
                model.getStateInfoVBox().set(new SellerState3(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;
            case SELLER_SENT_BTC_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new SellerState4(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;
            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                model.getStateInfoVBox().set(new BuyerState4(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                break;
            case BTC_CONFIRMED:
                if (isSeller) {
                    model.getStateInfoVBox().set(new SellerState5(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                } else {
                    model.getStateInfoVBox().set(new BuyerState5(serviceProvider, bisqEasyTrade, channel).getView().getRoot());
                }
                break;
            default:
                log.error(state.name());
        }
    }

    private void applyCloseTradeReason(@Nullable BisqEasyTradeState state) {
        if (state == null) {
            model.setTradeCloseType(null);
            model.getCloseButtonText().set(null);
            return;
        }

        switch (state) {
            case INIT:
            case TAKER_SENT_TAKE_OFFER_REQUEST:
            case MAKER_SENT_TAKE_OFFER_RESPONSE:
            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.REJECT);
                model.getCloseButtonText().set(model.getBisqEasyTrade().get().isMaker() ?
                        Res.get("bisqEasy.openTrades.closeTrade.reject.maker") :
                        Res.get("bisqEasy.openTrades.closeTrade.reject.taker"));
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
                model.getCloseButtonText().set(Res.get("bisqEasy.openTrades.closeTrade.cancel"));
                break;
            case BTC_CONFIRMED:
                model.setTradeCloseType(TradeStateModel.TradeCloseType.COMPLETED);
                model.getCloseButtonText().set(Res.get("bisqEasy.openTrades.closeTrade.completed"));
                break;
        }
    }
}
