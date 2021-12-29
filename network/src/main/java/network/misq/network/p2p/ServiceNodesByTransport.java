/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p;


import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.authorization.UnrestrictedAuthorizationService;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.confidential.ConfidentialMessageService;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.network.p2p.services.data.broadcast.BroadcastResult;
import network.misq.network.p2p.services.data.filter.DataFilter;
import network.misq.network.p2p.services.data.inventory.RequestInventoryResult;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.security.KeyPairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServiceNodesByTransport {
    private static final Logger log = LoggerFactory.getLogger(ServiceNodesByTransport.class);

    private final Map<Transport.Type, ServiceNode> map = new ConcurrentHashMap<>();

    public ServiceNodesByTransport(Transport.Config transportConfig,
                                   Set<Transport.Type> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                   DataService.Config dataServiceConfig,
                                   KeyPairRepository keyPairRepository) {

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
                    dataServiceConfig,
                    keyPairRepository,
                    seedAddresses);
            map.put(transportType, serviceNode);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean initializeServer(int port) {
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int numNodes = map.size();
        map.values().forEach(networkNode -> {
            try {
                networkNode.initializeServer(Node.DEFAULT_NODE_ID, port);
                completed.incrementAndGet();
            } catch (Throwable throwable) {
                failed.incrementAndGet();
            }
        });
        return completed.get() == numNodes;
    }

    public boolean bootstrap(int port) {
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int numNodes = map.size();
        map.values().forEach(networkNode -> {
            try {
                networkNode.bootstrap(Node.DEFAULT_NODE_ID, port);
                completed.incrementAndGet();
            } catch (Throwable throwable) {
                failed.incrementAndGet();
            }
        });
        return completed.get() == numNodes;
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
                    ConfidentialMessageService.Result result = serviceNode.confidentialSend(message, address, receiverNetworkId.pubKey(), senderKeyPair, senderNodeId);
                    resultsByType.put(transportType, result);
                } catch (Throwable throwable) {
                    resultsByType.put(transportType, new ConfidentialMessageService.Result(ConfidentialMessageService.State.FAILED)
                            .errorMsg(throwable.getMessage()));
                }
            } else {
                //todo
            }
        });
        return resultsByType;
    }

    public CompletableFuture<List<BroadcastResult>> addNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        return CompletableFutureUtils.allOf(
                map.values().stream()
                        .map(serviceNode -> serviceNode.addNetworkPayload(networkPayload, keyPair))
        );
    }

    public void requestRemoveData(Message message, Consumer<BroadcastResult> resultHandler) {
        map.values().forEach(dataService -> {
            dataService.requestRemoveData(message)
                    .whenComplete((gossipResult, throwable) -> {
                        if (gossipResult != null) {
                            resultHandler.accept(gossipResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    public void requestInventory(DataFilter dataFilter, Consumer<RequestInventoryResult> resultHandler) {
        map.values().forEach(serviceNode -> {
            serviceNode.requestInventory(dataFilter)
                    .whenComplete((requestInventoryResult, throwable) -> {
                        if (requestInventoryResult != null) {
                            resultHandler.accept(requestInventoryResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
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

    public void addDataServiceListener(DataService.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.addDataServiceListener(listener));
    }

    public void removeDataServiceListener(DataService.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeDataServiceListener(listener));
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

    public Map<Transport.Type, State> getStateByTransportType() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getState()));
    }
}
