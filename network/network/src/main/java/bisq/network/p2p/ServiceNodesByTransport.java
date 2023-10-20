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
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.common.TransportConfig;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.AddressByTransportTypeMap;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.BootstrapInfo;
import bisq.network.p2p.node.transport.TransportType;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import bisq.security.pow.ProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Maintains a map of ServiceNodes by transportType. Delegates to relevant ServiceNode.
 *
 * TODO: if we change the supported transports we need to clean up the persisted networkIds.
 */
@Slf4j
public class ServiceNodesByTransport implements PersistenceClient<ServiceNodesByTransportStore> {
    @Getter
    private final ServiceNodesByTransportStore persistableStore = new ServiceNodesByTransportStore();
    private final KeyPairService keyPairService;
    @Getter
    private final Persistence<ServiceNodesByTransportStore> persistence;
    @Getter
    private final Map<TransportType, ServiceNode> map = new ConcurrentHashMap<>();
    private final Set<TransportType> supportedTransportTypes;

    public ServiceNodesByTransport(Map<TransportType, TransportConfig> configByTransportType,
                                   Set<TransportType> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<TransportType, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<TransportType, Set<Address>> seedAddressesByTransport,
                                   Optional<DataService> dataService,
                                   Optional<MessageDeliveryStatusService> messageDeliveryStatusService,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService,
                                   ProofOfWorkService proofOfWorkService) {
        this.supportedTransportTypes = supportedTransportTypes;
        this.keyPairService = keyPairService;

        persistence = persistenceService.getOrCreatePersistence(this,
                NetworkService.SUB_PATH,
                "ServiceNodesByTransportStore",
                persistableStore);

        supportedTransportTypes.forEach(transportType -> {
            TransportConfig transportConfig = configByTransportType.get(transportType);

            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    new AuthorizationService(proofOfWorkService),
                    transportConfig,
                    transportConfig.getSocketTimeout());
            Set<Address> seedAddresses = seedAddressesByTransport.get(transportType);
            checkNotNull(seedAddresses, "Seed nodes must be setup for %s", transportType);
            PeerGroupService.Config peerGroupServiceConfig = peerGroupServiceConfigByTransport.get(transportType);
            ServiceNode serviceNode = new ServiceNode(serviceNodeConfig,
                    nodeConfig,
                    peerGroupServiceConfig,
                    dataService,
                    messageDeliveryStatusService,
                    keyPairService,
                    persistenceService,
                    seedAddresses,
                    transportType);
            map.put(transportType, serviceNode);
        });

        setupDefaultNodeInitializeObserver();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return A CompletableFuture with the success state if at least one of the service node initialisations was
     * successfully completed. In case all fail, we complete exceptionally.
     */
    public CompletableFuture<Boolean> initialize() {
        // We initialize all service nodes per transport type in parallel. As soon one has completed we
        // return a success state.
        String defaultNodeId = Node.DEFAULT;
        Optional<NetworkId> persistedDefaultNodeId = findPersistedNetworkId(defaultNodeId);
        Stream<CompletableFuture<Void>> futures = map.entrySet().stream()
                .map(entry -> runAsync(() -> {
                    ServiceNode serviceNode = entry.getValue();
                    serviceNode.initializeTransport();

                    TransportType transportType = entry.getKey();
                    Optional<Integer> persistedDefaultNodePort = persistedDefaultNodeId
                            .map(e -> e.getAddressByTransportTypeMap().get(transportType).getPort());
                    serviceNode.initializeDefaultNode(persistedDefaultNodePort);
                    serviceNode.findNode(defaultNodeId)
                            .ifPresent(node -> maybePersistNetworkId(transportType, node, keyPairService.getDefaultPubKey()));

                    serviceNode.initializePeerGroup();
                }, NETWORK_IO_POOL));
        return CompletableFutureUtils.anyOf(futures).thenApply(nil -> true);
    }

