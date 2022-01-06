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

package network.misq.desktop.main.content.networkinfo;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.Model;
import network.misq.desktop.main.content.networkinfo.transport.TransportTypeView;
import network.misq.i18n.Res;
import network.misq.identity.Identity;
import network.misq.identity.IdentityService;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.message.TextData;
import network.misq.network.p2p.message.TextMessage;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.confidential.ConfidentialMessageService;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.security.KeyPairService;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public class NetworkInfoModel implements Model {
    private final NetworkService networkService;

    private final BooleanProperty clearNetDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty torDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty i2pDisabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<Optional<TransportTypeView>> transportTypeView = new SimpleObjectProperty<>();
    private final Set<Transport.Type> supportedTransportTypes;
    private final IdentityService identityService;
    @Setter
    private Optional<Transport.Type> selectedTransportType = Optional.empty();


    private final KeyPairService keyPairService;
    private final ObservableList<DataListItem> dataListItems = FXCollections.observableArrayList();
    private final FilteredList<DataListItem> filteredDataListItems = new FilteredList<>(dataListItems);
    private final SortedList<DataListItem> sortedDataListItems = new SortedList<>(filteredDataListItems);
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.common.get("na"));
    private final StringProperty nodeIdString = new SimpleStringProperty();
    private final StringProperty messageReceiver = new SimpleStringProperty();
    private final StringProperty receivedMessages = new SimpleStringProperty("");
    private final Optional<DataService> dataService;
    private Optional<NetworkId> selectedNetworkId = Optional.empty();
    private DataService.Listener dataListener;

    public NetworkInfoModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();

        supportedTransportTypes = networkService.getSupportedTransportTypes();

        clearNetDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.CLEAR));
        torDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.I2P));

        keyPairService = serviceProvider.getKeyPairService();

        dataService = networkService.getServiceNodesByTransport().getDataService();
        dataService.ifPresent(dataService -> {
            dataListener = new DataService.Listener() {
                @Override
                public void onNetworkDataAdded(NetworkPayload networkPayload) {
                    UIThread.run(() -> dataListItems.add(new DataListItem(networkPayload)));
                }

                @Override
                public void onNetworkDataRemoved(NetworkPayload networkPayload) {
                    UIThread.run(() -> dataListItems.remove(new DataListItem(networkPayload)));
                }
            };
            dataService.addListener(dataListener);

            dataListItems.addAll(dataService.getAllAuthenticatedPayload()
                    .map(DataListItem::new)
                    .collect(Collectors.toList()));
        });

        //todo
        networkService.addMessageListener(new Node.Listener() {
            @Override
            public void onMessage(Message message, Connection connection, String nodeId) {
                UIThread.run(() -> receivedMessages.set(receivedMessages.get() + "NodeId: " + nodeId + "; message: " + message.toString() + "\n"));
            }

            @Override
            public void onConnection(Connection connection) {
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
            }
        });
    }

    public void activate() {
    }

    public void deactivate() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    StringProperty addData(String dataText, String domainId) {
        StringProperty resultProperty = new SimpleStringProperty("Create Servers for node ID");
        Identity identity = identityService.getOrCreateIdentity(domainId);
        String keyId = identity.keyId();
        networkService.addDataAsync(new TextData(dataText), identity.nodeId(), keyId)
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

    CompletableFuture<String> sendMessage(String message) {
        checkArgument(selectedNetworkId.isPresent(), "Network ID must be set before calling sendMessage");
        NetworkId receiverNetworkId = selectedNetworkId.get();
        KeyPair senderKeyPair = keyPairService.getOrCreateKeyPair(KeyPairService.DEFAULT);
        CompletableFuture<String> future = new CompletableFuture<>();
        String senderNodeId = selectedNetworkId.get().getNodeId();
        networkService.confidentialSendAsync(new TextMessage(message), receiverNetworkId, senderKeyPair, senderNodeId)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        resultMap.entrySet().stream().forEach(typeResultEntry -> {
                            Transport.Type transportType = typeResultEntry.getKey();
                            ConfidentialMessageService.Result result = resultMap.get(transportType);
                            result.getMailboxFuture().forEach(broadcastFuture -> broadcastFuture.whenComplete((broadcastResult, error) -> {
                                if (error == null) {
                                    future.complete(result.getState() + "; " + broadcastResult.toString());
                                } else {
                                    String value = result.getState().toString();
                                    if (result.getState() == ConfidentialMessageService.State.FAILED) {
                                        value += " with Error: " + result.getErrorMsg();
                                    }
                                    future.complete(value);
                                }
                            }));
                        });
                    }
                });
        return future;
    }


    public void applyNetworkId(Optional<NetworkId> networkId) {
        this.selectedNetworkId = networkId;
        nodeIdString.set(networkId.map(NetworkId::getNodeId)
                .orElse(Res.common.get("na")));
        messageReceiver.set(networkId.map(n -> n.addressByNetworkType().values().stream().findAny().orElseThrow().getFullAddress())
                .orElse(Res.common.get("na")));
    }
}
