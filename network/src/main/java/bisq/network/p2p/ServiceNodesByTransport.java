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
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.UnrestrictedAuthorizationService;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.*;
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
    private final Map<Transport.Type, ServiceNode> map = new ConcurrentHashMap<>();

    public ServiceNodesByTransport(Transport.Config transportConfig,
                                   Set<Transport.Type> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                   Optional<DataService> dataService,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService) {
        long socketTimeout = TimeUnit.MINUTES.toMillis(5);

        supportedTransportTypes.forEach(transportType -> {
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    new UnrestrictedAuthorizationService(),
                    transportConfig,
                    (int) socketTimeout);
            List<Address> seedAddresses = seedAddressesByTransport.get(transportType);
            checkNotNull(seedAddresses, "Seed nodes must be setup for %s", transportType);
            PeerGroupService.Config peerGroupServiceConfig = peerGroupServiceConfigByTransport.get(transportType);
            ServiceNode serviceNode = new ServiceNode(serviceNodeConfig,
                    nodeConfig,
                    peerGroupServiceConfig,
                    dataService,
                    keyPairService,
                    persistenceService,
                    seedAddresses);
            map.put(transportType, serviceNode);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrapToNetwork(int port, String nodeId) {
        return CompletableFutureUtils.allOf(map.values().stream().map(networkNode ->
                runAsync(() -> networkNode.maybeInitializeServer(nodeId, port), NETWORK_IO_POOL)
                        .whenComplete((__, throwable) -> {
                            if (throwable == null) {
                                networkNode.maybeInitializePeerGroup();
                            } else {
                                log.error(throwable.toString());
                            }
                        }))).thenApply(list -> true);
    }

    public Map<Transport.Type, CompletableFuture<Boolean>> maybeInitializeServerAsync(int port, String nodeId) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> supplyAsync(() -> entry.getValue().maybeInitializeServer(nodeId, port), NETWORK_IO_POOL)));
    }

    public Map<Transport.Type, ConfidentialMessageService.Result> confidentialSend(Message message,
                                                                                   NetworkId receiverNetworkId,
                                                                                   KeyPair senderKeyPair,
                                                                                   String senderNodeId) {
        Map<Transport.Type, ConfidentialMessageService.Result> resultsByType = new HashMap<>();
        receiverNetworkId.addressByNetworkType().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                ServiceNode serviceNode = map.get(transportType);
                try {
                    ConfidentialMessageService.Result result = serviceNode.confidentialSend(message, address, receiverNetworkId.getPubKey(), senderKeyPair, senderNodeId);
                    resultsByType.put(transportType, result);
                } catch (Throwable throwable) {
                    resultsByType.put(transportType, new ConfidentialMessageService.Result(ConfidentialMessageService.State.FAILED)
                            .setErrorMsg(throwable.getMessage()));
                }
            } else {
                //todo relay case
            }
        });
        return resultsByType;
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


    public void addMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.addMessageListener(listener));
    }

    public void removeMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeMessageListener(listener));
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFutureUtils.allOf(map.values().stream().map(ServiceNode::shutdown))
                .orTimeout(6, TimeUnit.SECONDS)
                .thenApply(list -> {
                    map.clear();
                    return null;
                });
    }

    public Map<Transport.Type, Map<String, Address>> findMyAddresses() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAddressesByNodeId()));
    }

    public Optional<Map<String, Address>> findMyAddresses(Transport.Type transport) {
        return Optional.ofNullable(findMyAddresses().get(transport));
    }

    public Optional<Address> findMyAddresses(Transport.Type transport, String nodeId) {
        return findMyAddresses(transport).flatMap(map -> Optional.ofNullable(map.get(nodeId)));
    }

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return Optional.ofNullable(map.get(transport));
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return findServiceNode(transport)
                .flatMap(serviceNode -> serviceNode.findNode(nodeId));
    }

    public Map<Transport.Type, ServiceNode.State> getStateByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getState().get()));
    }
}