    public Map<TransportType, CompletableFuture<Node>> getInitializedNodeByTransport(String nodeId, PubKey pubKey) {
        // We initialize all service nodes per transport type in parallel. As soon one has completed we
        // return a success state.
        Optional<NetworkId> persistedNodeId = findPersistedNetworkId(nodeId, pubKey);
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(),
                        entry -> supplyAsync(() -> {
                            ServiceNode serviceNode = entry.getValue();
                            if (serviceNode.isNodeInitialized(nodeId)) {
                                return serviceNode.findNode(nodeId).orElseThrow();
                            } else {
                                TransportType transportType = entry.getKey();
                                Optional<Integer> persistedNodePort = persistedNodeId
                                        .map(e -> e.getAddressByTransportTypeMap().get(transportType).getPort());
                                Node node = serviceNode.getInitializedNode(nodeId, persistedNodePort);
                                maybePersistNetworkId(transportType, node, pubKey);
                                return node;
                            }
                        }, NETWORK_IO_POOL)));
    }

    public CompletableFuture<NetworkId> getNetworkIdOfInitializedNode(String nodeId, PubKey pubKey) {
        Collection<CompletableFuture<Node>> futures = getInitializedNodeByTransport(nodeId, pubKey).values();
        // As we persist networkIds after initialize, and we require all futures to be completed we can be sure that
        // the networkId is complete with all addresses of all our supported transports.
        return CompletableFutureUtils.allOf(futures)
                .thenApply(pairList -> getPersistedNetworkId(nodeId, pubKey));
    }

    public CompletableFuture<Boolean> shutdown() {
        Stream<CompletableFuture<Boolean>> futures = map.values().stream().map(ServiceNode::shutdown);
        return CompletableFutureUtils.allOf(futures)
                .handle((list, throwable) -> {
                    map.clear();
                    return throwable == null && list.stream().allMatch(e -> e);
                });
    }

    public boolean isNodeOnAllTransportsInitialized(String nodeId) {
        return map.values().stream()
                .allMatch(serviceNode -> serviceNode.isNodeInitialized(nodeId));
    }

    public boolean isInitialized(TransportType transportType, String nodeId) {
        return map.get(transportType).isNodeInitialized(nodeId);
    }

    public void addSeedNode(AddressByTransportTypeMap seedNode) {
        supportedTransportTypes.forEach(transportType -> {
            Address seedNodeAddress = seedNode.get(transportType);
            map.get(transportType).addSeedNodeAddress(seedNodeAddress);
        });
    }

    public void removeSeedNode(AddressByTransportTypeMap seedNode) {
        supportedTransportTypes.forEach(transportType -> {
            Address seedNodeAddress = seedNode.get(transportType);
            map.get(transportType).removeSeedNodeAddress(seedNodeAddress);
        });
    }

    public NetworkService.SendMessageResult confidentialSend(NetworkMessage networkMessage,
                                                             NetworkId receiverNetworkId,
                                                             KeyPair senderKeyPair,
                                                             String senderNodeId) {
        NetworkService.SendMessageResult resultsByType = new NetworkService.SendMessageResult();
        receiverNetworkId.getAddressByTransportTypeMap().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                ServiceNode serviceNode = map.get(transportType);
                ConfidentialMessageService.Result result = serviceNode.confidentialSend(networkMessage,
                        address,
                        receiverNetworkId.getPubKey(),
                        senderKeyPair,
                        senderNodeId);
                resultsByType.put(transportType, result);
            }
        });
        return resultsByType;
    }

    public Map<TransportType, Connection> send(String senderNodeId,
                                               NetworkMessage networkMessage,
                                               AddressByTransportTypeMap receiver) {
        return receiver.entrySet().stream().map(entry -> {
                    TransportType transportType = entry.getKey();
                    if (map.containsKey(transportType)) {
                        return new Pair<>(transportType, map.get(transportType).send(senderNodeId, networkMessage, entry.getValue()));
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

    public Optional<Node> findNode(TransportType transport, String nodeId) {
        return findServiceNode(transport)
                .flatMap(serviceNode -> serviceNode.findNode(nodeId));
    }

    public Map<TransportType, Map<String, Address>> getAddressesByNodeIdMapByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAddressesByNodeId()));
    }

    public Optional<Map<String, Address>> findAddressesByNodeId(TransportType transport) {
        return Optional.ofNullable(getAddressesByNodeIdMapByTransportType().get(transport));
    }

    public Optional<Address> findAddress(TransportType transport, String nodeId) {
        return findAddressesByNodeId(transport)
                .flatMap(addressesByNodeId -> Optional.ofNullable(addressesByNodeId.get(nodeId)));
    }

    public Optional<NetworkId> findPersistedNetworkId(String nodeId) {
        return Optional.ofNullable(persistableStore.getNetworkIdByNodeId().get(nodeId));
    }

    public Optional<NetworkId> findPersistedNetworkId(String nodeId, PubKey pubKey) {
        return findPersistedNetworkId(nodeId).filter(networkId -> networkId.getPubKey().equals(pubKey));
    }

    public NetworkId getPersistedNetworkId(String nodeId, PubKey pubKey) {
        Optional<NetworkId> networkId = findPersistedNetworkId(nodeId, pubKey)
                .filter(e -> e.getAddressByTransportTypeMap().size() == supportedTransportTypes.size());
        checkArgument(networkId.isPresent(),
                "We expect that the NodeId has all addresses for all supported transport types");
        return networkId.get();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void maybePersistNetworkId(TransportType transportType, Node node, PubKey pubKey) {
        node.findMyAddress().ifPresent(address -> {
            String nodeId = node.getNodeId();
            if (persistableStore.getNetworkIdByNodeId().containsKey(nodeId)) {
                NetworkId networkId = persistableStore.getNetworkIdByNodeId().get(nodeId);
                networkId.getAddressByTransportTypeMap().put(transportType, address);
            } else {
                AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
                addressByTransportTypeMap.put(transportType, address);
                NetworkId networkId = new NetworkId(addressByTransportTypeMap, pubKey, nodeId);
                persistableStore.getNetworkIdByNodeId().put(nodeId, networkId);
            }
            persist();
        });
    }

    private void setupDefaultNodeInitializeObserver() {
        map.forEach((transportType, serviceNode) -> {
            serviceNode.getState().addObserver(state -> {
                // Once we have our default node initialize wer persist the address in the NetworkId
                if (state == ServiceNode.State.DEFAULT_NODE_INITIALIZED) {
                    Node defaultNode = serviceNode.getDefaultNode();
                    maybePersistNetworkId(transportType, defaultNode, keyPairService.getDefaultPubKey());
                }
            });
        });
    }
}
