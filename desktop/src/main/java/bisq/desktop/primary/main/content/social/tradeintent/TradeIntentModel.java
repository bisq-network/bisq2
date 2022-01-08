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
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
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
import java.util.stream.Collectors;

@Slf4j
@Getter
public class TradeIntentModel implements Model {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private final ObservableList<TradeIntentListItem> tradeIntentListItems = FXCollections.observableArrayList();
    private final FilteredList<TradeIntentListItem> filteredTradeIntentListItems = new FilteredList<>(tradeIntentListItems);
    private final SortedList<TradeIntentListItem> sortedTradeIntentListItems = new SortedList<>(filteredTradeIntentListItems);
    private final Optional<DataService> dataService;
    private DataService.Listener dataListener;

    public TradeIntentModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();
        dataService = networkService.getServiceNodesByTransport().getDataService();
        dataService.ifPresent(dataService -> {
            dataListener = new DataService.Listener() {
                @Override
                public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                    UIThread.run(() -> tradeIntentListItems.add(new TradeIntentListItem(networkPayload)));
                }

                @Override
                public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
                    UIThread.run(() -> tradeIntentListItems.remove(new TradeIntentListItem(networkPayload)));
                }
            };
            dataService.addListener(dataListener);
            fillDataListItems(dataService);
        });

        //todo listen on bootstrap
        UIScheduler.run(this::requestInventory).after(2000);
    }

    private void fillDataListItems(DataService dataService) {
        tradeIntentListItems.addAll(dataService.getNetworkPayloads("TradeIntent")
                .map(TradeIntentListItem::new)
                .collect(Collectors.toList()));
    }

    public void onViewAttached() {
    }

    public void onViewDetached() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    void requestInventory() {
        // We get updated our data listener once we get responses
        networkService.requestInventory("TradeIntent");
    }

    StringProperty addData(String ask, String bid) {
        StringProperty resultProperty = new SimpleStringProperty("Create Servers for node ID");
        Identity identity = identityService.getOrCreateIdentity("tradeIntent");
        String keyId = identity.keyId();
        networkService.addData(new TradeIntent(ask, bid, new Date().getTime()), identity.nodeId(), keyId)
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
}
