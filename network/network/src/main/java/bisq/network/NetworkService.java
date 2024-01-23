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
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportType;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.HttpClientsByTransport;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.node.transport.BootstrapInfo;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.PubKey;
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

import static bisq.network.common.TransportType.TOR;
import static bisq.network.p2p.services.data.DataService.Listener;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService implements PersistenceClient<NetworkServiceStore>, Service {
    public static final ExecutorService NETWORK_IO_POOL = ExecutorFactory.newCachedThreadPool("NetworkService.network-IO-pool");
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    @Getter
    private final NetworkServiceStore persistableStore = new NetworkServiceStore();
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance
    @Getter
    private final Set<TransportType> supportedTransportTypes;
    @Getter
    private final Map<TransportType, Integer> defaultPortByTransportType;
    private final KeyBundleService keyBundleService;
    private final HttpClientsByTransport httpClientsByTransport;
    @Getter
    private final Optional<DataService> dataService;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    private final Optional<NetworkLoadService> monitorService;
    @Getter
    private final Persistence<NetworkServiceStore> persistence;
    @Getter
    private final Map<TransportType, CompletableFuture<Node>> initializedDefaultNodeByTransport = new HashMap<>();

    public NetworkService(NetworkServiceConfig config,
                          PersistenceService persistenceService,
                          KeyBundleService keyBundleService,
                          ProofOfWorkService proofOfWorkService) {
        socks5ProxyAddress = config.getSocks5ProxyAddress();
        supportedTransportTypes = config.getSupportedTransportTypes();
        defaultPortByTransportType = config.getDefaultPortByTransportType();
        this.keyBundleService = keyBundleService;
        NetworkEnvelope.setNetworkVersion(config.getVersion());

        httpClientsByTransport = new HttpClientsByTransport();

        Set<ServiceNode.SupportedService> supportedServices = config.getServiceNodeConfig().getSupportedServices();

        dataService = supportedServices.contains(ServiceNode.SupportedService.DATA) ?
                Optional.of(new DataService(persistenceService)) :
                Optional.empty();

        messageDeliveryStatusService = supportedServices.contains(ServiceNode.SupportedService.ACK) &&
                supportedServices.contains(ServiceNode.SupportedService.CONFIDENTIAL) ?
                Optional.of(new MessageDeliveryStatusService(persistenceService, keyBundleService, this)) :
                Optional.empty();

        NetworkLoadSnapshot networkLoadSnapshot = new NetworkLoadSnapshot();

        serviceNodesByTransport = new ServiceNodesByTransport(config.getConfigByTransportType(),
                config.getServiceNodeConfig(),
                config.getPeerGroupServiceConfigByTransport(),
                config.getSeedAddressesByTransport(),
                config.getInventoryServiceConfig(),
                supportedTransportTypes,
                config.getFeatures(),
                keyBundleService,
                persistenceService,
                proofOfWorkService,
                dataService,
                messageDeliveryStatusService,
                networkLoadSnapshot);

        monitorService = supportedServices.contains(ServiceNode.SupportedService.DATA) &&
                supportedServices.contains(ServiceNode.SupportedService.PEER_GROUP) &&
                supportedServices.contains(ServiceNode.SupportedService.MONITOR) ?
                Optional.of(new NetworkLoadService(serviceNodesByTransport, dataService.orElseThrow(), networkLoadSnapshot)) :
                Optional.empty();

        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.CACHE,
                persistableStore);
    }

    @Override
    public void onPersistedApplied(NetworkServiceStore persisted) {
        serviceNodesByTransport.addSeedNodes(persistableStore.getSeedNodes());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        NetworkId defaultNetworkId = getOrCreateDefaultNetworkId();
        initializedDefaultNodeByTransport.putAll(serviceNodesByTransport.getInitializedDefaultNodeByTransport(defaultNetworkId));

        // We use anyOf to complete as soon as we got at least one transport node initialized
        return CompletableFutureUtils.anyOf(initializedDefaultNodeByTransport.values())
                .thenApply(node -> {
                    if (node != null) {
                        messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::initialize);
                        monitorService.ifPresent(NetworkLoadService::initialize);
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::shutdown);
        monitorService.ifPresent(NetworkLoadService::shutdown);
        dataService.ifPresent(DataService::shutdown);
        return serviceNodesByTransport.shutdown()
                .thenApply(list -> list.stream().filter(e -> e).count() == supportedTransportTypes.size());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Initialize nodes or return initialized node
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns an initialized node for the given transport. If the node was not yet initialized we do the initialization,
     * otherwise the future returns the already initialized node.
     */
    public CompletableFuture<Node> supplyInitializedNode(TransportType transportType, NetworkId networkId) {
        return serviceNodesByTransport.supplyInitializedNode(transportType, networkId);
    }

    /**
     * Returns a future of the first initialized node on any transport
     */
    public CompletableFuture<Node> anySuppliedInitializedNode(NetworkId networkId) {
        return serviceNodesByTransport.anySuppliedInitializedNode(networkId);
    }

    /**
     * Returns a future of a list of all initialized nodes on all transports
     * A slow transport would delay the result. A failing transport would let the result future fail.
     * In most cases we do not want to be that strict.
     */
    public CompletableFuture<List<Node>> allSuppliedInitializedNode(NetworkId networkId) {
        return serviceNodesByTransport.allSuppliedInitializedNode(networkId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send confidential message
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Send message via given senderNetworkIdWithKeyPair to the receiverNetworkId as encrypted message.
     * If peer is offline and if message is of type mailBoxMessage it will not be stored as mailbox message in the
     * network.
     * We send if at least one node on any transport was initialized
     */
    public CompletableFuture<SendMessageResult> confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                                                 NetworkId receiverNetworkId,
                                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        return anySuppliedInitializedNode(senderNetworkIdWithKeyPair.getNetworkId())
                .thenCompose(networkId -> supplyAsync(() -> serviceNodesByTransport.confidentialSend(envelopePayloadMessage,
                                receiverNetworkId,
                                senderNetworkIdWithKeyPair.getKeyPair(),
                                senderNetworkIdWithKeyPair.getNetworkId()),
                        NETWORK_IO_POOL));
    }

    // TODO (low prio): Not used. Consider to remove it so it wont get used accidentally.

    /**
     * Send message via given senderNodeId to the supported network types of the addresses specified at
     * receiverAddressByNetworkType as direct, unencrypted message. If peer is offline it will not be stored as
     * mailbox message.
     */
    public CompletableFuture<Map<TransportType, Connection>> send(NetworkId senderNetworkId,
                                                                  EnvelopePayloadMessage envelopePayloadMessage,
                                                                  AddressByTransportTypeMap receiver) {
        return supplyAsync(() -> serviceNodesByTransport.send(senderNetworkId, envelopePayloadMessage, receiver),
                NETWORK_IO_POOL);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add/remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> publishAuthenticatedData(DistributedData distributedData, KeyPair keyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().addAuthenticatedData(authenticatedData, keyPair);
    }

    public CompletableFuture<BroadcastResult> removeAuthenticatedData(DistributedData distributedData, KeyPair ownerKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when removeData is called.");
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().removeAuthenticatedData(authenticatedData, ownerKeyPair);
    }

    public CompletableFuture<BroadcastResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                    KeyPair keyPair) {
        return publishAuthorizedData(authorizedDistributedData, keyPair, keyPair.getPrivate(), keyPair.getPublic());
    }

    public CompletableFuture<BroadcastResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                    KeyPair keyPair,
                                                                    PrivateKey authorizedPrivateKey,
                                                                    PublicKey authorizedPublicKey) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        try {
            byte[] signature = SignatureUtil.sign(authorizedDistributedData.serialize(), authorizedPrivateKey);
            AuthorizedData authorizedData = new AuthorizedData(authorizedDistributedData, Optional.of(signature), authorizedPublicKey);
            return dataService.get().addAuthorizedData(authorizedData, keyPair);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<BroadcastResult> removeAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                   KeyPair keyPair) {
        return removeAuthorizedData(authorizedDistributedData, keyPair, keyPair.getPublic());
    }

    public CompletableFuture<BroadcastResult> removeAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                   KeyPair ownerKeyPair,
                                                                   PublicKey authorizedPublicKey) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        // When removing data the signature is not used.
        AuthorizedData authorizedData = new AuthorizedData(authorizedDistributedData, authorizedPublicKey);
        return dataService.get().removeAuthorizedData(authorizedData, ownerKeyPair);
    }

    public CompletableFuture<BroadcastResult> publishAppendOnlyData(AppendOnlyData appendOnlyData) {
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

    public void addConfidentialMessageListener(ConfidentialMessageListener listener) {
        serviceNodesByTransport.addConfidentialMessageListener(listener);
    }

    public void removeConfidentialMessageListener(ConfidentialMessageListener listener) {
        serviceNodesByTransport.removeConfidentialMessageListener(listener);
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

    public BaseHttpClient getHttpClient(String url, String userAgent, TransportType transportType) {
        // socksProxy only supported for TOR
        Optional<Socks5Proxy> socksProxy = transportType == TOR ? serviceNodesByTransport.getSocksProxy() : Optional.empty();
        return httpClientsByTransport.getHttpClient(url, userAgent, transportType, socksProxy, socks5ProxyAddress);
    }

    public Map<TransportType, Observable<Node.State>> getNodeStateByTransportType() {
        return serviceNodesByTransport.getNodeStateByTransportType();
    }

    public Map<TransportType, BootstrapInfo> getBootstrapInfoByTransportType() {
        return serviceNodesByTransport.getBootstrapInfoByTransportType();
    }

    public boolean isTransportTypeSupported(TransportType transportType) {
        return supportedTransportTypes.contains(transportType);
    }

    public ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return messageDeliveryStatusService.map(MessageDeliveryStatusService::getMessageDeliveryStatusByMessageId)
                .orElse(new ObservableHashMap<>());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get optional
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<ServiceNode> findServiceNode(TransportType transport) {
        return serviceNodesByTransport.findServiceNode(transport);
    }

    public Optional<Node> findDefaultNode(TransportType transport) {
        return serviceNodesByTransport.findServiceNode(transport).map(ServiceNode::getDefaultNode);
    }

    public Optional<Node> findNode(TransportType transport, NetworkId networkId) {
        return serviceNodesByTransport.findNode(transport, networkId);
    }

    public Set<Node> findNodesOfAllTransports(NetworkId networkId) {
        return serviceNodesByTransport.findNodesOfAllTransports(networkId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add seed node from seed node bonded role
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSeedNodeAddressByTransport(AddressByTransportTypeMap seedNode) {
        serviceNodesByTransport.addSeedNode(seedNode);
        persistableStore.getSeedNodes().add(seedNode);
        persist();
    }

    public void removeSeedNodeAddressByTransport(AddressByTransportTypeMap seedNode) {
        serviceNodesByTransport.removeSeedNode(seedNode);
        persistableStore.getSeedNodes().remove(seedNode);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // NetworkId
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public NetworkId getOrCreateDefaultNetworkId() {
        // keyBundleService creates the defaultKeyBundle at initialize, and is called before we get initialized
        KeyBundle keyBundle = keyBundleService.findDefaultKeyBundle().orElseThrow();
        return getOrCreateNetworkId(keyBundle, "default");
    }

    public NetworkId getOrCreateNetworkId(KeyBundle keyBundle, String tag) {
        return persistableStore.findNetworkId(tag)
                .orElseGet(() -> createNetworkId(keyBundle, tag));
    }

    private NetworkId createNetworkId(KeyBundle keyBundle, String tag) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
        supportedTransportTypes.forEach(transportType -> {
            int port = getPortByTransport(tag, transportType);
            Address address = getAddressByTransport(keyBundle, port, transportType);
            addressByTransportTypeMap.put(transportType, address);
        });

        KeyPair keyPair = keyBundle.getKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyBundle.getKeyId());
        NetworkId networkId = new NetworkId(addressByTransportTypeMap, pubKey);
        persistableStore.getNetworkIdByTag().put(tag, networkId);
        persist();
        return networkId;
    }

    private int getPortByTransport(String tag, TransportType transportType) {
        boolean isDefault = tag.equals("default");
        switch (transportType) {
            case TOR:
                return isDefault ?
                        defaultPortByTransportType.computeIfAbsent(TransportType.TOR, key -> NetworkUtils.selectRandomPort()) :
                        NetworkUtils.selectRandomPort();
            case I2P:
                  /*  return isDefault ?
                            defaultPorts.computeIfAbsent(TransportType.I2P, key-> NetworkUtils.selectRandomPort()) :
                            NetworkUtils.selectRandomPort();*/
                throw new RuntimeException("I2P not unsupported yet");
            case CLEAR:
                return isDefault ?
                        defaultPortByTransportType.computeIfAbsent(TransportType.CLEAR, key -> NetworkUtils.findFreeSystemPort()) :
                        NetworkUtils.findFreeSystemPort();
        }
        throw new RuntimeException("getPortByTransport called with unsupported transportType " + transportType);
    }

    private Address getAddressByTransport(KeyBundle keyBundle, int port, TransportType transportType) {
        switch (transportType) {
            case TOR:
                return new Address(keyBundle.getTorKeyPair().getOnionAddress(), port);
            case I2P:
                //return new Address(keyBundle.getI2pKeyPair().getDestination(), port);
                throw new RuntimeException("I2P not unsupported yet");
            case CLEAR:
                return Address.localHost(port);
        }
        throw new RuntimeException("getAddressByTransport called with unsupported transportType " + transportType);
    }
}
