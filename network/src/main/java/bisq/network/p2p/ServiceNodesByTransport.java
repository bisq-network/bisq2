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


import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.NetworkService.InitializeServerResult;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.UnrestrictedAuthorizationService;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class ServiceNodesByTransport {
    @Getter
    private final Map<Transport.Type, ServiceNode> map = new ConcurrentHashMap<>();

    public ServiceNodesByTransport(Map<Transport.Type, Transport.Config> configByTransportType,
                                   Set<Transport.Type> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<Transport.Type, Set<Address>> seedAddressesByTransport,
                                   Optional<DataService> dataService,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService) {
        supportedTransportTypes.forEach(transportType -> {
            Transport.Config transportConfig = configByTransportType.get(transportType);
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    new UnrestrictedAuthorizationService(),
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
        return CompletableFutureUtils.allOf(map.values().stream().map(ServiceNode::shutdown))
                .orTimeout(6, TimeUnit.SECONDS)
                .thenApply(list -> {
                    map.clear();
                    return true;
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public InitializeServerResult maybeInitializeServer(Map<Transport.Type, Integer> portByTransport, String nodeId) {
        return new InitializeServerResult(map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        supplyAsync(() -> entry.getValue().maybeInitializeServer(nodeId, portByTransport.get(entry.getKey())),
                                NETWORK_IO_POOL))));
    }

    public CompletableFuture<Boolean> bootstrapToNetwork(Map<Transport.Type, Integer> portByTransport, String nodeId) {
        return CompletableFutureUtils.allOf(map.entrySet().stream()
                .map(entry -> {
                    int port = portByTransport.get(entry.getKey());
                    ServiceNode serviceNode = entry.getValue();
                    return runAsync(() -> serviceNode.maybeInitializeServer(nodeId, port), NETWORK_IO_POOL)
                            .whenComplete((__, throwable) -> {
                                if (throwable == null) {
                                    serviceNode.maybeInitializePeerGroup();
                                } else {
                                    log.error(throwable.toString());
                                }
                            });
                })).thenApply(list -> true);
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
                    ConfidentialMessageService.Result result = serviceNode.confidentialSend(networkMessage, address, receiverNetworkId.getPubKey(), senderKeyPair, senderNodeId);
                    resultsByType.put(transportType, result);
                } catch (Throwable throwable) {
                    resultsByType.put(transportType, new ConfidentialMessageService.Result(ConfidentialMessageService.State.FAILED)
                            .setErrorMsg(throwable.getMessage()));
                }
            }
        });
        return resultsByType;
    }


    public void addMessageListener(MessageListener messageListener) {
        map.values().forEach(serviceNode -> serviceNode.addMessageListener(messageListener));
    }

    public void removeMessageListener(MessageListener messageListener) {
        map.values().forEach(serviceNode -> serviceNode.removeMessageListener(messageListener));
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().addListener(nodeListener));
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        map.values().forEach(serviceNode -> serviceNode.getDefaultNode().removeListener(nodeListener));
    }

    public Optional<Socks5Proxy> getSocksProxy() {
        return findServiceNode(Transport.Type.TOR)
                .flatMap(serviceNode -> {
                    try {
                        return serviceNode.getSocksProxy();
                    } catch (IOException e) {
                        log.warn("Could not get socks proxy", e);
                        return Optional.empty();
                    }
                });
    }

    public Map<Transport.Type, ServiceNode.State> getStateByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getState().get()));
    }

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return Optional.ofNullable(map.get(transport));
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return findServiceNode(transport)
                .flatMap(serviceNode -> serviceNode.findNode(nodeId));
    }

    public Map<Transport.Type, Map<String, Address>> getAddressesByNodeIdMapByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAddressesByNodeId()));
    }

    public Optional<Map<String, Address>> findAddressesByNodeId(Transport.Type transport) {
        return Optional.ofNullable(getAddressesByNodeIdMapByTransportType().get(transport));
    }

    public Optional<Address> findAddress(Transport.Type transport, String nodeId) {
        return findAddressesByNodeId(transport)
                .flatMap(addressesByNodeId -> Optional.ofNullable(addressesByNodeId.get(nodeId)));
    }
}
