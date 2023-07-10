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
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataNetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.monitor.MonitorService;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Creates nodesById, the default node and the services according to the Config.
 */
@Slf4j
public class ServiceNode {

    @Getter
    public static final class Config {
        private final Set<Service> services;

        public Config(Set<Service> services) {
            this.services = services;
        }
    }

    public interface Listener {
        void onStateChanged(ServiceNode.State state);
    }

    public enum State {
        NEW,
        INITIALIZE_PEER_GROUP,
        PEER_GROUP_INITIALIZED,
        STOPPING,
        TERMINATED
    }

    public enum Service {
        PEER_GROUP,
        DATA,
        CONFIDENTIAL,
        MONITOR
    }

    @Getter
    private final NodesById nodesById;
    @Getter
    private final Node defaultNode;
    @Getter
    private Optional<ConfidentialMessageService> confidentialMessageService;
    @Getter
    private Optional<PeerGroupService> peerGroupService;
    @Getter
    private Optional<DataNetworkService> dataServicePerTransport;

    @Getter
    private Optional<MonitorService> monitorService;
    @Getter
    public Observable<State> state = new Observable<>(State.NEW);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ServiceNode(Config config,
                       Node.Config nodeConfig,
                       PeerGroupService.Config peerGroupServiceConfig,
                       Optional<DataService> dataService,
                       KeyPairService keyPairService,
                       PersistenceService persistenceService,
                       Set<Address> seedNodeAddresses,
                       Transport.Type transportType) {
        BanList banList = new BanList();
        nodesById = new NodesById(banList, nodeConfig);
        defaultNode = nodesById.getOrCreateDefaultNode();
        Set<Service> services = config.getServices();

        if (services.contains(Service.PEER_GROUP)) {
            PeerGroupService peerGroupService = new PeerGroupService(persistenceService,
                    defaultNode,
                    banList,
                    peerGroupServiceConfig,
                    seedNodeAddresses,
                    transportType);
            this.peerGroupService = Optional.of(peerGroupService);

            if (services.contains(Service.DATA)) {
                dataServicePerTransport = Optional.of(dataService.orElseThrow().getDataServicePerTransport(nodeConfig.getTransportType(),
                        defaultNode,
                        peerGroupService));
            }

            if (services.contains(Service.MONITOR)) {
                monitorService = Optional.of(new MonitorService(defaultNode,
                        peerGroupService.getPeerGroup(),
                        peerGroupService.getPeerGroupStore()));
            }
        }

        if (services.contains(Service.CONFIDENTIAL)) {
            confidentialMessageService = Optional.of(new ConfidentialMessageService(nodesById, keyPairService, dataService));
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        setState(State.STOPPING);
        return CompletableFutureUtils.allOf(
                        confidentialMessageService.map(ConfidentialMessageService::shutdown).orElse(completedFuture(true)),
                        peerGroupService.map(PeerGroupService::shutdown).orElse(completedFuture(true)),
                        dataServicePerTransport.map(DataNetworkService::shutdown).orElse(completedFuture(true)),
                        monitorService.map(MonitorService::shutdown).orElse(completedFuture(true)),
                        nodesById.shutdown()
                )
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((list, throwable) -> {
                    setState(State.TERMINATED);
                    return throwable == null && list.stream().allMatch(e -> e);
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void initializeNode(String nodeId, int serverPort) {
        nodesById.initialize(nodeId, serverPort);
    }

    public boolean isNodeInitialized(String nodeId) {
        return nodesById.isNodeInitialized(nodeId);
    }

    public void initializePeerGroup() {
        peerGroupService.ifPresent(PeerGroupService::initialize);
    }

    public void addSeedNodeAddress(Address seedNodeAddress) {
        peerGroupService.ifPresent(peerGroupService -> peerGroupService.addSeedNodeAddress(seedNodeAddress));
    }

    public void removeSeedNodeAddress(Address seedNodeAddress) {
        peerGroupService.ifPresent(peerGroupService -> peerGroupService.removeSeedNodeAddress(seedNodeAddress));
    }

    public ConfidentialMessageService.Result confidentialSend(NetworkMessage networkMessage,
                                                              Address address,
                                                              PubKey receiverPubKey,
                                                              KeyPair senderKeyPair,
                                                              String senderNodeId) {
        return confidentialMessageService.map(service -> service.send(networkMessage, address, receiverPubKey, senderKeyPair, senderNodeId))
                .orElseThrow(() -> new RuntimeException("ConfidentialMessageService not present at confidentialSend"));
    }

    public Connection send(String senderNodeId, NetworkMessage networkMessage, Address address) {
        return getNodesById().send(senderNodeId, networkMessage, address);
    }

    public void addMessageListener(MessageListener messageListener) {
        //todo
        nodesById.addNodeListener(new Node.Listener() {
            @Override
            public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
                messageListener.onMessage(networkMessage);
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

    public void removeMessageListener(MessageListener messageListener) {
        confidentialMessageService.ifPresent(service -> service.removeMessageListener(messageListener));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return defaultNode.getSocksProxy();
    }

    public Map<String, Address> getAddressesByNodeId() {
        return nodesById.getAddressesByNodeId();
    }

    public Optional<Node> findNode(String nodeId) {
        return nodesById.findNode(nodeId);
    }

    public Optional<Address> findMyAddress(String nodeId) {
        return nodesById.findMyAddress(nodeId);
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
