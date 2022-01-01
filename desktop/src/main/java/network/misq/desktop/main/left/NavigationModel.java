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

package network.misq.desktop.main.left;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.Model;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;

import java.util.Set;

public class NavigationModel implements Model {
    private final NetworkService networkService;

    final StringProperty clearNetNumConnections = new SimpleStringProperty("0");
    final StringProperty clearNetNumTargetConnections = new SimpleStringProperty("0");
    final BooleanProperty clearNetIsVisible = new SimpleBooleanProperty(false);

    final StringProperty torNumConnections = new SimpleStringProperty("0");
    final StringProperty torNumTargetConnections = new SimpleStringProperty("0");
    final BooleanProperty torIsVisible = new SimpleBooleanProperty(false);

    final StringProperty i2pNumConnections = new SimpleStringProperty("0");
    final StringProperty i2pNumTargetConnections = new SimpleStringProperty("0");
    final BooleanProperty i2pIsVisible = new SimpleBooleanProperty(false);


    public NavigationModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();

        Set<Transport.Type> supportedTransportTypes = networkService.getSupportedTransportTypes();
        clearNetIsVisible.set(supportedTransportTypes.contains(Transport.Type.CLEAR));
        torIsVisible.set(supportedTransportTypes.contains(Transport.Type.TOR));
        i2pIsVisible.set(supportedTransportTypes.contains(Transport.Type.I2P));

        networkService.getSupportedTransportTypes().forEach(type ->
                networkService.getServiceNodesByTransport().findServiceNode(type).ifPresent(serviceNode -> {
                    serviceNode.getPeerGroupService().ifPresent(peerGroupService -> {
                        PeerGroup peerGroup = peerGroupService.getPeerGroup();
                        switch (type) {
                            case TOR -> torNumTargetConnections.set(String.valueOf(peerGroup.getTargetNumConnectedPeers()));
                            case I2P -> i2pNumTargetConnections.set(String.valueOf(peerGroup.getTargetNumConnectedPeers()));
                            case CLEAR -> clearNetNumTargetConnections.set(String.valueOf(peerGroup.getTargetNumConnectedPeers()));
                        }

                        Node defaultNode = serviceNode.getDefaultNode();
                        defaultNode.addListener(new Node.Listener() {
                            @Override
                            public void onMessage(Message message, Connection connection, String nodeId) {
                            }

                            @Override
                            public void onConnection(Connection connection) {
                                onNumConnectionsChanged(type, peerGroup);
                            }

                            @Override
                            public void onDisconnect(Connection connection, CloseReason closeReason) {
                                onNumConnectionsChanged(type, peerGroup);
                            }
                        });
                    });
                })
        );
    }

    private void onNumConnectionsChanged(Transport.Type type, PeerGroup peerGroup) {
        UIThread.run(() -> {
            switch (type) {
                case TOR -> torNumConnections.set(String.valueOf(peerGroup.getNumConnections()));
                case I2P -> i2pNumConnections.set(String.valueOf(peerGroup.getNumConnections()));
                case CLEAR -> clearNetNumConnections.set(String.valueOf(peerGroup.getNumConnections()));
            }
        });
    }
}
