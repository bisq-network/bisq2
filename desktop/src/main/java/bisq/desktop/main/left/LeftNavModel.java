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

package bisq.desktop.main.left;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationTarget;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.PeerGroup;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class LeftNavModel implements Model {
    private final boolean isWalletEnabled;
    private final NetworkService networkService;
    private final Set<NavigationTarget> navigationTargets = new HashSet<>();
    private final List<LeftNavButton> leftNavButtons = new ArrayList<>();
    private final ObjectProperty<NavigationTarget> selectedNavigationTarget = new SimpleObjectProperty<>();
    private final ObjectProperty<LeftNavButton> selectedNavigationButton = new SimpleObjectProperty<>();

    private final StringProperty torNumConnections = new SimpleStringProperty("0");
    private final StringProperty torNumTargetConnections = new SimpleStringProperty("0");
    private final BooleanProperty torEnabled = new SimpleBooleanProperty(false);
    private final StringProperty i2pNumConnections = new SimpleStringProperty("0");
    private final StringProperty i2pNumTargetConnections = new SimpleStringProperty("0");
    private final BooleanProperty i2pEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty menuHorizontalExpanded = new SimpleBooleanProperty(true);
    private final BooleanProperty tradeAppsSubMenuExpanded = new SimpleBooleanProperty(false);
    private final BooleanProperty learnsSubMenuExpanded = new SimpleBooleanProperty(false);
    private final BooleanProperty authorizedRoleVisible = new SimpleBooleanProperty(false);


    public LeftNavModel(ServiceProvider serviceProvider) {
        isWalletEnabled = serviceProvider.getWalletService().isPresent();
        networkService = serviceProvider.getNetworkService();

        torEnabled.set(networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pEnabled.set(networkService.isTransportTypeSupported(Transport.Type.I2P));

        networkService.getSupportedTransportTypes().forEach(type ->
                networkService.getServiceNodesByTransport().findServiceNode(type).ifPresent(serviceNode -> {
                    serviceNode.getPeerGroupService().ifPresent(peerGroupService -> {
                        PeerGroup peerGroup = peerGroupService.getPeerGroup();
                        switch (type) {
                            case TOR:
                                torNumTargetConnections.set(String.valueOf(peerGroup.getTargetNumConnectedPeers()));
                                break;
                            case I2P:
                                i2pNumTargetConnections.set(String.valueOf(peerGroup.getTargetNumConnectedPeers()));
                                break;
                        }

                        Node defaultNode = serviceNode.getDefaultNode();
                        defaultNode.addListener(new Node.Listener() {
                            @Override
                            public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
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
                case TOR:
                    torNumConnections.set(String.valueOf(peerGroup.getNumConnections()));
                    break;
                case I2P:
                    i2pNumConnections.set(String.valueOf(peerGroup.getNumConnections()));
                    break;
            }
        });
    }


}
