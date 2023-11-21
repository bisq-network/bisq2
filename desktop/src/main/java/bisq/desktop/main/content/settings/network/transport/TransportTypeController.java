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

import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.security.KeyPairService;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

public class TransportTypeController implements Controller {
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final TransportTypeModel model;
    @Getter
    private final TransportTypeView view;

    private final NodesById.Listener nodesByIdListener;
    private Pin nodeListItemsPin, connectionListItemsPin;

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
                UIThread.run(() -> model.getNodes().add(node));
            }

            @Override
            public void onNodeRemoved(Node node) {
                UIThread.run(() -> model.getNodes().remove(node));
            }
        };
    }

    @Override
    public void onActivate() {
        model.getMyDefaultNodeAddress().set(model.getDefaultNode().findMyAddress()
                .map(Address::getFullAddress)
                .orElseGet(() -> Res.get("data.na")));

        nodeListItemsPin = FxBindings.<Node, NodeListItem>bind(model.getNodeListItems())
                .map(node -> new NodeListItem(node, keyPairService, identityService))
                .to(model.getNodes());

        connectionListItemsPin = model.getNodes().addObserver(new CollectionObserver<>() {
            @Override
            public void add(Node node) {
                UIThread.run(() -> {
                    model.getConnectionListItems().addAll(node.getAllConnections()
                            .map(connection -> new ConnectionListItem(connection, node.getNetworkId().getKeyId()))
                            .collect(Collectors.toSet()));
                });
            }

            @Override
            public void remove(Object element) {
                UIThread.run(() -> {
                    if (element instanceof Node) {
                        Node node = (Node) element;
                        Set<ConnectionListItem> toRemove = model.getConnectionListItems().stream().filter(item -> item.getKeyId().equals(node.getNetworkId().getKeyId()))
                                .collect(Collectors.toSet());
                        model.getConnectionListItems().removeAll(toRemove);
                    }
                });
            }

            @Override
            public void clear() {
                UIThread.run(() -> model.getConnections().clear());
            }
        });

        model.getServiceNode().getNodesById().addListener(nodesByIdListener);
        model.getServiceNode().getNodesById().getAllNodes().forEach(node -> model.getNodes().add(node));
    }

    @Override
    public void onDeactivate() {
        nodeListItemsPin.unbind();
        connectionListItemsPin.unbind();
        model.getServiceNode().getNodesById().addListener(nodesByIdListener);
        model.getNodeListItems().forEach(NodeListItem::deactivate);
        model.getSortedConnectionListItems().forEach(ConnectionListItem::deactivate);
    }
}
