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
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataNetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.monitor.MonitorService;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.network.p2p.services.relay.RelayService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
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

    public interface Listener {
        void onStateChanged(ServiceNode.State state);
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
    private Optional<DataNetworkService> dataServicePerTransport;

    private Optional<RelayService> relayService;
    @Getter
    private Optional<MonitorService> monitorService;
    @Getter
    public AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ServiceNode(Config config,
                       Node.Config nodeConfig,
                       PeerGroupService.Config peerGroupServiceConfig,
                       Optional<DataService> dataService,
                       KeyPairService keyPairService,
                       PersistenceService persistenceService,
                       Set<Address> seedNodeAddresses) {
        BanList banList = new BanList();
        nodesById = new NodesById(banList, nodeConfig);
        defaultNode = nodesById.getDefaultNode();
        Set<Service> services = config.services();

        if (services.contains(Service.PEER_GROUP)) {
            PeerGroupService peerGroupService = new PeerGroupService(persistenceService, defaultNode, banList, peerGroupServiceConfig, seedNodeAddresses);
            this.peerGroupService = Optional.of(peerGroupService);

            if (services.contains(Service.DATA)) {
                dataServicePerTransport = Optional.of(dataService.orElseThrow().getDataServicePerTransport(nodeConfig.transportType(),
                        defaultNode,
                        peerGroupService));
            }

            if (services.contains(Service.RELAY)) {
                relayService = Optional.of(new RelayService(defaultNode));
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

    public CompletableFuture<Void> shutdown() {
        setState(State.SHUTDOWN_STARTED);
        return CompletableFutureUtils.allOf(nodesById.shutdown(),
                        confidentialMessageService.map(ConfidentialMessageService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        peerGroupService.map(PeerGroupService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        dataServicePerTransport.map(DataNetworkService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        relayService.map(RelayService::shutdown).orElse(CompletableFuture.completedFuture(null)),
                        monitorService.map(MonitorService::shutdown).orElse(CompletableFuture.completedFuture(null)))
                .orTimeout(4, TimeUnit.SECONDS)
                .whenComplete((list, throwable) -> {
                    setState(State.SHUTDOWN_COMPLETE);
                }).thenApply(list -> null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean maybeInitializeServer(String nodeId, int serverPort) {
        if (nodeId.equals(Node.DEFAULT)) {
            setState(State.INITIALIZE_DEFAULT_NODE_SERVER);
        }
        try {
            nodesById.maybeInitializeServer(nodeId, serverPort);
            if (nodeId.equals(Node.DEFAULT)) {
                setState(State.DEFAULT_NODE_SERVER_INITIALIZED);
            }
            return true;
        } catch (Throwable throwable) {
            log.error("maybeInitializeServer failed. nodeId=" + nodeId + ";serverPort=" + serverPort, throwable);
            return false;
        }
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
        peerGroupService.ifPresent(PeerGroupService::start);
        setState(State.PEER_GROUP_INITIALIZED);
    }

    public ConfidentialMessageService.Result confidentialSend(NetworkMessage networkMessage,
                                                              Address address,
                                                              PubKey receiverPubKey,
                                                              KeyPair senderKeyPair,
                                                              String senderNodeId) {
        return confidentialMessageService.map(service -> service.send(networkMessage, address, receiverPubKey, senderKeyPair, senderNodeId))
                .orElseThrow(() -> new RuntimeException("ConfidentialMessageService not present at confidentialSend"));
    }

    public CompletableFuture<Connection> relay(NetworkMessage networkMessage, NetworkId networkId, KeyPair myKeyPair) {
        return relayService.map(service -> service.relay(networkMessage, networkId, myKeyPair))
                .orElseThrow(() -> new RuntimeException("RelayService not present at relay"));
    }

    public void addMessageListener(MessageListener messageListener) {
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

    public Optional<Address> findMyDefaultAddresses() {
        return findMyAddress(Node.DEFAULT);
    }

    private void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
        runAsync(() -> listeners.forEach(e -> e.onStateChanged(newState)), NetworkService.DISPATCHER);
    }
}
