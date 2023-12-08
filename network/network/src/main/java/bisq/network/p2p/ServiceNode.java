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


import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.confidential.SendConfidentialMessageResult;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.data.DataNetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.PubKey;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Creates nodesById, the default node and the services according to the Config.
 */
@Slf4j
public class ServiceNode {
    @Getter
    public static final class Config {
        public static Config from(com.typesafe.config.Config config) {
            return new Config(new HashSet<>(config.getEnumList(SupportedService.class, "p2pServiceNode")));
        }

        private final Set<SupportedService> supportedServices;

        public Config(Set<SupportedService> supportedServices) {
            this.supportedServices = supportedServices;
        }
    }

    public interface Listener {
        void onStateChanged(ServiceNode.State state);
    }

    public enum State {
        NEW,

        INITIALIZING,
        INITIALIZED,

        STOPPING,
        TERMINATED
    }

    public enum SupportedService {
        PEER_GROUP,
        DATA,
        CONFIDENTIAL,
        ACK,
        MONITOR
    }

    private final Config config;
    private final PeerGroupManager.Config peerGroupServiceConfig;
    private final Optional<DataService> dataService;
    private final InventoryService.Config inventoryServiceConfig;
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    private final KeyBundleService keyBundleService;
    private final PersistenceService persistenceService;
    private final Set<Address> seedNodeAddresses;

    @Getter
    private final NodesById nodesById;
    @Getter
    private final TransportService transportService;
    private final BanList banList = new BanList();

    @Getter
    private Node defaultNode;
    @Getter
    private Optional<ConfidentialMessageService> confidentialMessageService = Optional.empty();
    @Getter
    private Optional<PeerGroupManager> peerGroupManager = Optional.empty();
    @Getter
    private Optional<InventoryService> inventoryService = Optional.empty();
    @Getter
    private Optional<DataNetworkService> dataNetworkService = Optional.empty();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    @Getter
    public Observable<State> state = new Observable<>(State.NEW);

