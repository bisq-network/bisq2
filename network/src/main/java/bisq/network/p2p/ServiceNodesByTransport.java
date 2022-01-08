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
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.storage.Storage;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class ServiceNodesByTransport {
    private static final Logger log = LoggerFactory.getLogger(ServiceNodesByTransport.class);

    private final Map<Transport.Type, ServiceNode> map = new ConcurrentHashMap<>();
    private final Optional<Storage> storage;
    @Getter
    private final Optional<DataService> dataService;

    public ServiceNodesByTransport(Transport.Config transportConfig,
                                   Set<Transport.Type> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                   KeyPairService keyPairService,
                                   PersistenceService persistenceService) {
        long socketTimeout = TimeUnit.MINUTES.toMillis(5);

        boolean supportDataService = serviceNodeConfig.services().contains(ServiceNode.Service.DATA);
        storage = supportDataService ? Optional.of(new Storage(persistenceService)) : Optional.empty();
        dataService = supportDataService ? Optional.of(new DataService(storage.orElseThrow())) : Optional.empty();

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

    // We require all servers on all transports to be initialized, but do not wait for the peer group initialisation 
    // has completed
    public CompletableFuture<Boolean> bootstrapPeerGroupAsync(int port, String nodeId) {
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
                //todo
            }
        });
        return resultsByType;
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addNetworkPayloadAsync(NetworkPayload networkPayload, KeyPair keyPair) {
        if (dataService.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("DataService need to be enabled when using addNetworkPayload"));
        }
        return dataService.get().addNetworkPayloadAsync(networkPayload, keyPair);
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addMailboxPayloadAsync(MailboxPayload mailboxPayload,
                                                                                              KeyPair senderKeyPair,
                                                                                              PublicKey receiverPublicKey) {
        if (dataService.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("DataService need to be enabled when using addNetworkPayload"));
        }
        return dataService.get().addMailboxPayloadAsync(mailboxPayload, senderKeyPair, receiverPublicKey);
    }

    public void removeNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        if (dataService.isEmpty()) {
            throw new IllegalStateException("DataService need to be enabled when using removeNetworkPayload");
        }
        dataService.get().removeNetworkPayload(networkPayload, keyPair);
    }

    public void requestInventory(Storage.StoreType storeType) {
        if (dataService.isEmpty()) {
            throw new IllegalStateException("DataService need to be enabled when using requestInventory");
        }
        dataService.get().requestInventory(storeType);
    }

    public void requestInventory(String storeName) {
        if (dataService.isEmpty()) {
            throw new IllegalStateException("DataService need to be enabled when using requestInventory");
        }
        dataService.get().requestInventory(storeName);
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
        dataService.ifPresent(dataService -> dataService.addListener(listener));
    }

    public void removeDataServiceListener(DataService.Listener listener) {
        dataService.ifPresent(dataService -> dataService.removeListener(listener));
    }

    public void addMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.addMessageListener(listener));
    }

    public void removeMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeMessageListener(listener));
    }

    public CompletableFuture<Void> shutdown() {
        List<CompletableFuture<Void>> futures = map.values().stream().map(ServiceNode::shutdown).collect(Collectors.toList());
        futures.add(dataService.map(DataService::shutdown).orElse(CompletableFuture.completedFuture(null)));
        storage.ifPresent(Storage::shutdown);
        return CompletableFutureUtils.allOf(futures)
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
