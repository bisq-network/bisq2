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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TransportTypeController implements Controller {
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final TransportTypeModel model;
    @Getter
    private final TransportTypeView view;

    private final Node.Listener nodeListener;
    private final NodesById.Listener nodesByIdListener;

    public TransportTypeController(ServiceProvider serviceProvider, TransportType transportType) {
        keyPairService = serviceProvider.getSecurityService().getKeyPairService();
        identityService = serviceProvider.getIdentityService();

        ServiceNode serviceNode = serviceProvider.getNetworkService().findServiceNode(transportType).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();

        model = new TransportTypeModel(transportType, serviceNode, defaultNode);
        view = new TransportTypeView(model, this);

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
            public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
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
        model.getMyDefaultNodeAddress().set(model.getDefaultNode().findMyAddress()
                .map(Address::getFullAddress)
                .orElseGet(() -> Res.get("data.na")));

        model.getServiceNode().getNodesById().getAllNodes().forEach(this::addNode);

        model.getServiceNode().getNodesById().addListener(nodesByIdListener);
        model.getServiceNode().getNodesById().addNodeListener(nodeListener);
    }

    @Override
    public void onDeactivate() {
        model.getServiceNode().getNodesById().removeListener(nodesByIdListener);
        model.getServiceNode().getNodesById().removeNodeListener(nodeListener);
        model.getNodeListItems().forEach(NodeListItem::deactivate);
        model.getConnectionListItems().forEach(ConnectionListItem::deactivate);
        model.getNodeListItems().clear();
        model.getConnectionListItems().clear();
    }

    private void addNode(Node node) {
        NodeListItem nodeListItem = new NodeListItem(node, keyPairService, identityService);
        if (!model.getNodeListItems().contains(nodeListItem)) {
            model.getNodeListItems().add(nodeListItem);
        }

        node.getAllConnections().forEach(connection -> addConnection(connection, node));
    }

    private void removeNode(Node node) {
        model.getNodeListItems().remove(new NodeListItem(node, keyPairService, identityService));

        node.getAllConnections().forEach(this::removeConnection);
    }

    private void addConnection(Connection connection, Node node) {
        ConnectionListItem item = new ConnectionListItem(connection, node, identityService);
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
