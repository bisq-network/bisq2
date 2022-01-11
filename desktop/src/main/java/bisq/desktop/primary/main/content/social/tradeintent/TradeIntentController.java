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

package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.hangout.ChatUser;
import bisq.desktop.primary.main.content.social.user.UserController;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NodeIdAndKeyId;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeIntentController implements Controller, UserController.Listener {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Optional<DataService> dataService;
    private final TradeIntentModel model;
    @Getter
    private final TradeIntentView view;
    private final UserController userController;
    private Optional<DataService.Listener> dataListener = Optional.empty();

    public TradeIntentController(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        userController = new UserController(serviceProvider);
        dataService = networkService.getDataService();

        model = new TradeIntentModel(serviceProvider);
        view = new TradeIntentView(model, this, userController.getView());
        //todo listen on bootstrap
        UIScheduler.run(this::requestInventory).after(2000);
    }

    void requestInventory() {
        // We get updated our data listener once we get responses
        networkService.requestInventory("TradeIntent");
    }

    @Override
    public void onViewAttached() {
        userController.addListener(this);
        model.setMyChatIdentity(userController.getModel().getSelectedChatUser().get());

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
                    .filter(payload -> payload instanceof AuthenticatedPayload)
                    .map(payload -> (AuthenticatedPayload) payload)
                    .map(TradeIntentListItem::new)
                    .collect(Collectors.toList()));
        });
    }

    @Override
    public void onViewDetached() {
        userController.removeListener(this);
    }

    void onAddTradeIntent(String ask, String bid) {
        checkNotNull(model.getMyChatIdentity().get(), "myChatIdentity must be set");
        NodeIdAndKeyId nodeIdAndKeyId = identityService.getNodeIdAndKeyId(model.getMyChatIdentity().get().id());
        TradeIntent tradeIntent = model.createTradeIntent(ask, bid);
        model.getAddDataResultProperty().set("...");
        log.error("Add {}", nodeIdAndKeyId);
        networkService.addData(tradeIntent, nodeIdAndKeyId)
                .whenComplete((broadCastResultFutures, throwable) -> {
                    if (throwable != null) {
                        UIThread.run(() -> model.setAddTradeIntentError(tradeIntent, throwable));
                        return;
                    }
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadcastResult, throwable2) -> {
                            if (throwable2 != null) {
                                UIThread.run(() -> model.setAddTradeIntentError(tradeIntent, throwable2));
                                return;
                            }
                            UIThread.run(() -> model.setAddTradeIntentResult(tradeIntent, broadcastResult));
                        });
                    });
                });
    }

    @Override
    public void onSelectChatUser(ChatUser chatUser) {
        model.setMyChatIdentity(chatUser);
    }

    public void onActionButtonClicked(TradeIntentListItem item) {
        if (model.isMyTradeIntent(item)) {
            onRemoveTradeIntent(item);
        } else {
            onContactPeer(item);
        }
    }

    private void onContactPeer(TradeIntentListItem item) {
        Navigation.navigateTo(NavigationTarget.HANGOUT, item.getTradeIntent());
    }

    private void onRemoveTradeIntent(TradeIntentListItem item) {
        checkNotNull(model.getMyChatIdentity().get(), "myChatIdentity must be set");
        NodeIdAndKeyId nodeIdAndKeyId = identityService.getNodeIdAndKeyId(model.getMyChatIdentity().get().id());
        log.error("remove {}", nodeIdAndKeyId);
        networkService.removeData(item.getPayload().getData(), nodeIdAndKeyId)
                .whenComplete((broadCastResultFutures, throwable) -> {
                    if (throwable != null) {
                        UIThread.run(() -> model.setRemoveTradeIntentError(item.getTradeIntent(), throwable));
                        return;
                    }
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadcastResult, throwable2) -> {
                            if (throwable2 != null) {
                                UIThread.run(() -> model.setRemoveTradeIntentError(item.getTradeIntent(), throwable2));
                                return;
                            }
                            UIThread.run(() -> model.setRemoveTradeIntentResult(item.getTradeIntent(), broadcastResult));
                        });
                    });
                });
    }
}
