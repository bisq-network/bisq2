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

package bisq.desktop.main.content.settings.network.transport;

import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.components.table.TableList;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import bisq.security.keys.KeyBundleService;
import bisq.user.profile.UserProfileService;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

public class ConnectionsAndNodes {
    private final Controller controller;

    public ConnectionsAndNodes(ServiceProvider serviceProvider, TransportType transportType) {
        controller = new Controller(serviceProvider, transportType);
    }

    public Pane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        private final KeyBundleService keyBundleService;
        private final IdentityService identityService;
        private final Model model;
        @Getter
        private final View view;

        private final Optional<PeerGroupService> peerGroupService;
        private final UserProfileService userProfileService;
        private final Node.Listener nodeListener;
        private final NodesById.Listener nodesByIdListener;

        public Controller(ServiceProvider serviceProvider, TransportType transportType) {
            keyBundleService = serviceProvider.getSecurityService().getKeyBundleService();
            identityService = serviceProvider.getIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();

            ServiceNode serviceNode = serviceProvider.getNetworkService().findServiceNode(transportType).orElseThrow();
            peerGroupService = serviceNode.getPeerGroupManager().map(PeerGroupManager::getPeerGroupService);

            model = new Model(serviceNode);
            view = new View(model, this);

            nodesByIdListener = new NodesById.Listener() {
                @Override
                public void onNodeAdded(Node node) {
                    UIThread.run(() -> addNode(node));
                }

                @Override
                public void onNodeRemoved(Node node) {
                    UIThread.run(() -> removeNode(node));
                }
            };

            nodeListener = new Node.Listener() {
                @Override
                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                      Connection connection,
                                      NetworkId networkId) {
                }

                @Override
                public void onConnection(Connection connection) {
                    UIThread.run(() -> findNodeListItem(connection).ifPresent(nodeListItem -> addConnection(connection, nodeListItem.getNode())));
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    UIThread.run(() -> removeConnection(connection));
                }
            };
        }

        @Override
        public void onActivate() {
            // addNode triggers addConnection
            model.getServiceNode().getNodesById().getAllNodes().forEach(this::addNode);

            model.getServiceNode().getNodesById().addListener(nodesByIdListener);
            model.getServiceNode().getNodesById().addNodeListener(nodeListener);

            model.getNodeListItems().onActivate();
            model.getConnectionListItems().onActivate();
        }

        @Override
        public void onDeactivate() {
            model.getServiceNode().getNodesById().removeListener(nodesByIdListener);
            model.getServiceNode().getNodesById().removeNodeListener(nodeListener);

            model.getNodeListItems().onDeactivate();
            model.getConnectionListItems().onDeactivate();
            model.getNodeListItems().clear();
            model.getConnectionListItems().clear();
        }

        void applyConnectionListSearchPredicate(String searchText) {
            String string = searchText.toLowerCase();
            model.getConnectionListItems().setPredicate(item ->
                    StringUtils.isEmpty(string) ||
                            item.getPeer().toLowerCase().contains(string) ||
                            item.getAddress().toLowerCase().contains(string) ||
                            item.getNodeTag().contains(string) ||
                            item.getDirection().contains(string));
        }

        void applyNodeListSearchPredicate(String searchText) {
            String string = searchText.toLowerCase();
            model.getNodeListItems().setPredicate(item ->
                    StringUtils.isEmpty(string) ||
                            item.getType().toLowerCase().contains(string) ||
                            item.getNodeTag().contains(string) ||
                            item.getAddress().toLowerCase().contains(string) ||
                            item.getKeyId().contains(string));
        }

        private void addNode(Node node) {
            NodeListItem nodeListItem = new NodeListItem(node, keyBundleService, identityService);
            if (!model.getNodeListItems().contains(nodeListItem)) {
                model.getNodeListItems().add(nodeListItem);
            }

            node.getAllConnections().forEach(connection -> addConnection(connection, node));
        }

        private void removeNode(Node node) {
            model.getNodeListItems().remove(new NodeListItem(node, keyBundleService, identityService));

            node.getAllConnections().forEach(this::removeConnection);
        }

        private void addConnection(Connection connection, Node node) {
            ConnectionListItem item = new ConnectionListItem(connection, node, identityService, userProfileService, peerGroupService);
            if (!model.getConnectionListItems().contains(item)) {
                model.getConnectionListItems().add(item);
            }
        }

        private void removeConnection(Connection connection) {
            Optional<ConnectionListItem> toRemove = model.getConnectionListItems().stream()
                    .filter(item -> item.getConnection().getId().equals(connection.getId()))
                    .findAny();
            toRemove.ifPresent(c -> model.getConnectionListItems().removeAll(c));
        }

        private Optional<NodeListItem> findNodeListItem(Connection connection) {
            return model.getNodeListItems().stream()
                    .filter(item -> item.getNode().getAllConnections()
                            .anyMatch(c -> c.getId().equals(connection.getId())))
                    .findAny();
        }
    }

    @Slf4j
    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        private final ServiceNode serviceNode;
        private final TableList<ConnectionListItem> connectionListItems = new TableList<>();
        private final TableList<NodeListItem> nodeListItems = new TableList<>();

        public Model(ServiceNode serviceNode) {
            this.serviceNode = serviceNode;
        }
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final RichTableView<ConnectionListItem> connectionsTableView;
        private final RichTableView<NodeListItem> nodesTableView;

        public View(Model model, Controller controller) {
            super(new VBox(20), model, controller);

            connectionsTableView = new RichTableView<>(model.getConnectionListItems().getSortedList(),
                    Res.get("settings.network.connections.title"),
                    controller::applyConnectionListSearchPredicate);
            // Fill available width
            connectionsTableView.setPrefWidth(2000);
            configConnectionsTableView();

            nodesTableView = new RichTableView<>(model.getNodeListItems().getSortedList(),
                    Res.get("settings.network.nodes.title"),
                    controller::applyNodeListSearchPredicate);
            nodesTableView.setPrefHeight(200);
            configNodesTableView();

            root.getChildren().addAll(connectionsTableView, nodesTableView);
        }

        @Override
        protected void onViewAttached() {
            connectionsTableView.initialize();
            nodesTableView.initialize();
        }

        @Override
        protected void onViewDetached() {
            connectionsTableView.dispose();
            nodesTableView.dispose();
        }

        private void configConnectionsTableView() {
            connectionsTableView.getColumns().add(DateColumnUtil.getDateColumn(connectionsTableView.getSortOrder()));

            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.peer"))
                    .minWidth(130)
                    .valueSupplier(ConnectionListItem::getPeer)
                    .tooltipSupplier(ConnectionListItem::getPeer)
                    .comparator(ConnectionListItem::comparePeer)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.address"))
                    .minWidth(130)
                    .valueSupplier(ConnectionListItem::getAddress)
                    .tooltipSupplier(ConnectionListItem::getAddress)
                    .comparator(ConnectionListItem::compareAddress)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.header.nodeTag"))
                    .minWidth(120)
                    .valueSupplier(ConnectionListItem::getNodeTag)
                    .tooltipSupplier(ConnectionListItem::getNodeTagTooltip)
                    .comparator(ConnectionListItem::compareNodeTag)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.connectionDirection"))
                    .minWidth(100)
                    .valueSupplier(ConnectionListItem::getDirection)
                    .comparator(ConnectionListItem::compareDirection)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.sentHeader"))
                    .minWidth(160)
                    .valuePropertySupplier(ConnectionListItem::getSent)
                    .tooltipPropertySupplier(ConnectionListItem::getSent)
                    .comparator(ConnectionListItem::compareSent)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.receivedHeader"))
                    .minWidth(160)
                    .valuePropertySupplier(ConnectionListItem::getReceived)
                    .tooltipPropertySupplier(ConnectionListItem::getReceived)
                    .comparator(ConnectionListItem::compareReceived)
                    .build());
            connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                    .title(Res.get("settings.network.connections.header.rtt"))
                    .minWidth(100)
                    .valuePropertySupplier(ConnectionListItem::getRtt)
                    .comparator(ConnectionListItem::compareRtt)
                    .build());
        }

        private void configNodesTableView() {
            nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                    .title(Res.get("settings.network.nodes.header.type"))
                    .minWidth(70)
                    .left()
                    .valueSupplier(NodeListItem::getType)
                    .comparator(NodeListItem::compareType)
                    .build());
            nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                    .title(Res.get("settings.network.header.nodeTag"))
                    .minWidth(100)
                    .valueSupplier(NodeListItem::getNodeTag)
                    .tooltipSupplier(NodeListItem::getNodeTagTooltip)
                    .comparator(NodeListItem::compareNodeTag)
                    .build());
            nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                    .title(Res.get("settings.network.nodes.header.address"))
                    .minWidth(130)
                    .valueSupplier(NodeListItem::getAddress)
                    .tooltipSupplier(NodeListItem::getAddress)
                    .comparator(NodeListItem::compareAddress)
                    .build());
            BisqTableColumn<NodeListItem> numConnections = new BisqTableColumn.Builder<NodeListItem>()
                    .title(Res.get("settings.network.nodes.header.numConnections"))
                    .minWidth(170)
                    .valuePropertySupplier(NodeListItem::getNumConnections)
                    .comparator(NodeListItem::compareNumConnections)
                    .build();
            nodesTableView.getColumns().add(numConnections);
            nodesTableView.getSortOrder().add(numConnections);
            nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                    .title(Res.get("settings.network.nodes.header.keyId"))
                    .minWidth(100)
                    .valueSupplier(NodeListItem::getKeyId)
                    .tooltipSupplier(NodeListItem::getKeyId)
                    .comparator(NodeListItem::compareKeyId)
                    .build());
        }
    }
}
