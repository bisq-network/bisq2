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
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.SendMessageResult;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportConfig;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.BootstrapInfo;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.confidential.SendConfidentialMessageResult;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.pow.ProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Maintains a map of ServiceNodes by transportType. Delegates to relevant ServiceNode.
 */
// TODO: if we change the supported transports we need to clean up the persisted networkIds.
@Slf4j
public class ServiceNodesByTransport {
    private final Map<TransportType, ServiceNode> map = new ConcurrentHashMap<>();
    private final Set<TransportType> supportedTransportTypes;
    private final AuthorizationService authorizationService;

    public ServiceNodesByTransport(Map<TransportType, TransportConfig> configByTransportType,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<TransportType, PeerGroupManager.Config> peerGroupServiceConfigByTransport,
                                   Map<TransportType, Set<Address>> seedAddressesByTransport,
                                   InventoryService.Config inventoryServiceConfig,
                                   Set<TransportType> supportedTransportTypes,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService,
                                   ProofOfWorkService proofOfWorkService,
                                   Optional<DataService> dataService,
                                   Optional<MessageDeliveryStatusService> messageDeliveryStatusService,
                                   NetworkLoadService networkLoadService) {
        this.supportedTransportTypes = supportedTransportTypes;

        authorizationService = new AuthorizationService(proofOfWorkService);

        supportedTransportTypes.forEach(transportType -> {
            TransportConfig transportConfig = configByTransportType.get(transportType);
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    transportConfig,
                    transportConfig.getDefaultNodeSocketTimeout(),
                    transportConfig.getUserNodeSocketTimeout());
            Set<Address> seedAddresses = seedAddressesByTransport.get(transportType);
            checkNotNull(seedAddresses, "Seed nodes must be setup for %s", transportType);
            PeerGroupManager.Config peerGroupServiceConfig = peerGroupServiceConfigByTransport.get(transportType);

            ServiceNode serviceNode = new ServiceNode(serviceNodeConfig,
                    nodeConfig,
                    peerGroupServiceConfig,
                    inventoryServiceConfig,
                    dataService,
                    messageDeliveryStatusService,
                    keyPairService,
                    persistenceService,
                    authorizationService,
                    seedAddresses,
                    transportType,
                    networkLoadService);
            map.put(transportType, serviceNode);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<TransportType, CompletableFuture<Node>> getInitializedDefaultNodeByTransport(NetworkId defaultNetworkId, TorIdentity defaultTorIdentity) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> supplyAsync(() -> entry.getValue().getInitializedDefaultNode(defaultNetworkId, defaultTorIdentity),
                                NETWORK_IO_POOL)));
    }

    public CompletableFuture<List<Boolean>> shutdown() {
        Stream<CompletableFuture<Boolean>> futures = map.values().stream().map(ServiceNode::shutdown);
        return CompletableFutureUtils.allOf(futures).whenComplete((list, throwable) -> map.clear());
    }

    public Map<TransportType, CompletableFuture<Node>> getInitializedNodeByTransport(NetworkId networkId, TorIdentity torIdentity) {
        // We initialize all service nodes per transport type in parallel. As soon one has completed we
        // return a success state.
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> supplyAsync(() -> {
                            ServiceNode serviceNode = entry.getValue();
                            if (serviceNode.isNodeInitialized(networkId)) {
                                return serviceNode.findNode(networkId).orElseThrow();
                            } else {
                                return serviceNode.getInitializedNode(networkId, torIdentity);
                            }
                        }, NETWORK_IO_POOL)));
    }

    public CompletableFuture<List<Node>> getAllInitializedNodes(NetworkId networkId, TorIdentity torIdentity) {
        Collection<CompletableFuture<Node>> futures = getInitializedNodeByTransport(networkId, torIdentity).values();
        // As we persist networkIds after initialize, and we require all futures to be completed we can be sure that
        // the networkId is complete with all addresses of all our supported transports.
        return CompletableFutureUtils.allOf(futures);
    }

    public boolean isNodeOnAllTransportsInitialized(NetworkId networkId) {
        return map.values().stream()
                .allMatch(serviceNode -> serviceNode.isNodeInitialized(networkId));
    }

    public boolean isInitialized(TransportType transportType, NetworkId networkId) {
        return map.get(transportType).isNodeInitialized(networkId);
    }

    public void addAddressByTransportTypeMaps(Set<AddressByTransportTypeMap> seedNodeMaps) {
        supportedTransportTypes.forEach(transportType -> {
            Set<Address> seeds = seedNodeMaps.stream()
                    .map(map -> map.get(transportType))
                    .collect(Collectors.toSet());
            map.get(transportType).addSeedNodeAddresses(seeds);
        });
    }

    public void addAddressByTransportTypeMap(AddressByTransportTypeMap seedNodeMap) {
        supportedTransportTypes.forEach(transportType ->
                map.get(transportType).addSeedNodeAddress(seedNodeMap.get(transportType)));
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
                                              NetworkId senderNetworkId,
                                              TorIdentity senderTorIdentity) {
        SendMessageResult sendMessageResult = new SendMessageResult();
        receiverNetworkId.getAddressByTransportTypeMap().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                ServiceNode serviceNode = map.get(transportType);
                SendConfidentialMessageResult result = serviceNode.confidentialSend(envelopePayloadMessage,
                        address,
                        receiverNetworkId.getPubKey(),
                        senderKeyPair,
                        senderNetworkId,
                        senderTorIdentity);
                sendMessageResult.put(transportType, result);
            }
        });
        return sendMessageResult;
    }

    public Map<TransportType, Connection> send(NetworkId senderNetworkId,
                                               EnvelopePayloadMessage envelopePayloadMessage,
                                               AddressByTransportTypeMap receiver,
                                               TorIdentity senderTorIdentity) {
        return receiver.entrySet().stream().map(entry -> {
                    TransportType transportType = entry.getKey();
                    if (map.containsKey(transportType)) {
                        return new Pair<>(transportType, map.get(transportType).send(senderNetworkId, envelopePayloadMessage, entry.getValue(), senderTorIdentity));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }


    public void addMessageListener(MessageListener messageListener) {
        map.values().forEach(serviceNode -> serviceNode.addMessageListener(messageListener));
    }

    public void removeMessageListener(MessageListener messageListener) {
        map.values().forEach(serviceNode -> serviceNode.removeMessageListener(messageListener));
    }

    public void addConfidentialMessageListener(ConfidentialMessageListener listener) {
        map.values().forEach(serviceNode -> serviceNode.addConfidentialMessageListener(listener));
    }

    public void removeConfidentialMessageListener(ConfidentialMessageListener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeConfidentialMessageListener(listener));
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().addListener(nodeListener));
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().removeListener(nodeListener));
    }

    public Optional<Socks5Proxy> getSocksProxy() {
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

    public Map<TransportType, Observable<Node.State>> getNodeStateByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDefaultNode().getObservableState()));
    }

    public Map<TransportType, BootstrapInfo> getBootstrapInfoByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getTransportService().getBootstrapInfo()));
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

    public Collection<ServiceNode> getAllServices() {
        return map.values();
    }
}
