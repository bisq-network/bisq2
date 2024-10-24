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
import bisq.common.platform.MemoryReportService;
import bisq.network.NetworkService;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.SendConfidentialMessageResult;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.network.p2p.services.data.DataNetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import bisq.network.p2p.services.reporting.Report;
import bisq.network.p2p.services.reporting.ReportRequestService;
import bisq.network.p2p.services.reporting.ReportResponseService;
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Creates nodesById, the default node and the services according to the Config.
 */
@Slf4j
public class ServiceNode implements Node.Listener {
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
        MONITOR,
        REPORT_REQUEST,
        REPORT_RESPONSE
    }

    private final Config config;
    private final Node.Config nodeConfig;
    private final PeerGroupManager.Config peerGroupServiceConfig;
    private final Optional<DataService> dataService;
    private final PeerGroupService peerGroupService;
    private final InventoryService.Config inventoryServiceConfig;
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    private final KeyBundleService keyBundleService;
    private final Set<Address> seedNodeAddresses;
    @Getter
    private final TransportType transportType;
    private final MemoryReportService memoryReportService;
    private final NetworkLoadSnapshot networkLoadSnapshot;

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
    private Optional<ReportRequestService> reportRequestService = Optional.empty();
    @Getter
    private Optional<ReportResponseService> reportResponseService = Optional.empty();
    @Getter
    private Optional<PeerGroupManager> peerGroupManager = Optional.empty();
    @Getter
    private Optional<InventoryService> inventoryService = Optional.empty();
    @Getter
    private Optional<DataNetworkService> dataNetworkService = Optional.empty();
    @Getter
    private Optional<NetworkLoadService> networkLoadService = Optional.empty();

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Set<ConfidentialMessageService.Listener> confidentialMessageListeners = new CopyOnWriteArraySet<>();

    @Getter
    public final Observable<State> state = new Observable<>(State.NEW);

    ServiceNode(Config config,
                Node.Config nodeConfig,
                PeerGroupManager.Config peerGroupServiceConfig,
                InventoryService.Config inventoryServiceConfig,
                KeyBundleService keyBundleService,
                PersistenceService persistenceService,
                Optional<DataService> dataService,
                Optional<MessageDeliveryStatusService> messageDeliveryStatusService,
                Optional<ResendMessageService> resendMessageService,
                AuthorizationService authorizationService,
                Set<Address> seedNodeAddresses,
                TransportType transportType,
                MemoryReportService memoryReportService) {
        this.config = config;
        this.nodeConfig = nodeConfig;
        this.peerGroupServiceConfig = peerGroupServiceConfig;
        this.inventoryServiceConfig = inventoryServiceConfig;
        this.keyBundleService = keyBundleService;
        this.dataService = dataService;
        this.messageDeliveryStatusService = messageDeliveryStatusService;
        this.seedNodeAddresses = seedNodeAddresses;
        this.transportType = transportType;
        this.memoryReportService = memoryReportService;

        this.networkLoadSnapshot = new NetworkLoadSnapshot();

        transportService = TransportService.create(transportType, nodeConfig.getTransportConfig());
        nodesById = new NodesById(banList, nodeConfig, keyBundleService, transportService, networkLoadSnapshot, authorizationService);
        peerGroupService = new PeerGroupService(persistenceService, transportType, peerGroupServiceConfig.getPeerGroupConfig(), seedNodeAddresses, banList);

        nodesById.addNodeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        confidentialMessageListeners.forEach(listener -> {
            try {
                listener.onMessage(envelopePayloadMessage);
            } catch (Exception e) {
                log.error("Calling onMessage at messageListener {} failed", listener, e);
            }
        });
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Node getInitializedDefaultNode(NetworkId defaultNetworkId) {
        defaultNode = nodesById.createAndConfigNode(defaultNetworkId, true);

        Set<SupportedService> supportedServices = config.getSupportedServices();
        peerGroupManager = supportedServices.contains(SupportedService.PEER_GROUP) ?
                Optional.of(new PeerGroupManager(defaultNode,
                        peerGroupService,
                        banList,
                        peerGroupServiceConfig)) :
                Optional.empty();

        boolean dataServiceEnabled = supportedServices.contains(SupportedService.PEER_GROUP) &&
                supportedServices.contains(SupportedService.DATA);

        dataNetworkService = dataServiceEnabled ?
                Optional.of(new DataNetworkService(defaultNode, dataService.orElseThrow())) :
                Optional.empty();

        inventoryService = dataServiceEnabled ?
                Optional.of(new InventoryService(inventoryServiceConfig,
                        defaultNode,
                        peerGroupManager.orElseThrow(),
                        dataService.orElseThrow(),
                        nodeConfig.getFeatures())) :
                Optional.empty();

        confidentialMessageService = supportedServices.contains(SupportedService.CONFIDENTIAL) ?
                Optional.of(new ConfidentialMessageService(nodesById,
                        keyBundleService,
                        dataService,
                        messageDeliveryStatusService)) :
                Optional.empty();

        reportRequestService = supportedServices.contains(ServiceNode.SupportedService.REPORT_REQUEST) ?
                Optional.of(new ReportRequestService(defaultNode)) :
                Optional.empty();
        reportResponseService = supportedServices.contains(ServiceNode.SupportedService.DATA) &&
                supportedServices.contains(ServiceNode.SupportedService.PEER_GROUP) &&
                supportedServices.contains(ServiceNode.SupportedService.MONITOR) &&
                supportedServices.contains(ServiceNode.SupportedService.REPORT_RESPONSE) ?
                Optional.of(new ReportResponseService(defaultNode,
                        dataService.orElseThrow(),
                        networkLoadSnapshot,
                        memoryReportService)) :
                Optional.empty();

        networkLoadService = supportedServices.contains(ServiceNode.SupportedService.DATA) &&
                supportedServices.contains(ServiceNode.SupportedService.PEER_GROUP) &&
                supportedServices.contains(ServiceNode.SupportedService.MONITOR) ?
                Optional.of(new NetworkLoadService(this,
                        dataService.orElseThrow().getStorageService(),
                        networkLoadSnapshot,
                        peerGroupServiceConfig.getPeerGroupConfig().getMaxNumConnectedPeers())) :
                Optional.empty();

        setState(State.INITIALIZING);
        transportService.initialize();// blocking
        defaultNode.initialize();// blocking
        peerGroupManager.ifPresentOrElse(peerGroupManager -> {
                    peerGroupManager.initialize();// blocking
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
                .thenCompose(result -> transportService.shutdown())
                .whenComplete((result, throwable) -> setState(State.TERMINATED));
    }


    Node initializeNode(NetworkId networkId) {
        return nodesById.initializeNode(networkId);
    }

    boolean isNodeInitialized(NetworkId networkId) {
        return nodesById.isNodeInitialized(networkId);
    }

    void addSeedNodeAddresses(Set<Address> seedNodeAddresses) {
        this.seedNodeAddresses.addAll(seedNodeAddresses);
        peerGroupManager.ifPresent(peerGroupManager -> peerGroupManager.addSeedNodeAddresses(seedNodeAddresses));
    }

    void addSeedNodeAddress(Address seedNodeAddress) {
        // In case we would get called before peerGroupManager is created we add the seedNodeAddress to the
        // seedNodeAddresses field
        seedNodeAddresses.add(seedNodeAddress);
        peerGroupManager.ifPresent(peerGroupManager -> peerGroupManager.addSeedNodeAddress(seedNodeAddress));
    }

    void removeSeedNodeAddress(Address seedNodeAddress) {
        seedNodeAddresses.remove(seedNodeAddress);
        peerGroupManager.ifPresent(peerGroupManager -> peerGroupManager.removeSeedNodeAddress(seedNodeAddress));
    }

    SendConfidentialMessageResult confidentialSend(EnvelopePayloadMessage envelopePayloadMessage,
                                                   NetworkId receiverNetworkId,
                                                   Address address,
                                                   PubKey receiverPubKey,
                                                   KeyPair senderKeyPair,
                                                   NetworkId senderNetworkId) {
        checkArgument(confidentialMessageService.isPresent(), "ConfidentialMessageService not present at confidentialSend");
        return confidentialMessageService.get().send(envelopePayloadMessage, receiverNetworkId, address, receiverPubKey, senderKeyPair, senderNetworkId);
    }

    Connection send(NetworkId senderNetworkId, EnvelopePayloadMessage envelopePayloadMessage, Address address) {
        return nodesById.send(senderNetworkId, envelopePayloadMessage, address);
    }

    void addConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        confidentialMessageListeners.add(listener);
        confidentialMessageService.ifPresent(service -> service.addListener(listener));
    }

    void removeConfidentialMessageListener(ConfidentialMessageService.Listener listener) {
        confidentialMessageListeners.remove(listener);
        confidentialMessageService.ifPresent(service -> service.removeListener(listener));
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

    boolean isPeerOnline(NetworkId networkId, Address address) {
        return nodesById.isPeerOnline(networkId, address);
    }

    private void setState(State newState) {
        if (newState == state.get()) {
            return;
        }
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
        runAsync(() -> listeners.forEach(listener -> {
            try {
                listener.onStateChanged(newState);
            } catch (Exception e) {
                log.error("Calling onMessage at onStateChanged {} failed", listener, e);
            }
        }), NetworkService.DISPATCHER);
    }

    CompletableFuture<Report> requestReport(Address address) {
        checkArgument(reportRequestService.isPresent(), "ReportRequestService not present at requestReport");
        return reportRequestService.get().request(address);
    }
}
