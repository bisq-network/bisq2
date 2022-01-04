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

package network.misq.desktop.main.content.networkinfo.transport;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.application.DefaultServiceProvider;
import network.misq.common.data.Pair;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.Model;
import network.misq.i18n.Res;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.message.TextMessage;
import network.misq.network.p2p.node.*;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.confidential.ConfidentialMessageService;
import network.misq.network.p2p.services.data.AuthenticatedTextPayload;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.security.KeyPairService;
import network.misq.security.PubKey;

import java.security.KeyPair;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class TransportTypeModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;
    private final Transport.Type transportType;
    private final ObservableList<ConnectionListItem> connectionListItems = FXCollections.observableArrayList();
    private final FilteredList<ConnectionListItem> filteredConnectionListItems = new FilteredList<>(connectionListItems);
    private final SortedList<ConnectionListItem> sortedConnectionListItems = new SortedList<>(filteredConnectionListItems);
    private final ObservableList<DataListItem> dataListItems = FXCollections.observableArrayList();
    private final FilteredList<DataListItem> filteredDataListItems = new FilteredList<>(dataListItems);
    private final SortedList<DataListItem> sortedDataListItems = new SortedList<>(filteredDataListItems);
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.common.get("na"));
    private final StringProperty nodeIdString = new SimpleStringProperty();
    private final StringProperty messageReceiver = new SimpleStringProperty();
    private final StringProperty receivedMessages = new SimpleStringProperty("");
    private final Collection<Node> allNodes;
    private final Node defaultNode;
    private final Optional<DataService> dataService;
    private final NodesById nodesById;
    private final Optional<ConfidentialMessageService> confidentialMessageService;
    private Optional<NetworkId> selectedNetworkId = Optional.empty();
    private DataService.Listener dataListener;
    private Map<String, Node.Listener> nodeListenersByNodeId = new HashMap<>();


    public TransportTypeModel(DefaultServiceProvider serviceProvider, Transport.Type transportType) {
        networkService = serviceProvider.getNetworkService();
        keyPairService = serviceProvider.getKeyPairService();
        this.transportType = transportType;

        Optional<ServiceNode> serviceNode = networkService.findServiceNode(transportType);
        checkArgument(serviceNode.isPresent(),
                "ServiceNode must be present");

        defaultNode = serviceNode.get().getDefaultNode();
        defaultNode.findMyAddress().ifPresent(e -> myDefaultNodeAddress.set(e.getFullAddress()));

        nodesById = serviceNode.get().getNodesById();
        allNodes = nodesById.getAllNodes();

        connectionListItems.setAll(allNodes.stream()
                .flatMap(node -> node.getAllConnections().map(c -> new Pair<>(c, node.getNodeId())))
                .map(pair -> new ConnectionListItem(pair.first(), pair.second()))
                .collect(Collectors.toList()));

        dataService = serviceNode.get().getDataService();
        dataService.ifPresent(dataService -> {
            //log.error(" dataService.addListener(dataListener);");
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
        });

        confidentialMessageService = serviceNode.get().getConfidentialMessageService();
        confidentialMessageService.ifPresent(service -> {
            service.addMessageListener(new Node.Listener() {
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
        });
    }


    public void activate() {
        allNodes.forEach(node -> {
            Node.Listener nodeListener = new Node.Listener() {
                @Override
                public void onMessage(Message message, Connection connection, String nodeId) {
                    UIThread.run(() -> maybeUpdateMyAddress(node));
                }

                @Override
                public void onConnection(Connection connection) {
                    UIThread.run(() -> {
                        connectionListItems.add(new ConnectionListItem(connection, node.getNodeId()));
                        maybeUpdateMyAddress(node);
                    });
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    UIThread.run(() -> {
                        connectionListItems.remove(new ConnectionListItem(connection, node.getNodeId()));
                        maybeUpdateMyAddress(node);
                    });

                }
            };
            node.addListener(nodeListener);
            nodeListenersByNodeId.put(node.getNodeId(), nodeListener);
        });
    }

    private void maybeUpdateMyAddress(Node node) {
        if (node.getNodeId().equals(defaultNode.getNodeId())) {
            defaultNode.findMyAddress().ifPresent(e -> myDefaultNodeAddress.set(e.getFullAddress()));
        }
    }

    public void deactivate() {
        allNodes.forEach(node -> node.removeListener(nodeListenersByNodeId.get(node.getNodeId())));
    }

    public void applyNetworkId(Optional<NetworkId> networkId) {
        this.selectedNetworkId = networkId;
        nodeIdString.set(networkId.map(NetworkId::nodeId)
                .orElse(Res.common.get("na")));
        messageReceiver.set(networkId.map(n -> n.addressByNetworkType().get(transportType).getFullAddress())
                .orElse(Res.common.get("na")));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletionStage<String> addData(String dataText, String id) {
        // We bootstrap a node with the give id to be ready to receive messages on that node.
        return networkService.maybeInitializeServerAsync(id)
                .thenCompose(result -> {
                    checkArgument(nodesById.findMyAddress(id).isPresent(),
                            "My Address must be present after initializeServer");
                    Map<Transport.Type, Address> addressByNetworkType = Map.of(transportType, nodesById.findMyAddress(id).get());
                    KeyPair keyPair = keyPairService.getOrCreateKeyPair(id);
                    PubKey pubKey = new PubKey(keyPair.getPublic(), id);
                    NetworkId networkId = new NetworkId(addressByNetworkType, pubKey, id);
                    AuthenticatedTextPayload payload = new AuthenticatedTextPayload(dataText, networkId);
                    return networkService.addNetworkPayload(payload, keyPair)
                            .thenApply(list -> {
                                return list.toString();
                            });
                });
    }


    CompletableFuture<String> sendMessage(String message) {
        checkArgument(selectedNetworkId.isPresent(), "Network ID must be set before calling sendMessage");
        NetworkId receiverNetworkId = selectedNetworkId.get();
        KeyPair senderKeyPair = keyPairService.getOrCreateKeyPair(KeyPairService.DEFAULT);
        CompletableFuture<String> future = new CompletableFuture<>();
        String senderNodeId = selectedNetworkId.get().nodeId();
        networkService.confidentialSendAsync(new TextMessage(message), receiverNetworkId, senderKeyPair, senderNodeId)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        if (resultMap.containsKey(transportType)) {
                            ConfidentialMessageService.Result result = resultMap.get(transportType);
                            result.getMailboxFuture()
                                    .ifPresentOrElse(broadcastFuture -> broadcastFuture
                                                    .whenComplete((broadcastResult, error) ->
                                                            future.complete(result.getState() + "; " + broadcastResult.toString())),
                                            () -> {
                                                String value = result.getState().toString();
                                                if (result.getState() == ConfidentialMessageService.State.FAILED) {
                                                    value += " with Error: " + result.getErrorMsg();
                                                }
                                                future.complete(value);
                                            });
                        }
                    }
                });
        return future;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}
