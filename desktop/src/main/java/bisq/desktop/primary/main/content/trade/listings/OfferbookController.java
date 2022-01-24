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

package bisq.desktop.primary.main.content.trade.listings;

import bisq.application.DefaultServiceProvider;
import bisq.common.util.StringUtils;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.user.ChatUserController;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.social.chat.ChatPeer;
import bisq.social.chat.ChatService;
import bisq.social.intent.TradeIntent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookController implements Controller {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final Optional<DataService> dataService;
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    private Optional<DataService.Listener> dataListener = Optional.empty();

    public OfferbookController(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        chatService = serviceProvider.getChatService();
        dataService = networkService.getDataService();

        ChatUserController chatUserController = new ChatUserController(serviceProvider);
        model = new OfferbookModel(serviceProvider);
        view = new OfferbookView(model, this, chatUserController.getView());
    }

    @Override
    public void onViewAttached() {
        dataService.ifPresent(dataService -> {
            dataListener = Optional.of(new DataService.Listener() {
                @Override
                public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedPayload payload &&
                            payload.getData() instanceof TradeIntent) {
                        UIThread.run(() -> model.addPayload(payload));
                    }
                }

                @Override
                public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedPayload payload && payload.getData() instanceof TradeIntent) {
                        UIThread.run(() -> model.removePayload(payload));
                    }
                }
            });
            dataService.addListener(dataListener.get());

            model.fillTradeIntentListItem(dataService.getAuthenticatedPayloadByStoreName("TradeIntent")
                    .map(OfferItem::new)
                    .collect(Collectors.toList()));
        });
    }

    @Override
    public void onViewDetached() {
        //    chatService.removeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onAddTradeIntent(String ask, String bid) {
        //todo select user
        String tradeIntentId = StringUtils.createUid();
        identityService.getOrCreateIdentity(tradeIntentId)
                .whenComplete((identity, throwable1) -> {
                    if (throwable1 != null) {
                        UIThread.run(() -> model.setAddTradeIntentError(throwable1));
                        return;
                    }
                    String userName = chatService.findUserName(tradeIntentId).orElse("Maker@" + StringUtils.truncate(tradeIntentId));
                    ChatPeer maker = new ChatPeer(userName, identity.networkId());
                    TradeIntent tradeIntent = new TradeIntent(tradeIntentId, maker, ask, bid, new Date().getTime());
                    model.getAddDataResultProperty().set("...");
                    log.error("onAddTradeIntent nodeIdAndKeyPair={}", identity.getNodeIdAndKeyPair());
                    networkService.addData(tradeIntent, identity.getNodeIdAndKeyPair())
                            .whenComplete((broadCastResultFutures, throwable2) -> {
                                if (throwable2 != null) {
                                    UIThread.run(() -> model.setAddTradeIntentError(tradeIntent, throwable2));
                                    return;
                                }
                                broadCastResultFutures.forEach(broadCastResultFuture -> {
                                    broadCastResultFuture.whenComplete((broadcastResult, throwable3) -> {
                                        if (throwable3 != null) {
                                            UIThread.run(() -> model.setAddTradeIntentError(tradeIntent, throwable3));
                                            return;
                                        }
                                        UIThread.run(() -> model.setAddTradeIntentResult(tradeIntent, broadcastResult));
                                    });
                                });
                            });
                });
    }

    public void onActionButtonClicked(OfferItem item) {
        if (model.isMyTradeIntent(item)) {
            onRemoveTradeIntent(item);
        } else {
            onContactPeer(item);
        }
    }

    private void onRemoveTradeIntent(OfferItem item) {
        Identity identity = identityService.findActiveIdentity(item.getId()).orElseThrow();
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        log.error("onRemoveTradeIntent nodeIdAndKeyPair={}", identity.getNodeIdAndKeyPair());
        networkService.removeData(item.getPayload().getData(), identity.getNodeIdAndKeyPair())
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        UIThread.run(() -> model.setRemoveTradeIntentError(item.getTradeIntent(), throwable2));
                        return;
                    }
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadcastResult, throwable3) -> {
                            if (throwable3 != null) {
                                UIThread.run(() -> model.setRemoveTradeIntentError(item.getTradeIntent(), throwable3));
                                return;
                            }
                            UIThread.run(() -> model.setRemoveTradeIntentResult(item.getTradeIntent(), broadcastResult));
                        });
                    });
                });
    }

    private void onContactPeer(OfferItem item) {
        Navigation.navigateTo(NavigationTarget.HANGOUT, item.getTradeIntent());
    }
}
