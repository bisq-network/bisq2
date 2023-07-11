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

package bisq.network;


import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.http.HttpService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import bisq.security.SignatureUtil;
import bisq.security.pow.ProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.node.transport.Transport.Type.*;
import static bisq.network.p2p.services.data.DataService.BroadCastDataResult;
import static bisq.network.p2p.services.data.DataService.Listener;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.*;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
@Getter
public class NetworkService implements PersistenceClient<NetworkServiceStore>, Service {
    public static final ExecutorService NETWORK_IO_POOL = ExecutorFactory.newCachedThreadPool("NetworkService.network-IO-pool");
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    public static class SendMessageResult extends HashMap<Transport.Type, ConfidentialMessageService.Result> {
        public SendMessageResult() {
            super();
        }
    }

    private final NetworkServiceStore persistableStore = new NetworkServiceStore();
    private final Persistence<NetworkServiceStore> persistence;
    private final KeyPairService keyPairService;
    private final HttpService httpService;
    private final Map<Transport.Type, Integer> defaultNodePortByTransportType;
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance 
    private final Set<Transport.Type> supportedTransportTypes;
    private final ServiceNodesByTransport serviceNodesByTransport;
    private final Optional<DataService> dataService;

    public NetworkService(NetworkServiceConfig config,
                          PersistenceService persistenceService,
                          KeyPairService keyPairService,
                          ProofOfWorkService proofOfWorkService) {
        this.keyPairService = keyPairService;
        httpService = new HttpService();

        boolean supportsDataService = config.getServiceNodeConfig().getServices().contains(ServiceNode.Service.DATA);
        dataService = supportsDataService ? Optional.of(new DataService(new StorageService(persistenceService))) : Optional.empty();

        socks5ProxyAddress = config.getSocks5ProxyAddress();
        supportedTransportTypes = config.getSupportedTransportTypes();
        serviceNodesByTransport = new ServiceNodesByTransport(config.getConfigByTransportType(),
                supportedTransportTypes,
                config.getServiceNodeConfig(),
                config.getPeerGroupServiceConfigByTransport(),
                config.getSeedAddressesByTransport(),
                dataService,
                keyPairService,
                persistenceService,
                proofOfWorkService);

        defaultNodePortByTransportType = config.getDefaultNodePortByTransportType();

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initialize default node and initialize peerGroupService.
     * We require at least one successful bootstrap from the available transports.
     */
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        PubKey pubKey = keyPairService.getDefaultPubKey();
        String nodeId = Node.DEFAULT;
        Stream<CompletableFuture<Void>> futures = getDefaultPortByTransport().entrySet().stream()
                .map(entry -> {
                    Transport.Type type = entry.getKey();
                    Integer port = entry.getValue();
                    return runAsync(() -> {
                        try {
                            serviceNodesByTransport.initializeNode(type, nodeId, port);
                            maybePersistNewNetworkId(nodeId, pubKey);
                            serviceNodesByTransport.initializePeerGroup(type);
                        } catch (Throwable t) {
                            log.error("initialize failed", t);
                        }
                    }, NETWORK_IO_POOL);
                });
        return CompletableFutureUtils.anyOf(futures).thenApply(__ -> true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFutureUtils.allOf(dataService.map(DataService::shutdown).orElse(completedFuture(true)),
                        serviceNodesByTransport.shutdown(),
                        httpService.shutdown())
                .thenApply(list -> list.stream().filter(e -> e).count() == 3);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Initialize node
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isInitialized(String nodeId) {
        return supportedTransportTypes.stream()
                .allMatch(type -> serviceNodesByTransport.isInitialized(type, nodeId));
    }

    public Map<Transport.Type, CompletableFuture<Void>> initializeNode(String nodeId, PubKey pubKey) {
        return initializeNode(getOrCreatePortByTransport(nodeId), nodeId, pubKey);
    }

    private Map<Transport.Type, CompletableFuture<Void>> initializeNode(Map<Transport.Type, Integer> portByTransport,
                                                                        String nodeId,
                                                                        PubKey pubKey) {
        log.info("initializeNode {}", nodeId);
        return portByTransport.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                            Transport.Type type = entry.getKey();
                            Integer port = entry.getValue();
                            if (serviceNodesByTransport.isInitialized(type, nodeId)) {
                                return CompletableFuture.completedFuture(null);
                            } else {
                                return runAsync(() -> {
                                    try {
                                        serviceNodesByTransport.initializeNode(type, nodeId, port);
                                        maybePersistNewNetworkId(nodeId, pubKey);
                                    } catch (Throwable t) {
                                        log.error("initialize node failed", t);
                                    }
                                }, NETWORK_IO_POOL);
                            }
                        }));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get networkId and initialize server
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkId> getInitializedNetworkId(String nodeId, PubKey pubKey) {
        log.info("getInitializedNetworkId {}", nodeId);
        Collection<CompletableFuture<Void>> futures = initializeNode(nodeId, pubKey).values();
        // Once the node of the last transport is completed we are expected to be able to create the networkId
        // as all addresses of all transports are available.
        return CompletableFutureUtils.allOf(futures)
                .thenApply(list -> findNetworkId(nodeId)
                        .or(() -> createNetworkId(nodeId, pubKey))
                        .orElseThrow(() -> {
                            String errorMsg = "Unexpected case. No networkId available after all initializeNode calls are completed.";
                            log.error(errorMsg);
                            return new RuntimeException(errorMsg);
                        }));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send confidential message
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Send message via given senderNetworkIdWithKeyPair to the receiverNetworkId as encrypted message.
     * If peer is offline and if message is of type mailBoxMessage it will not be stored as mailbox message in the
     * network.
     */
    public CompletableFuture<SendMessageResult> confidentialSend(NetworkMessage networkMessage,
                                                                 NetworkId receiverNetworkId,
                                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        return getInitializedNetworkId(senderNetworkIdWithKeyPair.getNodeId(), senderNetworkIdWithKeyPair.getPubKey())
                .thenCompose(networkId -> supplyAsync(() -> serviceNodesByTransport.confidentialSend(networkMessage,
                                receiverNetworkId,
                                senderNetworkIdWithKeyPair.getKeyPair(),
                                senderNetworkIdWithKeyPair.getNodeId()),
                        NETWORK_IO_POOL));
    }

    /**
     * Send message via given senderNodeId to the supported network types of the addresses specified at
     * receiverAddressByNetworkType as direct, unencrypted message. If peer is offline it will not be stored as
     * mailbox message.
     */
    public CompletableFuture<Map<Transport.Type, Connection>> send(String senderNodeId,
                                                                   NetworkMessage networkMessage,
                                                                   Map<Transport.Type, Address> receiverAddressByNetworkType) {
        return supplyAsync(() -> serviceNodesByTransport.send(senderNodeId, networkMessage, receiverAddressByNetworkType),
                NETWORK_IO_POOL);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add/remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadCastDataResult> publishAuthenticatedData(DistributedData distributedData,
                                                                           NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().addAuthenticatedData(authenticatedData, keyPair);
    }

    public CompletableFuture<BroadCastDataResult> removeAuthenticatedData(DistributedData distributedData, NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when removeData is called.");
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().removeAuthenticatedData(authenticatedData, keyPair);
    }

    public CompletableFuture<BroadCastDataResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                        NetworkIdWithKeyPair ownerNetworkIdWithKeyPair,
                                                                        PrivateKey authorizedPrivateKey,
                                                                        PublicKey authorizedPublicKey) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        KeyPair keyPair = ownerNetworkIdWithKeyPair.getKeyPair();
        try {
            byte[] signature = SignatureUtil.sign(authorizedDistributedData.serialize(), authorizedPrivateKey);
            AuthorizedData authorizedData = new AuthorizedData(authorizedDistributedData, signature, authorizedPublicKey);
            return dataService.get().addAuthenticatedData(authorizedData, keyPair);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<BroadCastDataResult> removeAuthorizedData(AuthorizedData authorizedData,
                                                                       NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        return dataService.get().removeAuthenticatedData(authorizedData, ownerNetworkIdWithKeyPair.getKeyPair());
    }

    public CompletableFuture<BroadCastDataResult> publishAppendOnlyData(AppendOnlyData appendOnlyData) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        return dataService.get().addAppendOnlyData(appendOnlyData);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addDataServiceListener(Listener listener) {
        dataService.orElseThrow().addListener(listener);
    }

    public void removeDataServiceListener(Listener listener) {
        dataService.orElseThrow().removeListener(listener);
    }

    public void addMessageListener(MessageListener messageListener) {
        serviceNodesByTransport.addMessageListener(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        serviceNodesByTransport.removeMessageListener(messageListener);
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.addDefaultNodeListener(nodeListener);
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.removeDefaultNodeListener(nodeListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BaseHttpClient getHttpClient(String url, String userAgent, Transport.Type transportType) {
        // socksProxy only supported for TOR
        Optional<Socks5Proxy> socksProxy = transportType == TOR ? serviceNodesByTransport.getSocksProxy() : Optional.empty();
        return httpService.getHttpClient(url, userAgent, transportType, socksProxy, socks5ProxyAddress);
    }

    public Map<Transport.Type, Map<String, Address>> getMyAddresses() {
        return serviceNodesByTransport.getAddressesByNodeIdMapByTransportType();
    }

    public Map<Transport.Type, Address> getAddressByNetworkType(String nodeId) {
        return supportedTransportTypes.stream()
                .filter(transportType -> findAddress(transportType, nodeId).isPresent())
                .collect(Collectors.toMap(transportType -> transportType,
                        transportType -> findAddress(transportType, nodeId).orElseThrow()));
    }

    public Map<Transport.Type, Observable<Node.State>> getNodeStateByTransportType() {
        return serviceNodesByTransport.getNodeStateByTransportType();
    }

    public boolean isTransportTypeSupported(Transport.Type transportType) {
        return getSupportedTransportTypes().contains(transportType);
    }

    // We return the port by transport type if found from the persisted networkId, otherwise we
    // fill in a random free system port for all supported transport types. 
    private Map<Transport.Type, Integer> getDefaultPortByTransport() {
        return getOrCreatePortByTransport(Node.DEFAULT);
    }

    //todo
    private Map<Transport.Type, Integer> getOrCreatePortByTransport(String nodeId) {
        Optional<NetworkId> networkIdOptional = findNetworkId(nodeId);
        // If we have a persisted networkId we take that port otherwise we take random system port.  
        Map<Transport.Type, Integer> persistedOrRandomPortByTransport = supportedTransportTypes.stream()
                .collect(Collectors.toMap(transportType -> transportType,
                        transportType -> networkIdOptional.stream()
                                .map(NetworkId::getAddressByNetworkType)
                                .flatMap(addressByNetworkType -> Optional.ofNullable(addressByNetworkType.get(transportType)).stream())
                                .map(Address::getPort)
                                .findAny()
                                .orElse(NetworkUtils.findFreeSystemPort())));

        // In case of the default node and if we have defined ports in the config we take those.
        Map<Transport.Type, Integer> portByTransport = new HashMap<>();
        if (nodeId.equals(Node.DEFAULT)) {
            if (defaultNodePortByTransportType.containsKey(CLEAR)) {
                portByTransport.put(CLEAR, defaultNodePortByTransportType.get(CLEAR));
            } else if (persistedOrRandomPortByTransport.containsKey(CLEAR)) {
                portByTransport.put(CLEAR, persistedOrRandomPortByTransport.get(CLEAR));
            }
            if (defaultNodePortByTransportType.containsKey(TOR)) {
                portByTransport.put(TOR, defaultNodePortByTransportType.get(TOR));
            } else if (persistedOrRandomPortByTransport.containsKey(TOR)) {
                portByTransport.put(TOR, persistedOrRandomPortByTransport.get(TOR));
            }
            if (defaultNodePortByTransportType.containsKey(I2P)) {
                portByTransport.put(I2P, defaultNodePortByTransportType.get(I2P));
            } else if (persistedOrRandomPortByTransport.containsKey(I2P)) {
                portByTransport.put(I2P, persistedOrRandomPortByTransport.get(I2P));
            }
        } else {
            portByTransport = persistedOrRandomPortByTransport;
        }
        return portByTransport;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get optional 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return serviceNodesByTransport.findServiceNode(transport);
    }

    public Optional<Node> findDefaultNode(Transport.Type transport) {
        return findNode(transport, Node.DEFAULT);
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findNode(transport, nodeId);
    }

    public Optional<Map<String, Address>> findAddressesByNodeId(Transport.Type transport) {
        return serviceNodesByTransport.findAddressesByNodeId(transport);
    }

    public Optional<Address> findAddress(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findAddress(transport, nodeId);
    }

    public Optional<NetworkId> findNetworkId(String nodeId) {
        return Optional.ofNullable(persistableStore.getNetworkIdByNodeId().get(nodeId));
    }

    public Optional<NetworkId> createNetworkId(String nodeId, PubKey pubKey) {
        Map<Transport.Type, Address> addressByNetworkType = getAddressByNetworkType(nodeId);
        // We need the addresses for all transports to be able to create the networkId.
        if (supportedTransportTypes.size() == addressByNetworkType.size()) {
            return Optional.of(new NetworkId(addressByNetworkType, pubKey, nodeId));
        } else {
            // Expected case at first startup or creation of new node when addresses of other transport types are 
            // not available yet. 
            log.debug("supportedTransportTypes.size() != addressByNetworkType.size(). " +
                            "supportedTransportTypes={}, addressByNetworkType={}. nodeId={}",
                    supportedTransportTypes, addressByNetworkType, nodeId);
            return Optional.empty();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add seed node address
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSeedNodeAddressByTransport(Map<Transport.Type, Address> seedNodeAddressesByTransport) {
        this.serviceNodesByTransport.addSeedNodeAddressByTransport(seedNodeAddressesByTransport);
    }

    public void removeSeedNodeAddressByTransport(Map<Transport.Type, Address> seedNodeAddressesByTransport) {
        this.serviceNodesByTransport.removeSeedNodeAddressByTransport(seedNodeAddressesByTransport);
    }

    // If not persisted we try to create the networkId and persist if available.
    private Optional<NetworkId> maybePersistNewNetworkId(String nodeId, PubKey pubKey) {
        return findNetworkId(nodeId)
                .or(() -> createNetworkId(nodeId, pubKey)
                        .map(networkId -> {
                            persistableStore.getNetworkIdByNodeId().put(nodeId, networkId);
                            persist();
                            return networkId;
                        })
                );
    }
}
