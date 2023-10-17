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
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.TransportType;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.pow.ProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
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
@Slf4j
public class ServiceNodesByTransport {
    @Getter
    private final Map<TransportType, ServiceNode> map = new ConcurrentHashMap<>();
    private final Set<TransportType> supportedTransportTypes;

    public ServiceNodesByTransport(Map<TransportType, TransportConfig> configByTransportType,
                                   Set<TransportType> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<TransportType, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<TransportType, Set<Address>> seedAddressesByTransport,
                                   Optional<DataService> dataService,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService,
                                   ProofOfWorkService proofOfWorkService) {
        this.supportedTransportTypes = supportedTransportTypes;
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
                    keyPairService,
                    persistenceService,
                    seedAddresses,
                    transportType);
            map.put(transportType, serviceNode);
        });
    }


    public CompletableFuture<Boolean> shutdown() {
        Stream<CompletableFuture<Boolean>> futures = map.values().stream().map(ServiceNode::shutdown);
        return CompletableFutureUtils.allOf(futures)
                .handle((list, throwable) -> {
                    map.clear();
                    return throwable == null && list.stream().allMatch(e -> e);
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void initializeNode(TransportType transportType, String nodeId, int portByTransport) {
        map.get(transportType).initializeNode(nodeId, portByTransport);
    }

    public boolean isInitialized(TransportType transportType, String nodeId) {
        return map.get(transportType).isNodeInitialized(nodeId);
    }

    public void initializePeerGroup(TransportType transportType) {
        map.get(transportType).initializePeerGroup();
    }

    public void addSeedNodeAddressByTransport(Map<TransportType, Address> seedNodeAddressesByTransport) {
        supportedTransportTypes.forEach(transportType -> {
            Address seedNodeAddress = seedNodeAddressesByTransport.get(transportType);
            map.get(transportType).addSeedNodeAddress(seedNodeAddress);
        });
    }

    public void removeSeedNodeAddressByTransport(Map<TransportType, Address> seedNodeAddressesByTransport) {
        supportedTransportTypes.forEach(transportType -> {
            Address seedNodeAddress = seedNodeAddressesByTransport.get(transportType);
            map.get(transportType).removeSeedNodeAddress(seedNodeAddress);
        });
    }

    public NetworkService.SendMessageResult confidentialSend(NetworkMessage networkMessage,
                                                             NetworkId receiverNetworkId,
                                                             KeyPair senderKeyPair,
                                                             String senderNodeId) {
        NetworkService.SendMessageResult resultsByType = new NetworkService.SendMessageResult();
        receiverNetworkId.getAddressByNetworkType().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                ServiceNode serviceNode = map.get(transportType);
                try {
                    ConfidentialMessageService.Result result = serviceNode.confidentialSend(networkMessage,
                            address,
                            receiverNetworkId.getPubKey(),
                            senderKeyPair,
                            senderNodeId);
                    resultsByType.put(transportType, result);
                } catch (Throwable throwable) {
                    resultsByType.put(transportType, new ConfidentialMessageService.Result(ConfidentialMessageService.State.FAILED)
                            .setErrorMsg(throwable.getMessage()));
                }
            }
        });
        return resultsByType;
    }

    public Map<TransportType, Connection> send(String senderNodeId,
                                               NetworkMessage networkMessage,
                                               Map<TransportType, Address> receiverAddressByNetworkType) {
        return receiverAddressByNetworkType.entrySet().stream().map(entry -> {
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
}
