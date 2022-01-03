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
import network.misq.application.DefaultServiceProvider;
import network.misq.common.data.Pair;
import network.misq.desktop.common.view.Model;
import network.misq.i18n.Res;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.security.KeyPairService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

// Handled jfx only concerns, others which can be re-used by other frontends are in OfferbookEntity
public class TransportTypeModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;
    private final ObservableList<ConnectionListItem> list = FXCollections.observableArrayList();
    private final FilteredList<ConnectionListItem> filtered = new FilteredList<>(list);
    @Getter
    private final SortedList<ConnectionListItem> sorted = new SortedList<>(filtered);

    @Getter
    private final String title;
    @Getter
    private final StringProperty dateHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty directionHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty addressHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty nodeIdHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty sentHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty receivedHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty rrtHeader = new SimpleStringProperty();
    @Getter
    private final StringProperty pubKey = new SimpleStringProperty();
    private final Collection<Node> allNodes;
    private Map<String, Node.Listener> nodeListenersByNodeId = new HashMap<>();


    public TransportTypeModel(DefaultServiceProvider serviceProvider, Transport.Type transportType) {
        networkService = serviceProvider.getNetworkService();
        keyPairService = serviceProvider.getKeyPairService();

        title = Res.network.get(switch (transportType) {
            case TOR -> "tor";
            case I2P -> "i2p";
            case CLEAR -> "clearNet";
        });

        Optional<ServiceNode> serviceNode = networkService.findServiceNode(transportType);
        checkArgument(serviceNode.isPresent(),
                "ServiceNode must be present");

        allNodes = serviceNode.get().getNodesById().getAllNodes();
        list.setAll(allNodes.stream()
                .flatMap(node -> node.getAllConnections().map(c -> new Pair<>(c, node.getNodeId())))
                .map(pair -> new ConnectionListItem(pair.first(), pair.second()))
                .collect(Collectors.toList()));

        dateHeader.set(Res.network.get("table.header.established"));
        directionHeader.set(Res.network.get("table.header.connectionDirection"));
        addressHeader.set(Res.network.get("table.header.address"));
        nodeIdHeader.set(Res.network.get("table.header.nodeId"));
        sentHeader.set(Res.network.get("table.header.sentHeader"));
        receivedHeader.set(Res.network.get("table.header.receivedHeader"));
        rrtHeader.set(Res.network.get("rrt"));
    }


    public void activate() {
        allNodes.forEach(node -> {
            Node.Listener nodeListener = new Node.Listener() {
                @Override
                public void onMessage(Message message, Connection connection, String nodeId) {
                }

                @Override
                public void onConnection(Connection connection) {
                    list.add(new ConnectionListItem(connection, node.getNodeId()));
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    list.remove(new ConnectionListItem(connection, node.getNodeId()));
                }
            };
            node.addListener(nodeListener);
            nodeListenersByNodeId.put(node.getNodeId(), nodeListener);
        });
    }

    public void deactivate() {
        allNodes.forEach(node -> node.removeListener(nodeListenersByNodeId.get(node.getNodeId())));
        allNodes.clear();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}
