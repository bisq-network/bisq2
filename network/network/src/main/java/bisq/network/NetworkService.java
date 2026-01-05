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
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.platform.MemoryReportService;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.HttpClientsByTransport;
import bisq.network.http.utils.ReferenceTimeService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.confidential.resend.ResendMessageData;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.reporting.Report;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static bisq.common.network.TransportType.TOR;
import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static bisq.network.p2p.services.data.DataService.Listener;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService extends RateLimitedPersistenceClient<NetworkServiceStore> implements Service {
    @Getter
    private final NetworkServiceStore persistableStore = new NetworkServiceStore();
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance
    @Getter
    private final Set<TransportType> supportedTransportTypes;
    @Getter
    private final Map<TransportType, Integer> defaultPortByTransportType;
    private final NetworkServiceConfig config;
    @Getter
    private final NetworkIdService networkIdService;
    private final HttpClientsByTransport httpClientsByTransport;
    @Getter
    private final Optional<DataService> dataService;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;
    @Getter
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    @Getter
    private final Optional<ResendMessageService> resendMessageService;
    private final ReferenceTimeService referenceTimeService;
    @Getter
    private final Persistence<NetworkServiceStore> persistence;
    @Getter
    private final Map<TransportType, CompletableFuture<Node>> initializedDefaultNodeByTransport = new HashMap<>();
    @Getter
    private final Map<TransportType, Set<Address>> seedAddressesByTransportFromConfig;
    private Set<Pin> transportStatePins = new HashSet<>();
    @Getter
    private final Observable<Long> referenceTime = new Observable<>();

    public NetworkService(NetworkServiceConfig config,
                          PersistenceService persistenceService,
                          KeyBundleService keyBundleService,
                          HashCashProofOfWorkService hashCashProofOfWorkService,
                          EquihashProofOfWorkService equihashProofOfWorkService,
                          MemoryReportService memoryReportService) {
        socks5ProxyAddress = config.getSocks5ProxyAddress();
        supportedTransportTypes = config.getSupportedTransportTypes();
        defaultPortByTransportType = config.getDefaultPortByTransportType();
        this.config = config;
        NetworkEnvelope.setNetworkVersion(config.getVersion());

        networkIdService = new NetworkIdService(persistenceService, keyBundleService, supportedTransportTypes, defaultPortByTransportType);
        Map<TransportType, TransportConfig> configByTransportType = config.getConfigByTransportType();
        httpClientsByTransport = new HttpClientsByTransport(configByTransportType);

        Set<ServiceNode.SupportedService> supportedServices = config.getServiceNodeConfig().getSupportedServices();

        boolean isDataServiceSupported = supportedServices.contains(ServiceNode.SupportedService.DATA);
        dataService = isDataServiceSupported ?
                Optional.of(new DataService(persistenceService)) :
                Optional.empty();

        messageDeliveryStatusService = supportedServices.contains(ServiceNode.SupportedService.ACK) &&
                supportedServices.contains(ServiceNode.SupportedService.CONFIDENTIAL) ?
                Optional.of(new MessageDeliveryStatusService(persistenceService, keyBundleService, this)) :
                Optional.empty();
        resendMessageService = supportedServices.contains(ServiceNode.SupportedService.ACK) &&
                supportedServices.contains(ServiceNode.SupportedService.CONFIDENTIAL) ?
                Optional.of(new ResendMessageService(persistenceService, this, messageDeliveryStatusService.orElseThrow())) :
                Optional.empty();


        seedAddressesByTransportFromConfig = config.getSeedAddressesByTransport();
        serviceNodesByTransport = new ServiceNodesByTransport(configByTransportType,
                config.getServiceNodeConfig(),
                config.getPeerGroupManagerConfigByTransport(),
                seedAddressesByTransportFromConfig,
                config.getInventoryServiceConfig(),
                config.getAuthorizationServiceConfig(),
                supportedTransportTypes,
                config.getFeatures(),
                keyBundleService,
                persistenceService,
                hashCashProofOfWorkService,
                equihashProofOfWorkService,
                dataService,
                messageDeliveryStatusService,
                resendMessageService,
                memoryReportService);
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.CACHE, persistableStore);

        referenceTimeService = new ReferenceTimeService(ReferenceTimeService.Config.from(config.getReferenceTimeService()), this);
    }

    @Override
    public void onPersistedApplied(NetworkServiceStore persisted) {
        serviceNodesByTransport.addSeedNodes(persistableStore.getSeedNodes());
        //noinspection deprecation
        Map<String, NetworkId> networkIdByTag = persisted.getNetworkIdByTag();
        if (!networkIdByTag.isEmpty()) {
            networkIdService.migrateFromDeprecatedStore(networkIdByTag);
        }
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        NetworkExecutors.initialize(config.getNotifyExecutorMaxPoolSize());
        Connection.setExecutorMaxPoolSize(config.getConnectionExecutorMaxPoolSize());

        NetworkId defaultNetworkId = networkIdService.getOrCreateDefaultNetworkId();

        requestReferenceTime();

        Map<TransportType, CompletableFuture<Node>> map = serviceNodesByTransport.getInitializedDefaultNodeByTransport(defaultNetworkId);
        initializedDefaultNodeByTransport.putAll(map);

        // We use anyOf to complete as soon as we got at least one transport node initialized
        return CompletableFutureUtils.anyOf(initializedDefaultNodeByTransport.values())
                .thenApply(node -> {
                    if (node != null) {
                        messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::initialize);
                        resendMessageService.ifPresent(ResendMessageService::initialize);
                        return true;
                    } else {
                        return false;
                    }
                })
                // We complete on a network thread and switch to ExecutorFactory.commonForkJoinPool() to prevent network threads
                // from being tied up during subsequent service initialization steps.
                .thenComposeAsync(CompletableFuture::completedFuture, commonForkJoinPool());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");

        transportStatePins.forEach(Pin::unbind);
        transportStatePins.clear();

        return CompletableFuture.supplyAsync(() -> {
                    messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::shutdown);
                    resendMessageService.ifPresent(ResendMessageService::shutdown);
                    dataService.ifPresent(DataService::shutdown);
                    return true;
                }, commonForkJoinPool())
                .thenCompose(result -> referenceTimeService.shutdown())
                .thenCompose(result -> serviceNodesByTransport.shutdown()
                        .thenApply(list -> list.stream().filter(e -> e).count() == supportedTransportTypes.size()))
                .whenComplete((r, t) -> NetworkExecutors.shutdown());
    }


    /* --------------------------------------------------------------------- */
    // Initialize nodes or return initialized node
    /* --------------------------------------------------------------------- */

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


    /* --------------------------------------------------------------------- */
    // Send confidential message
    /* --------------------------------------------------------------------- */

    /**
     * Send message via given senderNetworkIdWithKeyPair to the receiverNetworkId as encrypted message.
     * If peer is offline and if message is of type mailBoxMessage it will be stored as mailbox message in the
     * network.
     */
    public CompletableFuture<SendMessageResult> confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                                                 NetworkId receiverNetworkId,
                                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        KeyPair senderKeyPair = senderNetworkIdWithKeyPair.getKeyPair();
        NetworkId senderNetworkId = senderNetworkIdWithKeyPair.getNetworkId();
        // Before sending, we might need to establish a new connection to the peer. As that could take soe time,
        // we apply here already the CONNECTING MessageDeliveryStatus so that the UI can give visual feedback about
        // the state.
        if (envelopePayloadMessage instanceof AckRequestingMessage ackRequestingMessage) {
            resendMessageService.ifPresent(resendService ->
                    resendService.registerResendMessageData(new ResendMessageData(ackRequestingMessage,
                            receiverNetworkId,
                            senderKeyPair,
                            senderNetworkId,
                            MessageDeliveryStatus.CONNECTING,
                            System.currentTimeMillis())));
        }

        return anySuppliedInitializedNode(senderNetworkId) // Runs in NetworkNodeExecutor
                .thenApply(networkId ->
                        serviceNodesByTransport.confidentialSend(envelopePayloadMessage,
                                receiverNetworkId,
                                senderKeyPair,
                                senderNetworkId))
                .orTimeout(2, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable instanceof TimeoutException) {
                        log.warn("TimeoutException at confidentialSend, likely caused by the anySuppliedInitializedNode() method " +
                                "as the node for the given networkId is not initialized yet. " +
                                "We call serviceNodesByTransport.confidentialSend() to send the message as mailbox message.");
                        serviceNodesByTransport.confidentialSend(envelopePayloadMessage,
                                receiverNetworkId,
                                senderKeyPair,
                                senderNetworkId);
                    }
                });
    }


    /* --------------------------------------------------------------------- */
    // AuthenticatedData
    /* --------------------------------------------------------------------- */

    public CompletableFuture<BroadcastResult> publishAuthenticatedData(DistributedData distributedData,
                                                                       KeyPair keyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when publishAuthenticatedData is called.");
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().addAuthenticatedData(authenticatedData, keyPair);
    }

    public CompletableFuture<BroadcastResult> refreshAuthenticatedData(DistributedData distributedData,
                                                                       KeyPair keyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when refreshAuthenticatedData is called.");
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().refreshAuthenticatedData(authenticatedData, keyPair);
    }

    public CompletableFuture<BroadcastResult> removeAuthenticatedData(DistributedData distributedData,
                                                                      KeyPair ownerKeyPair) {
        checkArgument(dataService.isPresent(), "DataService must be supported when removeAuthenticatedData is called.");
        DefaultAuthenticatedData authenticatedData = new DefaultAuthenticatedData(distributedData);
        return dataService.get().removeAuthenticatedData(authenticatedData, ownerKeyPair);
    }

    public Optional<Long> findCreationDate(DistributedData distributedData, Predicate<DistributedData> predicate) {
        return dataService.flatMap(dataService -> dataService.getStorageService().getOrCreateAuthenticatedDataStore(distributedData.getClassName()).join()
                .getPersistableStore().getMap().values().stream()
                .filter(AddAuthenticatedDataRequest.class::isInstance)
                .map(AddAuthenticatedDataRequest.class::cast)
                .map(AddAuthenticatedDataRequest::getAuthenticatedSequentialData)
                .filter(sequentialData -> predicate.test(sequentialData.getAuthenticatedData().getDistributedData()))
                .map(AuthenticatedSequentialData::getCreated)
                .findAny());
    }


    /* --------------------------------------------------------------------- */
    // AuthorizedData
    /* --------------------------------------------------------------------- */

    public CompletableFuture<BroadcastResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                    KeyPair keyPair) {
        return publishAuthorizedData(authorizedDistributedData, keyPair, keyPair.getPrivate(), keyPair.getPublic());
    }

    public CompletableFuture<BroadcastResult> publishAuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                                                                    KeyPair keyPair,
                                                                    PrivateKey authorizedPrivateKey,
                                                                    PublicKey authorizedPublicKey) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        log.info("Publish authorizedData: {}", authorizedDistributedData.getClassName());
        try {
            byte[] signature = SignatureUtil.sign(authorizedDistributedData.serializeForHash(), authorizedPrivateKey);
            AuthorizedData authorizedData = new AuthorizedData(authorizedDistributedData, Optional.of(signature), authorizedPublicKey);
            return dataService.get().addAuthorizedData(authorizedData, keyPair);
        } catch (GeneralSecurityException e) {
            log.error("publishAuthorizedData failed", e);
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


    /* --------------------------------------------------------------------- */
    // AppendOnlyData
    /* --------------------------------------------------------------------- */

    public CompletableFuture<BroadcastResult> publishAppendOnlyData(AppendOnlyData appendOnlyData) {
        checkArgument(dataService.isPresent(), "DataService must be supported when addData is called.");
        return dataService.get().addAppendOnlyData(appendOnlyData);
    }


    /* --------------------------------------------------------------------- */
    // Listeners
    /* --------------------------------------------------------------------- */

    public void addDataServiceListener(Listener listener) {
        dataService.orElseThrow().addListener(listener);
    }

    public void removeDataServiceListener(Listener listener) {
        dataService.orElseThrow().removeListener(listener);
    }

    public void addConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        serviceNodesByTransport.addConfidentialMessageListener(listener);
    }

    public void removeConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        serviceNodesByTransport.removeConfidentialMessageListener(listener);
    }

    public void addDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.addDefaultNodeListener(nodeListener);
    }

    public void removeDefaultNodeListener(Node.Listener nodeListener) {
        serviceNodesByTransport.removeDefaultNodeListener(nodeListener);
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    public BaseHttpClient getHttpClient(String url, String userAgent, TransportType transportType) {
        if (Objects.requireNonNull(transportType) == TransportType.TOR) {
            return httpClientsByTransport.getHttpClient(url, userAgent, transportType, serviceNodesByTransport.getSocksProxy(transportType), socks5ProxyAddress);
        }
        return httpClientsByTransport.getHttpClient(url, userAgent, transportType);
    }

    public Map<TransportType, Observable<Node.State>> getDefaultNodeStateByTransportType() {
        return serviceNodesByTransport.getDefaultNodeStateByTransportType();
    }

    public boolean isTransportTypeSupported(TransportType transportType) {
        return supportedTransportTypes.contains(transportType);
    }

    public ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return messageDeliveryStatusService.map(MessageDeliveryStatusService::getMessageDeliveryStatusByMessageId)
                .orElseGet(ObservableHashMap::new);
    }

    public Set<NetworkLoadService> getNetworkLoadServices() {
        return serviceNodesByTransport.getAllServiceNodes().stream()
                .flatMap(serviceNode -> serviceNode.getNetworkLoadService().stream())
                .collect(Collectors.toSet());
    }

    public int getNumConnectionsOnAllTransports() {
        return serviceNodesByTransport.getAllServiceNodes().stream()
                .map(ServiceNode::getDefaultNode)
                .mapToInt(Node::getNumConnections)
                .sum();
    }


    /* --------------------------------------------------------------------- */
    // Get optional
    /* --------------------------------------------------------------------- */

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


    /* --------------------------------------------------------------------- */
    // Add seed node from seed node bonded role
    /* --------------------------------------------------------------------- */

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


    /* --------------------------------------------------------------------- */
    // Check peer's online state (In case of Tor it checks if the onion service is published)
    /* --------------------------------------------------------------------- */

    public Map<TransportType, CompletableFuture<Boolean>> isPeerOnline(NetworkId networkId,
                                                                       AddressByTransportTypeMap peer) {
        return serviceNodesByTransport.isPeerOnlineAsync(networkId, peer);
    }


    /* --------------------------------------------------------------------- */
    // Expose pending ResendMessageData and ConfidentialMessageService for higher level services
    /* --------------------------------------------------------------------- */

    public Set<ResendMessageData> getPendingResendMessageDataSet() {
        return resendMessageService.map(ResendMessageService::getPendingResendMessageDataSet).orElseGet(HashSet::new);
    }

    public Set<ConfidentialMessageService> getConfidentialMessageServices() {
        return serviceNodesByTransport.getAllServiceNodes().stream()
                .filter(serviceNode -> serviceNode.getConfidentialMessageService().isPresent())
                .map(serviceNode -> serviceNode.getConfidentialMessageService().get())
                .collect(Collectors.toSet());
    }


    /* --------------------------------------------------------------------- */
    // Report
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Report> requestReport(Address address) {
        return serviceNodesByTransport.requestReport(address);
    }


    /* --------------------------------------------------------------------- */
    // Publish onion service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        return serviceNodesByTransport.findServiceNode(TOR)
                .map(ServiceNode::getTransportService)
                .map(TorTransportService.class::cast)
                .map(torTransportService -> torTransportService.publishOnionService(localPort, onionServicePort, torKeyPair))
                .orElse(CompletableFuture.failedFuture(new UnsupportedOperationException("Calling publishOnionService requires to have a TorTransportService running.")));
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void requestReferenceTime() {
        transportStatePins.forEach(Pin::unbind);
        transportStatePins.clear();
        Collection<ServiceNode> allServiceNodes = serviceNodesByTransport.getAllServiceNodes();
        transportStatePins.addAll(allServiceNodes.stream()
                .map(serviceNode -> {
                    TransportService transportService = serviceNode.getTransportService();
                    return transportService.getTransportState().addObserver(state -> {
                        // Once transport service is initialized we start requesting
                        if (TransportService.TransportState.INITIALIZED == state) {
                            try {
                                referenceTimeService.request()
                                        .whenComplete((time, throwable) -> {
                                            if (throwable == null) {
                                                referenceTime.set(time);
                                            } else {
                                                log.warn("ReferenceTimeService request triggered during initialization of transport service {} failed.",
                                                        transportService.getClass().getSimpleName(), throwable);
                                            }
                                        });
                            } catch (Exception e) {
                                log.warn("ReferenceTimeService request triggered during initialization of transport service {} failed.",
                                        transportService.getClass().getSimpleName(), e);
                            }
                        }
                    });
                })
                .collect(Collectors.toSet()));
    }
}
