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
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedNetworkIdPayload;
import bisq.security.KeyPairService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class TradeIntentModel implements Model {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private final ObservableList<TradeIntentListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<TradeIntentListItem> filteredItems = new FilteredList<>(listItems);
    private final SortedList<TradeIntentListItem> sortedItems = new SortedList<>(filteredItems);
    private final Optional<DataService> dataService;
    private Optional<DataService.Listener> dataListener = Optional.empty();

    public TradeIntentModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();
        dataService = networkService.getDataService();

        //todo listen on bootstrap
        UIScheduler.run(this::requestInventory).after(2000);
    }

    public void onViewAttached() {
        dataService.ifPresent(dataService -> {
            dataListener = Optional.of(new DataService.Listener() {
                @Override
                public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedNetworkIdPayload payload && payload.getData() instanceof TradeIntent) {
                        UIThread.run(() -> listItems.add(new TradeIntentListItem(payload)));
                    }
                }

                @Override
                public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
                    if (networkPayload instanceof AuthenticatedNetworkIdPayload payload && payload.getData() instanceof TradeIntent) {
                        UIThread.run(() -> listItems.remove(new TradeIntentListItem(payload)));
                    }
                }
            });
            dataService.addListener(dataListener.get());

            listItems.setAll(dataService.getAuthenticatedPayloadByStoreName("TradeIntent")
                    .filter(payload -> payload instanceof AuthenticatedNetworkIdPayload)
                    .map(payload -> (AuthenticatedNetworkIdPayload) payload)
                    .map(TradeIntentListItem::new)
                    .collect(Collectors.toList()));
        });
    }

    public void onViewDetached() {
        dataService.ifPresent(dataService -> dataListener.ifPresent(dataService::removeListener));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void requestInventory() {
        // We get updated our data listener once we get responses
        networkService.requestInventory("TradeIntent");
    }

    StringProperty addData(String userId, String ask, String bid) {
        StringProperty resultProperty = new SimpleStringProperty("Create Servers for node ID");
        TradeIntent tradeIntent = new TradeIntent(UUID.randomUUID().toString().substring(0, 8), userId, ask, bid, new Date().getTime());
        Identity identity = identityService.getOrCreateIdentity(tradeIntent.id());
        String keyId = identity.keyId();
        networkService.addData(tradeIntent, identity.nodeId(), keyId)
                .whenComplete((broadCastResultFutures, throwable) -> {
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadCastResult, throwable2) -> {
                            //todo add states to networkService
                            UIThread.run(() -> {
                                if (throwable2 == null) {
                                    resultProperty.set("Data added. Broadcast result: " + broadCastResult);
                                } else {
                                    resultProperty.set("Error at add data: " + throwable);
                                }
                            });
                        });
                    });
                });
        return resultProperty;
    }

    public void removeTradeIntent(TradeIntentListItem item) {
        StringProperty resultProperty = new SimpleStringProperty("Remove data");
        Identity identity = identityService.getOrCreateIdentity(item.getTradeIntent().id());
        String keyId = identity.keyId();
        networkService.removeData(item.getPayload().getData(), identity.nodeId(), keyId)
                .whenComplete((broadCastResultFutures, throwable) -> {
                    broadCastResultFutures.forEach(broadCastResultFuture -> {
                        broadCastResultFuture.whenComplete((broadCastResult, throwable2) -> {
                            //todo add states to networkService
                            UIThread.run(() -> {
                                if (throwable2 == null) {
                                    resultProperty.set("Remove added. Broadcast result: " + broadCastResult);
                                } else {
                                    resultProperty.set("Error at remove data: " + throwable);
                                }
                            });
                        });
                    });
                });
    }

    boolean isMyTradeIntent(TradeIntentListItem item) {
        return keyPairService.findKeyPair(item.getNetworkId().getPubKey().keyId()).isPresent();
    }

    String getActionButtonTitle(TradeIntentListItem item) {
        return isMyTradeIntent(item) ? Res.common.get("remove") : Res.common.get("contact");
    }
}