    ServiceNode(Config config,
                Node.Config nodeConfig,
                PeerGroupManager.Config peerGroupServiceConfig,
                InventoryService.Config inventoryServiceConfig,
                Optional<DataService> dataService,
                Optional<MessageDeliveryStatusService> messageDeliveryStatusService,
                KeyBundleService keyBundleService,
                PersistenceService persistenceService,
                AuthorizationService authorizationService,
                Set<Address> seedNodeAddresses,
                TransportType transportType,
                NetworkLoadService networkLoadService) {
        this.config = config;
        this.peerGroupServiceConfig = peerGroupServiceConfig;
        this.inventoryServiceConfig = inventoryServiceConfig;
        this.messageDeliveryStatusService = messageDeliveryStatusService;
        this.dataService = dataService;
        this.keyBundleService = keyBundleService;
        this.persistenceService = persistenceService;
        this.seedNodeAddresses = seedNodeAddresses;

        transportService = TransportService.create(transportType, nodeConfig.getTransportConfig());
        nodesById = new NodesById(banList, nodeConfig, keyBundleService, transportService, networkLoadService, authorizationService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Node getInitializedDefaultNode(NetworkId defaultNetworkId, TorIdentity defaultTorIdentity) {
        defaultNode = nodesById.createAndConfigNode(defaultNetworkId, defaultTorIdentity, true);

        Set<SupportedService> supportedServices = config.getSupportedServices();
        peerGroupManager = supportedServices.contains(SupportedService.PEER_GROUP) ?
                Optional.of(new PeerGroupManager(persistenceService,
                        defaultNode,
                        banList,
                        peerGroupServiceConfig,
                        seedNodeAddresses)) :
                Optional.empty();

        boolean dataServiceEnabled = supportedServices.contains(SupportedService.PEER_GROUP) &&
                supportedServices.contains(SupportedService.DATA);

        dataNetworkService = dataServiceEnabled ?
                Optional.of(new DataNetworkService(defaultNode, peerGroupManager.orElseThrow(), dataService.orElseThrow())) :
                Optional.empty();

        inventoryService = dataServiceEnabled ?
                Optional.of(new InventoryService(inventoryServiceConfig,
                        defaultNode,
                        peerGroupManager.orElseThrow(),
                        dataService.orElseThrow())) :
                Optional.empty();

        confidentialMessageService = supportedServices.contains(SupportedService.CONFIDENTIAL) ?
                Optional.of(new ConfidentialMessageService(nodesById, keyBundleService, dataService, messageDeliveryStatusService)) :
                Optional.empty();

        setState(State.INITIALIZING);
        transportService.initialize();// blocking
        defaultNode.initialize();// blocking
        peerGroupManager.ifPresentOrElse(peerGroupService -> {
                    peerGroupService.initialize();// blocking
                    setState(State.INITIALIZED);
                },
                () -> setState(State.INITIALIZED));

        return defaultNode;
    }

    CompletableFuture<Boolean> shutdown() {
        setState(State.STOPPING);
        peerGroupManager.ifPresent(PeerGroupManager::shutdown);
        dataNetworkService.ifPresent(DataNetworkService::shutdown);
        inventoryService.ifPresent(InventoryService::shutdown);
        confidentialMessageService.ifPresent(ConfidentialMessageService::shutdown);
        return nodesById.shutdown()
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((result, throwable) -> throwable == null && result)
                .thenCompose(result -> transportService.shutdown())
                .whenComplete((result, throwable) -> setState(State.TERMINATED));
    }


    Node getInitializedNode(NetworkId networkId, TorIdentity torIdentity) {
        return nodesById.getInitializedNode(networkId, torIdentity);
    }

    boolean isNodeInitialized(NetworkId networkId) {
        return nodesById.isNodeInitialized(networkId);
    }

    void addSeedNodeAddresses(Set<Address> seedNodeAddresses) {
        this.seedNodeAddresses.addAll(seedNodeAddresses);
        peerGroupManager.ifPresent(peerGroupService -> peerGroupService.addSeedNodeAddresses(seedNodeAddresses));
    }

    void addSeedNodeAddress(Address seedNodeAddress) {
        // In case we would get called before peerGroupManager is created we add the seedNodeAddress to the
        // seedNodeAddresses field
        seedNodeAddresses.add(seedNodeAddress);
        peerGroupManager.ifPresent(peerGroupService -> peerGroupService.addSeedNodeAddress(seedNodeAddress));
    }

    void removeSeedNodeAddress(Address seedNodeAddress) {
        seedNodeAddresses.remove(seedNodeAddress);
        peerGroupManager.ifPresent(peerGroupService -> peerGroupService.removeSeedNodeAddress(seedNodeAddress));
    }

    SendConfidentialMessageResult confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                                   Address address,
                                                   PubKey receiverPubKey,
                                                   KeyPair senderKeyPair,
                                                   NetworkId senderNetworkId,
                                                   TorIdentity senderTorIdentity) {
        checkArgument(confidentialMessageService.isPresent(), "ConfidentialMessageService not present at confidentialSend");
        return confidentialMessageService.get().send(envelopePayloadMessage, address, receiverPubKey, senderKeyPair, senderNetworkId, senderTorIdentity);
    }

    Connection send(NetworkId senderNetworkId, EnvelopePayloadMessage envelopePayloadMessage, Address address, TorIdentity torIdentity) {
        return getNodesById().send(senderNetworkId, envelopePayloadMessage, address, torIdentity);
    }

    void addMessageListener(MessageListener messageListener) {
        //todo rename NodeListener

        nodesById.addNodeListener(new Node.Listener() {
            @Override
            public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
                messageListener.onMessage(envelopePayloadMessage);
            }

            @Override
            public void onConnection(Connection connection) {
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
            }
        });
        confidentialMessageService.ifPresent(service -> service.addMessageListener(messageListener));
    }

    void removeMessageListener(MessageListener messageListener) {
        //todo missing nodesById.removeNodeListener ?
        confidentialMessageService.ifPresent(service -> service.removeMessageListener(messageListener));
    }

    void addConfidentialMessageListener(ConfidentialMessageListener listener) {
        confidentialMessageService.ifPresent(service -> service.addConfidentialMessageListener(listener));
    }

    void removeConfidentialMessageListener(ConfidentialMessageListener listener) {
        confidentialMessageService.ifPresent(service -> service.removeConfidentialMessageListener(listener));
    }

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return defaultNode.getSocksProxy();
    }

    Optional<Node> findNode(NetworkId networkId) {
        return nodesById.findNode(networkId);
    }

    private void setState(State newState) {
        if (newState == state.get()) {
            return;
        }
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
        runAsync(() -> listeners.forEach(e -> e.onStateChanged(newState)), NetworkService.DISPATCHER);
    }
}
