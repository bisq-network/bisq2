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

package bisq.desktop.primary.main.content.settings.networkinfo.transport;

import bisq.application.DefaultApplicationService;
import bisq.common.data.Pair;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.node.transport.Transport;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class TransportTypeModel implements Model {
    private final NetworkService networkService;
    private final Transport.Type transportType;
    private final ObservableList<ConnectionListItem> connectionListItems = FXCollections.observableArrayList();
    private final FilteredList<ConnectionListItem> filteredConnectionListItems = new FilteredList<>(connectionListItems);
    private final SortedList<ConnectionListItem> sortedConnectionListItems = new SortedList<>(filteredConnectionListItems);
    private final ObservableList<NodeListItem> nodeListItems = FXCollections.observableArrayList();
    private final FilteredList<NodeListItem> filteredNodeListItems = new FilteredList<>(nodeListItems);
    private final SortedList<NodeListItem> sortedNodeListItems = new SortedList<>(filteredNodeListItems);
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.common.get("na"));
    private final StringProperty nodeIdString = new SimpleStringProperty();
    private final StringProperty messageReceiver = new SimpleStringProperty();
    private final StringProperty receivedMessages = new SimpleStringProperty("");
    private final IdentityService identityService;
    private final Node.Listener defaultNodeListener;
    private final NodesById.Listener nodesByIdListener;
    private final ServiceNode serviceNode;
    private final Node defaultNode;
    private final Optional<NetworkId> selectedNetworkId = Optional.empty();
    private final Map<String, Node.Listener> nodeListenersByNodeId = new HashMap<>();
    private Collection<Node> allNodes = new ArrayList<>();


    public TransportTypeModel(DefaultApplicationService applicationService, Transport.Type transportType) {
        networkService = applicationService.getNetworkService();
        identityService = applicationService.getIdentityService();
        this.transportType = transportType;

        Optional<ServiceNode> optionalServiceNode = networkService.findServiceNode(transportType);
        checkArgument(optionalServiceNode.isPresent(), "ServiceNode must be present");
        serviceNode = optionalServiceNode.get();
        defaultNode = serviceNode.getDefaultNode();
        defaultNode.findMyAddress().ifPresent(e -> myDefaultNodeAddress.set(e.getFullAddress()));

        defaultNodeListener = new Node.Listener() {
            @Override
            public void onMessage(Message message, Connection connection, String nodeId) {
            }

            @Override
            public void onConnection(Connection connection) {
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
            }

            @Override
            public void onStateChange(Node.State state) {
                if (state == Node.State.SERVER_INITIALIZED) {
                    UIThread.runLater(() -> {
                        updateLists();
                        networkService.findDefaultNode(transportType)
                                .filter(node -> node.getState().get() != Node.State.SERVER_INITIALIZED)
                                .ifPresent(node -> node.removeListener(defaultNodeListener));
                    });
                }
            }
        };
        networkService.findDefaultNode(transportType)
                .filter(node -> node.getState().get() != Node.State.SERVER_INITIALIZED)
                .ifPresent(node -> node.addListener(defaultNodeListener));

        nodesByIdListener = node -> UIThread.run(() -> {
            addNodeListener(node);
            updateLists();
        });
    }

    private void updateLists() {
        allNodes = new ArrayList<>(serviceNode.getNodesById().getAllNodes());
        connectionListItems.setAll(allNodes.stream()
                .flatMap(node -> node.getAllConnections().map(c -> new Pair<>(c, node.getNodeId())))
                .map(pair -> new ConnectionListItem(pair.first(), pair.second()))
                .collect(Collectors.toList()));
        nodeListItems.setAll(allNodes
                .stream()
                .filter(node -> node.getState().get() == Node.State.SERVER_INITIALIZED)
                .map(node -> new NodeListItem(node, identityService))
                .collect(Collectors.toList()));
    }

    public void onViewAttached() {
        networkService.findServiceNode(transportType)
                .map(ServiceNode::getNodesById)
                .ifPresent(nodesById -> nodesById.addListener(nodesByIdListener));

        updateLists();
        allNodes.forEach(this::addNodeListener);
    }

    private void addNodeListener(Node node) {
        Node.Listener listener = new Node.Listener() {
            @Override
            public void onMessage(Message message, Connection connection, String nodeId) {
                UIThread.run(() -> maybeUpdateMyAddress(node));
            }

            @Override
            public void onConnection(Connection connection) {
                UIThread.run(() -> {
                    ConnectionListItem connectionListItem = new ConnectionListItem(connection, node.getNodeId());
                    if (!connectionListItems.contains(connectionListItem)) {
                        connectionListItems.add(connectionListItem);
                    }
                    maybeUpdateMyAddress(node);
                });
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
                UIThread.run(() -> {
                    ConnectionListItem connectionListItem = new ConnectionListItem(connection, node.getNodeId());
                    if (!connectionListItems.contains(connectionListItem)) {
                        connectionListItems.remove(connectionListItem);
                    }
                    maybeUpdateMyAddress(node);
                });
            }

            @Override
            public void onStateChange(Node.State state) {
                UIThread.run(() -> {
                    if (state == Node.State.SERVER_INITIALIZED) {
                        NodeListItem nodeListItem = new NodeListItem(node, identityService);
                        if (!nodeListItems.contains(nodeListItem)) {
                            nodeListItems.add(nodeListItem);
                        }
                    }
                });
            }
        };
        node.addListener(listener);
        nodeListenersByNodeId.put(node.getNodeId(), listener);
    }

    public void onViewDetached() {
        allNodes.forEach(node -> node.removeListener(nodeListenersByNodeId.get(node.getNodeId())));
        networkService.findServiceNode(transportType)
                .map(ServiceNode::getNodesById)
                .ifPresent(nodesById -> nodesById.removeListener(nodesByIdListener));
    }

    private void maybeUpdateMyAddress(Node node) {
        if (node.getNodeId().equals(defaultNode.getNodeId())) {
            defaultNode.findMyAddress().ifPresent(e -> myDefaultNodeAddress.set(e.getFullAddress()));
        }
    }

}
