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

package bisq.network.p2p.services.data;

import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Responsible for broadcast and inventory service. One instance per transport type.
 */
@Slf4j
public class DataNetworkService implements PeerGroupManager.Listener, Node.Listener {

    private final PeerGroupService peerGroupService;
    private final PeerGroupManager peerGroupManager;

    public interface Listener {
        void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId);

        void onStateChanged(PeerGroupManager.State state, DataNetworkService dataNetworkService);

        void onSufficientlyConnected(int numConnections, DataNetworkService dataNetworkService);
    }

    private final Node node;
    private final Broadcaster broadcaster;
    private final Set<DataNetworkService.Listener> listeners = new CopyOnWriteArraySet<>();

    public DataNetworkService(Node node,
                              PeerGroupManager peerGroupManager) {
        this.node = node;
        peerGroupService = peerGroupManager.getPeerGroupService();
        this.peerGroupManager = peerGroupManager;
        peerGroupManager.addListener(this);
        broadcaster = new Broadcaster(node, peerGroupService);
        node.addListener(this);
    }

    public CompletableFuture<Boolean> shutdown() {
        node.removeListener(this);
        peerGroupManager.removeListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PeerGroupService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStateChanged(PeerGroupManager.State state) {
        listeners.forEach(listener -> listener.onStateChanged(state, this));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        listeners.forEach(listener -> listener.onMessage(envelopePayloadMessage, connection, networkId));
    }

    @Override
    public void onConnection(Connection connection) {
        if (peerGroupService.getNumConnections() > peerGroupService.getTargetNumConnectedPeers() / 2) {
            listeners.forEach(listener -> listener.onSufficientlyConnected(peerGroupService.getNumConnections(), this));
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
        return broadcaster.broadcast(broadcastMessage);
    }

    CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
        return broadcaster.reBroadcast(broadcastMessage);
    }

    void addListener(DataNetworkService.Listener listener) {
        listeners.add(listener);
    }

    void removeListener(DataNetworkService.Listener listener) {
        listeners.remove(listener);
    }
}
