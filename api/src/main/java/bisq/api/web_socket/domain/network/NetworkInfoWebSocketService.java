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

package bisq.api.web_socket.domain.network;

import bisq.api.dto.network.ConnectionDto;
import bisq.api.dto.network.NetworkInfoDto;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.Topic;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Pushes the local node's network state (peer count, data-sync flag, tor status, own address, key id and
 * the active connections) to subscribers. The values are read from {@link Node} / {@link ServiceNode} /
 * {@link PeerGroupService} / {@link InventoryService}, giving clients that reach the node over the API the
 * same network view an embedded-core node has.
 */
@Slf4j
public class NetworkInfoWebSocketService extends BaseWebSocketService {
    // Priority order used to pick the primary node whose address, key id and connections we expose.
    // We expose a single default node; for a node app this is typically the only supported transport,
    // so the first match wins.
    private static final List<TransportType> TRANSPORT_PRIORITY = List.of(TransportType.TOR, TransportType.I2P, TransportType.CLEAR);

    private final NetworkService networkService;
    private final Node.Listener nodeListener;
    private final Set<Pin> pins = new CopyOnWriteArraySet<>();
    @Nullable
    private volatile ServiceNode observedServiceNode;

    public NetworkInfoWebSocketService(SubscriberRepository subscriberRepository,
                                       NetworkService networkService) {
        super(subscriberRepository, Topic.NETWORK_INFO);
        this.networkService = networkService;

        nodeListener = new Node.Listener() {
            @Override
            public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
            }

            @Override
            public void onConnection(Connection connection) {
                onChange();
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
                onChange();
            }

            @Override
            public void onStateChange(Node.State state) {
                onChange();
            }
        };
    }

    @Override
    public void validate(SubscriptionRequest request) {
        if (StringUtils.toOptional(request.getParameter()).isPresent()) {
            throw new IllegalArgumentException("NETWORK_INFO does not support a subscription parameter");
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        findPrimaryServiceNode().ifPresent(serviceNode -> {
            observedServiceNode = serviceNode;
            serviceNode.getDefaultNode().addListener(nodeListener);

            serviceNode.getInventoryService()
                    .map(InventoryService::getInitialInventoryRequestsCompleted)
                    .ifPresent(observable -> pins.add(observable.addObserver(completed -> onChange())));
        });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        ServiceNode serviceNode = observedServiceNode;
        if (serviceNode != null) {
            serviceNode.getDefaultNode().removeListener(nodeListener);
            observedServiceNode = null;
        }
        pins.forEach(Pin::unbind);
        pins.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return toJson(buildNetworkInfoDto());
    }

    private void onChange() {
        Set<Subscriber> subscribers = subscriberRepository.findSubscribers(topic).values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        if (subscribers.isEmpty()) {
            return;
        }
        try {
            send(subscribers, getJsonPayload(), topic, ModificationType.REPLACE);
        } catch (Exception e) {
            log.error("Failed to send network info update", e);
        }
    }

    private NetworkInfoDto buildNetworkInfoDto() {
        Optional<ServiceNode> primaryServiceNode = Optional.ofNullable(observedServiceNode);
        Optional<Node> defaultNode = primaryServiceNode.map(ServiceNode::getDefaultNode);
        PeerGroupService peerGroupService = primaryServiceNode
                .flatMap(ServiceNode::getPeerGroupManager)
                .map(PeerGroupManager::getPeerGroupService)
                .orElse(null);

        // We traverse the active connections only once, so that numConnections and the connection list
        // are consistent with each other. Connections can come and go while we build the payload.
        List<ConnectionDto> connections = defaultNode
                .map(node -> toConnectionDtos(node, peerGroupService))
                .orElseGet(List::of);

        boolean allDataReceived = primaryServiceNode
                .flatMap(ServiceNode::getInventoryService)
                .map(inventoryService -> inventoryService.getInitialInventoryRequestsCompleted().get())
                .orElse(false);

        boolean torRunning = networkService.findDefaultNode(TransportType.TOR)
                .map(node -> node.getState().get() == Node.State.RUNNING)
                .orElse(false);

        String myAddress = defaultNode
                .flatMap(Node::findMyAddress)
                .map(Address::getFullAddress)
                .orElse(null);

        String keyId = defaultNode
                .map(node -> node.getNetworkId().getKeyId())
                .orElse(null);

        return new NetworkInfoDto(allDataReceived, torRunning, myAddress, keyId, connections);
    }

    private List<ConnectionDto> toConnectionDtos(Node node, @Nullable PeerGroupService peerGroupService) {
        List<ConnectionDto> connections = new ArrayList<>();
        node.getAllActiveConnections().forEach(connection ->
                connections.add(new ConnectionDto(
                        connection.getId(),
                        connection.getPeerAddress().getFullAddress(),
                        connection.isOutboundConnection(),
                        peerGroupService != null && peerGroupService.isSeed(connection),
                        connection.getCreated())));
        return connections;
    }

    private Optional<ServiceNode> findPrimaryServiceNode() {
        return TRANSPORT_PRIORITY.stream()
                .filter(networkService.getSupportedTransportTypes()::contains)
                .map(networkService::findServiceNode)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
