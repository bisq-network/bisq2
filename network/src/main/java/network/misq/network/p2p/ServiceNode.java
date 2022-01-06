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
import lombok.Getter;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.NodesById;
import network.misq.network.p2p.services.confidential.ConfidentialMessageService;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.DataServicePerTransport;
import network.misq.network.p2p.services.data.broadcast.BroadcastResult;
import network.misq.network.p2p.services.data.filter.DataFilter;
import network.misq.network.p2p.services.data.inventory.RequestInventoryResult;
import network.misq.network.p2p.services.monitor.MonitorService;
import network.misq.network.p2p.services.peergroup.BanList;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.relay.RelayService;
import network.misq.persistence.PersistenceService;
import network.misq.security.KeyPairService;
import network.misq.security.PubKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Creates nodeRepository and a default node
 * Creates services according to services defined in Config
 */
public class ServiceNode {
    private static final Logger log = LoggerFactory.getLogger(ServiceNode.class);

    public static record Config(Set<Service> services) {
    }

    public enum State {
        CREATED,
        INITIALIZE_DEFAULT_NODE_SERVER,
        DEFAULT_NODE_SERVER_INITIALIZED,
        INITIALIZE_PEER_GROUP,
        PEER_GROUP_INITIALIZED,
        SHUTDOWN_STARTED,
        SHUTDOWN_COMPLETE
    }

    public enum Service {
        PEER_GROUP,
        DATA,
        CONFIDENTIAL,
        RELAY,
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
    private Optional<DataServicePerTransport> dataServicePerTransport;

    private Optional<RelayService> relayService;
    @Getter
    private Optional<MonitorService> monitorService;
    @Getter
    public AtomicReference<State> state = new AtomicReference<>(State.CREATED);

    public ServiceNode(Config config,
                       Node.Config nodeConfig,
                       PeerGroupService.Config peerGroupServiceConfig,
                       Optional<DataService> dataService,
                       KeyPairService keyPairService,
                       PersistenceService persistenceService,
                       List<Address> seedNodeAddresses) {
        BanList banList = new BanList();
        nodesById = new NodesById(banList, nodeConfig);
        defaultNode = nodesById.getDefaultNode();
        Set<Service> services = config.services();

        if (services.contains(Service.PEER_GROUP)) {
            PeerGroupService peerGroupService = new PeerGroupService(persistenceService, defaultNode, banList, peerGroupServiceConfig, seedNodeAddresses);
            this.peerGroupService = Optional.of(peerGroupService);

            if (services.contains(Service.DATA)) {
                dataServicePerTransport = Optional.of(new DataServicePerTransport(defaultNode, peerGroupService, keyPairService));
            }

            if (services.contains(Service.RELAY)) {
                relayService = Optional.of(new RelayService(defaultNode));
            }

            if (services.contains(Service.MONITOR)) {
                monitorService = Optional.of(new MonitorService(defaultNode, peerGroupService));
            }
        }

        if (services.contains(Service.CONFIDENTIAL)) {
            confidentialMessageService = Optional.of(new ConfidentialMessageService(nodesById, keyPairService, dataService));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean maybeInitializeServer(String nodeId, int serverPort) {
        if (nodeId.equals(Node.DEFAULT_NODE_ID)) {
            setState(State.INITIALIZE_DEFAULT_NODE_SERVER);
        }
        try {
            nodesById.maybeInitializeServer(nodeId, serverPort);
            if (nodeId.equals(Node.DEFAULT_NODE_ID)) {
                setState(State.DEFAULT_NODE_SERVER_INITIALIZED);
            }
            return true;
        } catch (Throwable throwable) {
            log.error("maybeInitializeServer failed. nodeId=" + nodeId + ";serverPort=" + serverPort, throwable);
            return false;
        }
    }

    public CompletableFuture<Void> maybeInitializePeerGroupAsync() {
        return runAsync(this::maybeInitializePeerGroup, NetworkService.NETWORK_IO_POOL);
    }

    public void maybeInitializePeerGroup() {
        if (state.get() == State.PEER_GROUP_INITIALIZED) {
            log.debug("We had the peer group already initialized and ignore that call.");
            return;
        }
        if (state.get() == State.INITIALIZE_PEER_GROUP) {
            log.debug("We had the peer group already initialized but initialization is not completed yet.");
            try {
                Thread.sleep(1000);
                maybeInitializePeerGroup();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setState(State.INITIALIZE_PEER_GROUP);
        peerGroupService.ifPresent(PeerGroupService::initialize);
        setState(State.PEER_GROUP_INITIALIZED);
    }

    public CompletableFuture<Void> shutdown() {
        setState(State.SHUTDOWN_STARTED);
        return CompletableFutureUtils.allOf(nodesById.shutdown(),
                        confidentialMessageService.map(ConfidentialMessageService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        peerGroupService.map(PeerGroupService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        dataServicePerTransport.map(DataServicePerTransport::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        relayService.map(RelayService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        monitorService.map(MonitorService::shutdown).orElse(CompletableFuture.completedFuture(null)))
                .orTimeout(4, TimeUnit.SECONDS)
                .whenComplete((list, throwable) -> {
                    setState(State.SHUTDOWN_COMPLETE);
                }).thenApply(list -> null);
    }

    public ConfidentialMessageService.Result confidentialSend(Message message,
                                                              Address address,
                                                              PubKey receiverPubKey,
                                                              KeyPair senderKeyPair,
                                                              String senderNodeId) {
        return confidentialMessageService.map(service -> service.send(message, address, receiverPubKey, senderKeyPair, senderNodeId))
                .orElseThrow(() -> new RuntimeException("ConfidentialMessageService not present at confidentialSend"));
    }

    public CompletableFuture<Connection> relay(Message message, NetworkId networkId, KeyPair myKeyPair) {
        return relayService.map(service -> service.relay(message, networkId, myKeyPair))
                .orElseThrow(() -> new RuntimeException("RelayService not present at relay"));
    }


    public CompletableFuture<BroadcastResult> requestRemoveData(Message message) {
        checkArgument(dataServicePerTransport.isPresent());
        return dataServicePerTransport.get().requestRemoveData(message);
    }

    public CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter) {
        checkArgument(dataServicePerTransport.isPresent());
        return dataServicePerTransport.get().requestInventory(dataFilter);
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return defaultNode.getSocksProxy();
    }

    public void addMessageListener(Node.Listener listener) {
        confidentialMessageService.ifPresent(service -> service.addMessageListener(listener));
    }

    public void removeMessageListener(Node.Listener listener) {
        confidentialMessageService.ifPresent(service -> service.removeMessageListener(listener));
    }

    public Optional<Address> findMyAddress(String nodeId) {
        return nodesById.findMyAddress(nodeId);
    }

    public Optional<Address> findMyDefaultAddresses() {
        return findMyAddress(Node.DEFAULT_NODE_ID);
    }

    public Optional<Node> findNode(String nodeId) {
        return nodesById.findNode(nodeId);
    }

    public Map<String, Address> getAddressesByNodeId() {
        return nodesById.getAddressesByNodeId();
    }


    private void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
    }
}
