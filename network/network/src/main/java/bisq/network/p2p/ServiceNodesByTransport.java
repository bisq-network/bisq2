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

package bisq.network.p2p;


import bisq.common.data.Pair;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.platform.MemoryReportService;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.SendConfidentialMessageResult;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.reporting.Report;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains a map of ServiceNodes by transportType. Delegates to relevant ServiceNode.
 */
// TODO (deferred): if we change the supported transports we need to clean up the persisted networkIds.
@Slf4j
public class ServiceNodesByTransport {
    private final Map<TransportType, ServiceNode> map = new ConcurrentHashMap<>();
    private final Set<TransportType> supportedTransportTypes;
    @Getter
    private final AuthorizationService authorizationService;

    public ServiceNodesByTransport(Map<TransportType, TransportConfig> configByTransportType,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<TransportType, PeerGroupManager.Config> peerGroupServiceConfigByTransport,
                                   Map<TransportType, Set<Address>> seedAddressesByTransport,
                                   InventoryService.Config inventoryServiceConfig,
                                   AuthorizationService.Config authorizationServiceConfig,
                                   Set<TransportType> supportedTransportTypes,
                                   Set<Feature> features,
                                   KeyBundleService keyBundleService,
                                   PersistenceService persistenceService,
                                   HashCashProofOfWorkService hashCashProofOfWorkService,
                                   EquihashProofOfWorkService equihashProofOfWorkService,
                                   Optional<DataService> dataService,
                                   Optional<MessageDeliveryStatusService> messageDeliveryStatusService,
                                   Optional<ResendMessageService> resendMessageService,
                                   MemoryReportService memoryReportService) {
        this.supportedTransportTypes = supportedTransportTypes;

        authorizationService = new AuthorizationService(authorizationServiceConfig,
                hashCashProofOfWorkService,
                equihashProofOfWorkService,
                features);

        supportedTransportTypes.forEach(transportType -> {
            TransportConfig transportConfig = configByTransportType.get(transportType);
            int maxNumConnectedPeers = peerGroupServiceConfigByTransport.get(transportType).getPeerGroupConfig().getMaxNumConnectedPeers();
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    features,
                    transportConfig,
                    transportConfig.getSocketTimeout(),
                    transportConfig.getSendMessageThrottleTime(),
                    transportConfig.getReceiveMessageThrottleTime(),
                    maxNumConnectedPeers);
            Set<Address> seedAddresses = seedAddressesByTransport.get(transportType);
            checkNotNull(seedAddresses, "Seed nodes must be setup for %s", transportType);
            PeerGroupManager.Config peerGroupServiceConfig = peerGroupServiceConfigByTransport.get(transportType);

            ServiceNode serviceNode = new ServiceNode(serviceNodeConfig,
                    nodeConfig,
                    peerGroupServiceConfig,
                    inventoryServiceConfig,
                    keyBundleService,
                    persistenceService,
                    dataService,
                    messageDeliveryStatusService,
                    resendMessageService,
                    authorizationService,
                    seedAddresses,
                    transportType,
                    memoryReportService);
            map.put(transportType, serviceNode);
        });
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Map<TransportType, CompletableFuture<Node>> getInitializedDefaultNodeByTransport(NetworkId defaultNetworkId) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        entry.getValue().getInitializedDefaultNodeAsync(defaultNetworkId)));
    }

    public CompletableFuture<List<Boolean>> shutdown() {
        Stream<CompletableFuture<Boolean>> futures = map.values().stream().map(ServiceNode::shutdown);
        return CompletableFutureUtils.allOf(futures).whenComplete((list, throwable) -> map.clear());
    }

    public CompletableFuture<List<Node>> allSuppliedInitializedNode(NetworkId networkId) {
        return CompletableFutureUtils.allOf(suppliedInitializedNodeByTransport(networkId).values());
    }

    public CompletableFuture<Node> anySuppliedInitializedNode(NetworkId networkId) {
        return CompletableFutureUtils.anyOf(suppliedInitializedNodeByTransport(networkId).values());
    }

    public Map<TransportType, CompletableFuture<Node>> suppliedInitializedNodeByTransport(NetworkId networkId) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> supplyInitializedNode(entry.getKey(), networkId)));
    }

    public CompletableFuture<Node> supplyInitializedNode(TransportType transportType, NetworkId networkId) {
        ServiceNode serviceNode = map.get(transportType);
        if (serviceNode.isNodeInitialized(networkId)) {
            return CompletableFuture.completedFuture(serviceNode.findNode(networkId).orElseThrow());
        } else {
            return serviceNode.initializeNodeAsync(networkId);
        }
    }

    public void addSeedNodes(Set<AddressByTransportTypeMap> seedNodeMaps) {
        supportedTransportTypes.forEach(transportType -> {
            Set<Address> seeds = seedNodeMaps.stream()
                    .map(map -> map.getAddress(transportType))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            map.get(transportType).addSeedNodeAddresses(seeds);
        });
    }

    public void addSeedNode(AddressByTransportTypeMap seedNodeMap) {
        supportedTransportTypes.forEach(transportType -> {
            if (seedNodeMap.containsKey(transportType)) {
                Address seedNodeAddress = seedNodeMap.get(transportType);
                map.get(transportType).addSeedNodeAddress(seedNodeAddress);
            }
        });
    }

    public void removeSeedNode(AddressByTransportTypeMap seedNode) {
        supportedTransportTypes.forEach(transportType -> {
            Address seedNodeAddress = seedNode.get(transportType);
            map.get(transportType).removeSeedNodeAddress(seedNodeAddress);
        });
    }

    public SendMessageResult confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                              NetworkId receiverNetworkId,
                                              KeyPair senderKeyPair,
                                              NetworkId senderNetworkId) {
        SendMessageResult sendMessageResult = new SendMessageResult();
        receiverNetworkId.getAddressByTransportTypeMap().forEach((transportType, address) -> {
            ServiceNode serviceNode;
            // We try to use the transport of the receivers address
            if (map.containsKey(transportType)) {
                serviceNode = map.get(transportType);
            } else if (!map.isEmpty()) {
                // In case we do not have the transport of the receivers address we use our first transport.
                // This would be the case when we use Tor only and the peer use I2P only. As we do not have a
                // serviceNode for I2P we would fall back to Tor. In ConfidentialMessageService we will not find the node, and send it as mailbox message.
                // That way any node the p2p network supporting both networks act as a relay.
                serviceNode = map.values().stream().findFirst().get();
            } else {
                throw new RuntimeException("confidentialSend called but we do not have any serviceNode available. This should never happen.");
            }
            SendConfidentialMessageResult result = serviceNode.confidentialSend(envelopePayloadMessage,
                    receiverNetworkId,
                    address,
                    receiverNetworkId.getPubKey(),
                    senderKeyPair,
                    senderNetworkId);
            sendMessageResult.put(transportType, result);
        });
        return sendMessageResult;
    }

    public void addConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.addConfidentialMessageListener(listener));
    }

    public void removeConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeConfidentialMessageListener(listener));
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().addListener(nodeListener));
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().removeListener(nodeListener));
    }

    public Optional<Socks5Proxy> getSocksProxy(TransportType transportType) {
        if (transportType == TransportType.TOR) {
            return findServiceNode(TransportType.TOR)
                    .flatMap(serviceNode -> {
                        try {
                            return serviceNode.getSocksProxy();
                        } catch (IOException e) {
                            log.warn("Could not get socks proxy", e);
                            return Optional.empty();
                        }
                    });
        }
        return Optional.empty();
    }

    public Map<TransportType, Observable<Node.State>> getDefaultNodeStateByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDefaultNode().getObservableState()));
    }

    public Optional<ServiceNode> findServiceNode(TransportType transport) {
        return Optional.ofNullable(map.get(transport));
    }

    public Optional<Node> findNode(TransportType transport, NetworkId networkId) {
        return findServiceNode(transport)
                .flatMap(serviceNode -> serviceNode.findNode(networkId));
    }

    public Set<Node> findNodesOfAllTransports(NetworkId networkId) {
        return map.values().stream()
                .flatMap(serviceNode -> serviceNode.findNode(networkId).stream())
                .collect(Collectors.toSet());
    }

    public Map<TransportType, CompletableFuture<Boolean>> isPeerOnlineAsync(NetworkId networkId,
                                                                            AddressByTransportTypeMap peer) {
        return peer.entrySet().stream().map(entry -> {
                    TransportType transportType = entry.getKey();
                    if (map.containsKey(transportType)) {
                        return new Pair<>(transportType, map.get(transportType).isPeerOnlineAsync(networkId, entry.getValue()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    public Collection<ServiceNode> getAllServiceNodes() {
        return map.values();
    }

    public Map<TransportType, ServiceNode> getServiceNodesByTransport() {
        return map;
    }

    public CompletableFuture<Report> requestReport(Address address) {
        return map.get(address.getTransportType()).requestReport(address);
    }
}
