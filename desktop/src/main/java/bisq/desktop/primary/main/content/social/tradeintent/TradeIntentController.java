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

import bisq.application.DefaultApplicationService;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.social.user.ChatUserController;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.social.chat.ChatService;
import bisq.social.intent.TradeIntent;
import bisq.social.intent.TradeIntentListingsService;
import bisq.social.intent.TradeIntentService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeIntentController implements Controller/*, ChatService.Listener*/ {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final Optional<DataService> dataService;
    private final TradeIntentModel model;
    @Getter
    private final TradeIntentView view;
    private final TradeIntentListingsService tradeIntentListingsService;
    private final TradeIntentService tradeIntentService;
    private Optional<DataService.Listener> dataListener = Optional.empty();
    private int tradeIntentBindingKey;

    public TradeIntentController(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        identityService = applicationService.getIdentityService();
        chatService = applicationService.getChatService();
        tradeIntentService = applicationService.getTradeIntentService();
        tradeIntentListingsService = applicationService.getTradeIntentListingsService();
        dataService = networkService.getDataService();

        ChatUserController chatUserController = new ChatUserController(applicationService);
        model = new TradeIntentModel(applicationService);
        view = new TradeIntentView(model, this, chatUserController.getView());
    }

    @Override
    public void onViewAttached() {
        //todo
        String userName = "Natoshi Sakamoto  ";
        identityService.getOrCreateIdentity(userName)
                .whenComplete((identity, t) -> model.selectedUserIdentity.set(identity));

        tradeIntentBindingKey = tradeIntentListingsService.getTradeIntents().bind(model.getListItems(),
                TradeIntentListItem::new,
                UIThread::run);
        
       /* dataService.ifPresent(dataService -> {
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
                    .map(TradeIntentListItem::new)
                    .collect(Collectors.toList()));
        });*/
    }

    @Override
    public void onViewDetached() {
        tradeIntentListingsService.getTradeIntents().unbind(tradeIntentBindingKey);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onCreateTradeIntent(String ask, String bid) {
        tradeIntentService.createTradeIntent(ask, bid)
                .whenComplete((tradeIntent, throwable) -> {
                    if (throwable == null) {
                        model.getTradeIntentProperty().set(tradeIntent);
                        onPublishTradeIntent();
                    } else {
                        //todo provide error to UI
                    }
                });
    }

    public void onPublishTradeIntent() {
        tradeIntentService.publishTradeIntent(model.getTradeIntentProperty().get());
    }

    public void onActionButtonClicked(TradeIntentListItem item) {
        if (model.isMyTradeIntent(item)) {
            onRemoveTradeIntent(item);
        } else {
            onContactPeer(item);
        }
    }

    private void onRemoveTradeIntent(TradeIntentListItem item) {
        TradeIntent tradeIntent = item.getTradeIntent();
        tradeIntentService.removeMyTradeIntent(tradeIntent)
                .whenComplete((broadCastResultFutures, throwable2) -> {
                    if (throwable2 != null) {
                        // UIThread.run(() -> model.setRemoveOfferError(tradeIntent, throwable2));
                        return;
                    }
                    broadCastResultFutures.entrySet().forEach(broadCastResultFuture -> {
                        broadCastResultFuture.getValue().whenComplete((broadcastResult, throwable3) -> {
                            if (throwable3 != null) {
                                // UIThread.run(() -> model.setRemoveOfferError(tradeIntent, throwable3));
                                return;
                            }
                            // UIThread.run(() -> model.setRemoveOfferResult(tradeIntent, broadcastResult));
                        });
                    });
                });
    }

    private void onContactPeer(TradeIntentListItem item) {
        Navigation.navigateTo(NavigationTarget.HANGOUT, item.getTradeIntent());
    }
}
