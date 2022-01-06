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

package bisq.network;


import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.http.HttpService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.message.Proto;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.AuthenticatedNetworkIdPayload;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService {
    public static final ExecutorService NETWORK_IO_POOL = ExecutorFactory.newCachedThreadPool("NetworkService.network-IO-pool");
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    public static record Config(String baseDir,
                                Transport.Config transportConfig,
                                Set<Transport.Type> supportedTransportTypes,
                                ServiceNode.Config serviceNodeConfig,
                                Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
    }

    private final KeyPairService keyPairService;
    @Getter
    private final HttpService httpService;
    @Getter
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance 
    @Getter
    private final Set<Transport.Type> supportedTransportTypes;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;

    private final Map<String, NetworkId> networkIdByNodeId = new ConcurrentHashMap<>();

    public NetworkService(Config config, KeyPairService keyPairService, PersistenceService persistenceService) {
        this.keyPairService = keyPairService;
        httpService = new HttpService();
        socks5ProxyAddress = config.socks5ProxyAddress;
        supportedTransportTypes = config.supportedTransportTypes();
        serviceNodesByTransport = new ServiceNodesByTransport(config.transportConfig(),
                supportedTransportTypes,
                config.serviceNodeConfig(),
                config.peerGroupServiceConfigByTransport,
                config.seedAddressesByTransport(),
                keyPairService,
                persistenceService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

   /* public CompletableFuture<Boolean> initialize() {
        return initialize(NetworkUtils.findFreeSystemPort());
    }

    public CompletableFuture<Boolean> initialize(int port) {
        return serviceNodesByTransport.initializeServerAsync(port);
    }*/

 /*   public CompletableFuture<Boolean> initializePeerGroup() {
        return serviceNodesByTransport.initializePeerGroupAsync();
        //  return supplyAsync(serviceNodesByTransport::initializePeerGroup, NetworkService.NETWORK_IO_POOL);
    }*/

    public CompletableFuture<Boolean> bootstrap() {
        return bootstrap(NetworkUtils.findFreeSystemPort());
    }

    public CompletableFuture<Boolean> bootstrap(int port) {
        return bootstrap(port, Node.DEFAULT_NODE_ID);
    }

    public CompletableFuture<Boolean> bootstrap(String nodeId) {
        return bootstrap(NetworkUtils.findFreeSystemPort(), nodeId);
    }

    public CompletableFuture<Boolean> bootstrap(int port, String nodeId) {
        return serviceNodesByTransport.bootstrapAsync(port, nodeId);
    }

    public Map<Transport.Type, CompletableFuture<Boolean>> maybeInitializeServerAsync() {
        return maybeInitializeServerAsync(NetworkUtils.findFreeSystemPort());
    }

    public Map<Transport.Type, CompletableFuture<Boolean>> maybeInitializeServerAsync(int port) {
        return maybeInitializeServerAsync(port, Node.DEFAULT_NODE_ID);
    }

    public Map<Transport.Type, CompletableFuture<Boolean>> maybeInitializeServerAsync(String nodeId) {
        return maybeInitializeServerAsync(NetworkUtils.findFreeSystemPort(), nodeId);
    }

    public Map<Transport.Type, CompletableFuture<Boolean>> maybeInitializeServerAsync(int port, String nodeId) {
        return serviceNodesByTransport.maybeInitializeServerAsync(port, nodeId);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFutureUtils.allOf(serviceNodesByTransport.shutdown(), httpService.shutdown())
                .thenApply(list -> null);
    }


    public CompletableFuture<Map<Transport.Type, ConfidentialMessageService.Result>> confidentialSendAsync(Message message,
                                                                                                           NetworkId receiverNetworkId,
                                                                                                           KeyPair senderKeyPair,
                                                                                                           String senderNodeId) {
        return supplyAsync(() -> confidentialSend(message, receiverNetworkId, senderKeyPair, senderNodeId), NETWORK_IO_POOL);
    }

    public Map<Transport.Type, ConfidentialMessageService.Result> confidentialSend(Message message,
                                                                                   NetworkId receiverNetworkId,
                                                                                   KeyPair senderKeyPair,
                                                                                   String senderNodeId) {
        return serviceNodesByTransport.confidentialSend(message, receiverNetworkId, senderKeyPair, senderNodeId);
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addDataAsync(Proto data, String nodeId, String keyId) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        return CompletableFutureUtils.allOf(maybeInitializeServerAsync(nodeId).values())
                .thenCompose(list -> {
                    maybeInitializeServerAsync(nodeId);
                    NetworkId networkId = findNetworkId(nodeId, pubKey).orElseThrow();
                    AuthenticatedNetworkIdPayload netWorkPayload = new AuthenticatedNetworkIdPayload(data, networkId);
                    return serviceNodesByTransport.addNetworkPayloadAsync(netWorkPayload, keyPair)
                            .whenComplete((broadCastResultFutures, throwable) -> {
                                broadCastResultFutures.forEach(broadCastResultFuture -> {
                                    broadCastResultFuture.whenComplete((broadCastResult, throwable2) -> {
                                        //todo apply state
                                    });
                                });
                            });

                });
    }

    public void addDataServiceListener(DataService.Listener listener) {
        serviceNodesByTransport.addDataServiceListener(listener);
    }

    public void removeDataServiceListener(DataService.Listener listener) {
        serviceNodesByTransport.removeDataServiceListener(listener);
    }

    public void addMessageListener(Node.Listener listener) {
        serviceNodesByTransport.addMessageListener(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        serviceNodesByTransport.removeMessageListener(listener);
    }

    public CompletableFuture<BaseHttpClient> getHttpClient(String url, String userAgent, Transport.Type transportType) {
        return httpService.getHttpClient(url, userAgent, transportType, serviceNodesByTransport.getSocksProxy(), socks5ProxyAddress);
    }

    public Map<Transport.Type, Map<String, Address>> findMyAddresses() {
        return serviceNodesByTransport.findMyAddresses();
    }

    public Optional<Map<String, Address>> findMyAddresses(Transport.Type transport) {
        return serviceNodesByTransport.findMyAddresses(transport);
    }

    public Optional<Address> findMyAddresses(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findMyAddresses(transport, nodeId);
    }

    public Map<Transport.Type, Address> getAddressByNetworkType(String nodeId) {
        return supportedTransportTypes.stream()
                .filter(transportType -> findMyAddresses(transportType, nodeId).isPresent())
                .collect(Collectors.toMap(transportType -> transportType,
                        transportType -> findMyAddresses(transportType, nodeId).orElseThrow()));
    }

    public Optional<Address> findMyDefaultAddress(Transport.Type transport) {
        return serviceNodesByTransport.findMyAddresses(transport, Node.DEFAULT_NODE_ID);
    }

    public Optional<Node> findDefaultNode(Transport.Type transport) {
        return findNode(transport, Node.DEFAULT_NODE_ID);
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findNode(transport, nodeId);
    }

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return serviceNodesByTransport.findServiceNode(transport);
    }

    public Map<Transport.Type, ServiceNode.State> getStateByTransportType() {
        return serviceNodesByTransport.getStateByTransportType();
    }

    public boolean isTransportTypeSupported(Transport.Type transportType) {
        return getSupportedTransportTypes().contains(transportType);
    }

    public Optional<NetworkId> findNetworkId(String nodeId, PubKey pubKey) {
        if (networkIdByNodeId.containsKey(nodeId)) {
            return Optional.of(networkIdByNodeId.get(nodeId));
        } else {
            Map<Transport.Type, Address> addressByNetworkType = getAddressByNetworkType(nodeId);
            if (supportedTransportTypes.size() == addressByNetworkType.size()) {
                NetworkId networkId = new NetworkId(addressByNetworkType, pubKey, nodeId);
                networkIdByNodeId.put(nodeId, networkId);
                persist();
                return Optional.of(networkId);
            } else {
                return Optional.empty();
            }
        }
    }

    private void persist() {
        //todo
    }
}
