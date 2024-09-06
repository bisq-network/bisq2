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
import bisq.common.threading.ThreadName;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.SendMessageResult;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportConfig;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.BootstrapInfo;
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
                                   Optional<ResendMessageService> resendMessageService) {
        this.supportedTransportTypes = supportedTransportTypes;

        authorizationService = new AuthorizationService(authorizationServiceConfig,
                hashCashProofOfWorkService,
                equihashProofOfWorkService,
                features);

        supportedTransportTypes.forEach(transportType -> {
            TransportConfig transportConfig = configByTransportType.get(transportType);
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    features,
                    transportConfig,
                    transportConfig.getDefaultNodeSocketTimeout(),
                    transportConfig.getUserNodeSocketTimeout(),
                    transportConfig.getDevModeDelayInMs(),
                    transportConfig.getSendMessageThrottleTime(),
                    transportConfig.getReceiveMessageThrottleTime());
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
                    transportType);
            map.put(transportType, serviceNode);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<TransportType, CompletableFuture<Node>> getInitializedDefaultNodeByTransport(NetworkId defaultNetworkId) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> supplyAsync(() -> {
                                    ThreadName.set(this, "getInitializedDefaultNode-" + entry.getKey().name());
                                    return entry.getValue().getInitializedDefaultNode(defaultNetworkId);
                                },
                                NETWORK_IO_POOL)));
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
        return supplyAsync(() -> {
            ThreadName.set(this, "supplyInitializedNode-" + StringUtils.truncate(networkId.getAddresses(), 10));
            ServiceNode serviceNode = map.get(transportType);
            if (serviceNode.isNodeInitialized(networkId)) {
                return serviceNode.findNode(networkId).orElseThrow();
            } else {
                return serviceNode.initializeNode(networkId);
            }
        }, NETWORK_IO_POOL);
    }

    public void addSeedNodes(Set<AddressByTransportTypeMap> seedNodeMaps) {
        supportedTransportTypes.forEach(transportType -> {
            Set<Address> seeds = seedNodeMaps.stream()
                    .map(map -> map.get(transportType))
                    .collect(Collectors.toSet());
            map.get(transportType).addSeedNodeAddresses(seeds);
        });
    }

    public void addSeedNode(AddressByTransportTypeMap seedNodeMap) {
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
                                              NetworkId senderNetworkId) {
        SendMessageResult sendMessageResult = new SendMessageResult();
        receiverNetworkId.getAddressByTransportTypeMap().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                ServiceNode serviceNode = map.get(transportType);
                SendConfidentialMessageResult result = serviceNode.confidentialSend(envelopePayloadMessage,
                        receiverNetworkId,
                        address,
                        receiverNetworkId.getPubKey(),
                        senderKeyPair,
                        senderNetworkId);
                sendMessageResult.put(transportType, result);
            }
        });
        return sendMessageResult;
    }

    public Map<TransportType, Connection> send(NetworkId senderNetworkId,
                                               EnvelopePayloadMessage envelopePayloadMessage,
                                               AddressByTransportTypeMap receiver) {
        return receiver.entrySet().stream().map(entry -> {
                    TransportType transportType = entry.getKey();
                    if (map.containsKey(transportType)) {
                        return new Pair<>(transportType, map.get(transportType).send(senderNetworkId, envelopePayloadMessage, entry.getValue()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
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

    public Map<TransportType, Observable<Node.State>> getDefaultNodeStateByTransportType() {
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

    public Map<TransportType, Boolean> isPeerOnline(NetworkId networkId, AddressByTransportTypeMap peer) {
        return peer.entrySet().stream().map(entry -> {
                    TransportType transportType = entry.getKey();
                    if (map.containsKey(transportType)) {
                        return new Pair<>(transportType, map.get(transportType).isPeerOnline(networkId, entry.getValue()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    public Collection<ServiceNode> getAllServices() {
        return map.values();
    }

    public CompletableFuture<Report> requestReport(Address address) {
        return map.get(address.getTransportType()).requestReport(address);
    }
}
