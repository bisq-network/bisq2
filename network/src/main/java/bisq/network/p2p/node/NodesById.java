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

package bisq.network.p2p.node;


import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.peergroup.BanList;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Maintains nodes per nodeId.
 * Provides delegate methods to node with given nodeId
 */
public class NodesById implements Node.Listener {
    public interface Listener {
        void onNodeAdded(Node node);
    }

    private final Map<String, Node> map = new ConcurrentHashMap<>();
    private final BanList banList;
    private final Node.Config nodeConfig;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Set<Node.Listener> nodeListeners = new CopyOnWriteArraySet<>();

    public NodesById(BanList banList, Node.Config nodeConfig) {
        this.banList = banList;
        this.nodeConfig = nodeConfig;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void maybeInitializeServer(String nodeId) {
        maybeInitializeServer(nodeId, NetworkUtils.findFreeSystemPort());
    }

    public void maybeInitializeServer(String nodeId, int serverPort) {
        getOrCreateNode(nodeId).maybeInitializeServer(serverPort);
    }

    public Connection getConnection(String nodeId, Address address) {
        return getOrCreateNode(nodeId).getConnection(address);
    }

    public Connection send(String senderNodeId, NetworkMessage networkMessage, Address address) {
        return getOrCreateNode(senderNodeId).send(networkMessage, address);
    }

    public Connection send(String senderNodeId, NetworkMessage networkMessage, Connection connection) {
        return getOrCreateNode(senderNodeId).send(networkMessage, connection);
    }

    public void addNodeListener(Node.Listener listener) {
        nodeListeners.add(listener);
    }

    public void removeNodeListener(Node.Listener listener) {
        nodeListeners.remove(listener);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void addNodeListener(String nodeId, Node.Listener listener) {
        findNode(nodeId).ifPresent(node -> node.addListener(listener));
    }

    public void removeNodeListener(String nodeId, Node.Listener listener) {
        findNode(nodeId).ifPresent(node -> node.removeListener(listener));
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFutureUtils.allOf(map.values().stream().map(Node::shutdown))
                .orTimeout(2, TimeUnit.SECONDS)
                .thenApply(list -> {
                    map.clear();
                    return null;
                });
    }

    public Node getDefaultNode() {
        return getOrCreateNode(Node.DEFAULT);
    }

    public Optional<Address> findMyAddress(String nodeId) {
        return findNode(nodeId).flatMap(Node::findMyAddress);
    }

    public Optional<Node> findNode(String nodeId) {
        return Optional.ofNullable(map.get(nodeId));
    }

    public Collection<Node> getAllNodes() {
        return map.values();
    }

    public Map<String, Address> getAddressesByNodeId() {
        //noinspection OptionalGetWithoutIsPresent
        return map.entrySet().stream()
                .filter(e -> e.getValue().findMyAddress().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().findMyAddress().get()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
        nodeListeners.forEach(messageListener -> messageListener.onMessage(networkMessage, connection, nodeId));
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Node getOrCreateNode(String nodeId) {
        return findNode(nodeId)
                .orElseGet(() -> {
                    Node node = new Node(banList, nodeConfig, nodeId);
                    map.put(nodeId, node);
                    node.addListener(this);
                    listeners.forEach(listener -> listener.onNodeAdded(node));
                    return node;
                });
    }
}
