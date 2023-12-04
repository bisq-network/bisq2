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
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportType;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.HttpClientsByTransport;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoadService;
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
import bisq.network.p2p.services.monitor.MonitorService;
import bisq.network.p2p.vo.NetworkIdWithKeyPair;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.SignatureUtil;
import bisq.security.pow.ProofOfWorkService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static bisq.network.common.TransportType.TOR;
import static bisq.network.p2p.services.data.DataService.Listener;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService implements PersistenceClient<NetworkServiceStore>, Service {
    public static final String NETWORK_DB_PATH = "db" + File.separator + "network";
    public static final ExecutorService NETWORK_IO_POOL = ExecutorFactory.newCachedThreadPool("NetworkService.network-IO-pool");
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    @Getter
    private final NetworkServiceStore persistableStore = new NetworkServiceStore();
    @Getter
    private final Persistence<NetworkServiceStore> persistence;
    private final HttpClientsByTransport httpClientsByTransport;
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance
    @Getter
    private final Set<TransportType> supportedTransportTypes;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    @Getter
    private final Optional<DataService> dataService;
    private final Optional<MonitorService> monitorService;
    @Getter
    private final Map<TransportType, Integer> defaultNodePortByTransportType;

    public NetworkService(NetworkServiceConfig config,
                          PersistenceService persistenceService,
                          KeyPairService keyPairService,
                          ProofOfWorkService proofOfWorkService) {
        httpClientsByTransport = new HttpClientsByTransport();

        Set<ServiceNode.SupportedService> supportedServices = config.getServiceNodeConfig().getSupportedServices();

        dataService = supportedServices.contains(ServiceNode.SupportedService.DATA) ?
                Optional.of(new DataService(config.getInventoryServiceConfig(), persistenceService)) :
                Optional.empty();

        messageDeliveryStatusService = supportedServices.contains(ServiceNode.SupportedService.ACK) && supportedServices.contains(ServiceNode.SupportedService.CONFIDENTIAL) ?
                Optional.of(new MessageDeliveryStatusService(persistenceService, keyPairService, this)) :
                Optional.empty();

        NetworkLoadService networkLoadService = new NetworkLoadService();

        socks5ProxyAddress = config.getSocks5ProxyAddress();
        supportedTransportTypes = config.getSupportedTransportTypes();
        serviceNodesByTransport = new ServiceNodesByTransport(config.getConfigByTransportType(),
                supportedTransportTypes,
                config.getServiceNodeConfig(),
                config.getPeerGroupServiceConfigByTransport(),
                config.getSeedAddressesByTransport(),
                dataService,
                messageDeliveryStatusService,
                keyPairService,
                persistenceService,
                proofOfWorkService,
                networkLoadService);

        monitorService = supportedServices.contains(ServiceNode.SupportedService.DATA) &&
                supportedServices.contains(ServiceNode.SupportedService.PEER_GROUP) &&
                supportedServices.contains(ServiceNode.SupportedService.MONITOR) ?
                Optional.of(new MonitorService(serviceNodesByTransport, dataService.orElseThrow(), networkLoadService)) :
                Optional.empty();

        persistence = persistenceService.getOrCreatePersistence(this,
                NetworkService.NETWORK_DB_PATH,
                persistableStore.getClass().getSimpleName(),
                persistableStore);

        defaultNodePortByTransportType = config.getDefaultNodePortByTransportType();
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

        // Add persisted seed nodes to serviceNodesByTransport
        persistableStore.getSeedNodes().forEach(serviceNodesByTransport::addSeedNode);

        // We do not have the default node created yet.
        return serviceNodesByTransport.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::shutdown);
        monitorService.ifPresent(MonitorService::shutdown);
        return CompletableFutureUtils.allOf(
                        dataService.map(DataService::shutdown).orElse(completedFuture(true)),
                        serviceNodesByTransport.shutdown())
                .thenApply(list -> list.stream().filter(e -> e).count() == 3);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Initialize node
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void createDefaultServiceNodes(NetworkId defaultNetworkId, TorIdentity defaultTorIdentity) {
        serviceNodesByTransport.getMap()
                .forEach((transportType, serviceNode) -> serviceNode.createDefaultNode(defaultNetworkId, defaultTorIdentity));

        messageDeliveryStatusService.ifPresent(MessageDeliveryStatusService::initialize);
        monitorService.ifPresent(MonitorService::initialize);
    }

    public Map<TransportType, CompletableFuture<Node>> getInitializedNodeByTransport(NetworkId networkId, TorIdentity torIdentity) {
        return serviceNodesByTransport.getInitializedNodeByTransport(networkId, torIdentity);
    }

    public boolean isNodeOnAllTransportsInitialized(NetworkId networkId) {
        return serviceNodesByTransport.isNodeOnAllTransportsInitialized(networkId);
    }

    // TODO: This might be too restrictive as any failing transport would result in a failed completableFuture

    /**
     * NetworkId of a fully initialized node (on all transports)
     */
    public CompletableFuture<List<Node>> getAllInitializedNodes(NetworkId networkId, TorIdentity torIdentity) {
        return serviceNodesByTransport.getAllInitializedNodes(networkId, torIdentity);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Send confidential message
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Send message via given senderNetworkIdWithKeyPair to the receiverNetworkId as encrypted message.
     * If peer is offline and if message is of type mailBoxMessage it will not be stored as mailbox message in the
     * network.
     */
    public CompletableFuture<SendMessageResult> confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                                                 NetworkId receiverNetworkId,
                                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair,
                                                                 TorIdentity senderTorIdentity) {
        return getAllInitializedNodes(senderNetworkIdWithKeyPair.getNetworkId(), senderTorIdentity)
                .thenCompose(networkId -> supplyAsync(() -> serviceNodesByTransport.confidentialSend(envelopePayloadMessage,
                                receiverNetworkId,
                                senderNetworkIdWithKeyPair.getKeyPair(),
                                senderNetworkIdWithKeyPair.getNetworkId(),
                                senderTorIdentity),
                        NETWORK_IO_POOL));
    }

    // TODO: Not used. Consider to remove it so it wont get used accidentally.

    /**
     * Send message via given senderNodeId to the supported network types of the addresses specified at
     * receiverAddressByNetworkType as direct, unencrypted message. If peer is offline it will not be stored as
     * mailbox message.
     */
    public CompletableFuture<Map<TransportType, Connection>> send(NetworkId senderNetworkId,
                                                                  EnvelopePayloadMessage envelopePayloadMessage,
                                                                  AddressByTransportTypeMap receiver,
                                                                  TorIdentity senderTorIdentity) {
        return supplyAsync(() -> serviceNodesByTransport.send(senderNetworkId, envelopePayloadMessage, receiver, senderTorIdentity),
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

    public Optional<TorIdentity> findTorIdentity(NetworkId networkId) {
        // We get all nodes for all transports, but we only want to find the torIdentity which is the same at all nodes
        // for the given networkId, so we use Stream.findAny to get any node.
        return findNodesOfAllTransports(networkId).stream().findAny().map(Node::getTorIdentity);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add seed node address
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSeedNodeAddressByTransport(AddressByTransportTypeMap seedNodeAddressesByTransport) {
        serviceNodesByTransport.addSeedNode(seedNodeAddressesByTransport);
        persistableStore.getSeedNodes().add(seedNodeAddressesByTransport);
        persist();
    }

    public void removeSeedNodeAddressByTransport(AddressByTransportTypeMap seedNodeAddressesByTransport) {
        serviceNodesByTransport.removeSeedNode(seedNodeAddressesByTransport);
        persistableStore.getSeedNodes().remove(seedNodeAddressesByTransport);
        persist();
    }
}
