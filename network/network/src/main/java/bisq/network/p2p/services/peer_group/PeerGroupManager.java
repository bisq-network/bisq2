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

package bisq.network.p2p.services.peer_group;

import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.common.network.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeService;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeStrategy;
import bisq.network.p2p.services.peer_group.keep_alive.KeepAliveService;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeService;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.*;

@Slf4j
public class PeerGroupManager implements Node.Listener {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    public interface Listener {
        void onStateChanged(PeerGroupManager.State state);
    }

    @Getter
    @ToString
    public static final class Config {
        private final PeerGroupService.Config peerGroupConfig;
        private final PeerExchangeStrategy.Config peerExchangeConfig;
        private final KeepAliveService.Config keepAliveServiceConfig;
        private final long bootstrapTime;
        private final long houseKeepingInterval;
        private final long timeout;
        private final long maxAge;
        private final int maxReported;
        private final int maxPersisted;
        private final int maxSeeds;

        public Config(PeerGroupService.Config peerGroupConfig,
                      PeerExchangeStrategy.Config peerExchangeConfig,
                      KeepAliveService.Config keepAliveServiceConfig,
                      long bootstrapTime,
                      long houseKeepingInterval,
                      long timeout,
                      long maxAge,
                      int maxReported,
                      int maxPersisted,
                      int maxSeeds) {
            this.peerGroupConfig = peerGroupConfig;
            this.peerExchangeConfig = peerExchangeConfig;
            this.keepAliveServiceConfig = keepAliveServiceConfig;
            this.bootstrapTime = bootstrapTime;
            this.houseKeepingInterval = houseKeepingInterval;
            this.timeout = timeout;
            this.maxAge = maxAge;
            this.maxReported = maxReported;
            this.maxPersisted = maxPersisted;
            this.maxSeeds = maxSeeds;
        }

        public static Config from(PeerGroupService.Config peerGroupConfig,
                                  PeerExchangeStrategy.Config peerExchangeStrategyConfig,
                                  KeepAliveService.Config keepAliveServiceConfig,
                                  com.typesafe.config.Config typesafeConfig) {
            return new Config(peerGroupConfig,
                    peerExchangeStrategyConfig,
                    keepAliveServiceConfig,
                    SECONDS.toMillis(typesafeConfig.getLong("bootstrapTimeInSeconds")),
                    SECONDS.toMillis(typesafeConfig.getLong("houseKeepingIntervalInSeconds")),
                    SECONDS.toMillis(typesafeConfig.getLong("timeoutInSeconds")),
                    HOURS.toMillis(typesafeConfig.getLong("maxAgeInHours")),
                    typesafeConfig.getInt("maxPersisted"),
                    typesafeConfig.getInt("maxReported"),
                    typesafeConfig.getInt("maxSeeds")
            );
        }
    }

    @Getter
    private final Node node;
    private final BanList banList;
    private final Config config;
    @Getter
    private final PeerGroupService peerGroupService;
    private final PeerExchangeService peerExchangeService;
    private final KeepAliveService keepAliveService;
    private final NetworkLoadExchangeService networkLoadExchangeService;
    private Optional<Scheduler> scheduler = Optional.empty();
    private Optional<Scheduler> maybeCreateConnectionsScheduler = Optional.empty();

    @Getter
    public final AtomicReference<PeerGroupManager.State> state = new AtomicReference<>(PeerGroupManager.State.NEW);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private final RetryPolicy<Boolean> retryPolicy;

    public PeerGroupManager(Node node,
                            PeerGroupService peerGroupService,
                            BanList banList,
                            Config config) {
        this.node = node;
        this.banList = banList;
        this.config = config;
        this.peerGroupService = peerGroupService;
        PeerExchangeStrategy peerExchangeStrategy = new PeerExchangeStrategy(peerGroupService,
                node,
                config.getPeerExchangeConfig());
        peerExchangeService = new PeerExchangeService(node, peerExchangeStrategy);
        keepAliveService = new KeepAliveService(node, config.getKeepAliveServiceConfig());
        networkLoadExchangeService = new NetworkLoadExchangeService(node);

        retryPolicy = RetryPolicy.<Boolean>builder()
                .handle(IllegalStateException.class)
                .handleResultIf(result -> state.get() == State.STARTING)
                .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(20))
                .withJitter(0.25)
                .withMaxDuration(Duration.ofMinutes(5))
                .withMaxRetries(10)
                .onRetry(e -> log.info("Retry called. AttemptCount={}.", e.getAttemptCount()))
                .onRetriesExceeded(e -> log.warn("Failed. Max retries exceeded."))
                .onSuccess(e -> log.debug("Succeeded."))
                .build();
    }

    public void initialize() {
        // blocking
        node.addListener(this);
        Failsafe.with(retryPolicy).run(this::doInitialize);
    }

    public void shutdown() {
        setState(State.STOPPING);
        node.removeListener(this);
        peerExchangeService.shutdown();
        keepAliveService.shutdown();
        networkLoadExchangeService.shutdown();
        scheduler.ifPresent(Scheduler::stop);
        maybeCreateConnectionsScheduler.ifPresent(Scheduler::stop);
        setState(State.TERMINATED);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        maybeCreateConnectionsScheduler.ifPresent(Scheduler::stop);
        maybeCreateConnectionsScheduler = Optional.of(Scheduler.run(this::maybeCreateConnections)
                .host(this)
                .runnableName("maybeCreateConnections")
                .after(2000));
    }

    private void doInitialize() {
        log.info("{} called initialize", node);
        String nodeInfo = node.getNodeInfo();
        State state = getState().get();
        switch (state) {
            case NEW:
                setState(PeerGroupManager.State.STARTING);
                // blocking
                peerExchangeService.startInitialPeerExchange();
                log.info("Completed startInitialPeerExchange. Start periodic tasks with interval: {} sec",
                        config.getHouseKeepingInterval() / 1000);
                scheduler = Optional.of(Scheduler.run(this::doHouseKeeping)
                        .host(this)
                        .runnableName("doHouseKeeping")
                        .periodically(config.getHouseKeepingInterval() / 4, config.getHouseKeepingInterval(), MILLISECONDS));
                keepAliveService.initialize();
                networkLoadExchangeService.initialize();
                setState(State.RUNNING);
                break;
            case STARTING:
            case RUNNING:
            case STOPPING:
            case TERMINATED:
                log.warn("Got called at an invalid state. We ignore that call. State={}", state);
                break;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Seed nodes
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSeedNodeAddresses(Set<Address> seedNodeAddresses) {
        seedNodeAddresses.forEach(peerGroupService::addSeedNodeAddress);
    }

    public void addSeedNodeAddress(Address seedNodeAddress) {
        peerGroupService.addSeedNodeAddress(seedNodeAddress);
    }

    public void removeSeedNodeAddress(Address seedNodeAddress) {
        peerGroupService.removeSeedNodeAddress(seedNodeAddress);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void doHouseKeeping() {
        log.debug("{} called runBlockingTasks", node);
        try {
            closeBanned();
            maybeCloseDuplicateConnections();
            Thread.sleep(100);
            maybeCloseConnectionsToSeeds();
            Thread.sleep(100);
            maybeCloseAgedConnections();
            Thread.sleep(100);
            maybeCloseExceedingInboundConnections();
            Thread.sleep(100);
            maybeCloseExceedingConnections();
            Thread.sleep(100);
            maybeCreateConnectionsScheduler.ifPresent(Scheduler::stop);
            maybeCreateConnectionsScheduler = Optional.empty();
            maybeCreateConnections();
            maybeRemoveReportedPeers();
            maybeRemovePersistedPeers();
        } catch (InterruptedException ignore) {
        }
    }

    private void closeBanned() {
        log.debug("{} called closeBanned", node);
        node.getAllActiveConnections()
                .filter(Connection::isRunning)
                .filter(connection -> banList.isBanned(connection.getPeerAddress()))
                .peek(connection -> log.info("Close connection to banned node. connection={} ", connection.getPeerAddress()))
                .forEach(connection -> node.closeConnection(connection, CloseReason.BANNED));
    }

    /**
     * Remove duplicate connections (inbound connections which have an outbound connection with the same address)
     */
    private void maybeCloseDuplicateConnections() {
        log.debug("{} called maybeCloseDuplicateConnections", node);
        Set<Address> outboundAddresses = node.getActiveOutboundConnections()
                .map(Connection::getPeerAddress)
                .collect(Collectors.toSet());
        node.getActiveInboundConnections()
                .filter(this::allowDisconnect)
                .filter(this::hasNoPendingRequests)
                .filter(inbound -> outboundAddresses.contains(inbound.getPeerAddress()))
                .peek(inbound -> log.info("{}: Send CloseConnectionMessage as we have an " +
                                "outbound connection with the same address.",
                        inbound.getPeerAddress()))
                .forEach(inbound -> node.closeConnectionGracefully(inbound, CloseReason.DUPLICATE_CONNECTION));
    }


    private void maybeCloseConnectionsToSeeds() {
        log.debug("{} called maybeCloseConnectionsToSeeds", node);
        node.getAllActiveConnections()
                .filter(this::allowDisconnect)
                .filter(this::hasNoPendingRequests)
                .filter(peerGroupService::isSeed)
                .sorted(comparingForSkip())
                .skip(config.getMaxSeeds())
                .peek(connection -> log.info("{}: Send CloseConnectionMessage as we have too " +
                                "many connections to seeds.",
                        connection.getPeerAddress()))
                .forEach(connection -> node.closeConnectionGracefully(connection, CloseReason.TOO_MANY_CONNECTIONS_TO_SEEDS));
    }

    private void maybeCloseAgedConnections() {
        log.debug("{} called maybeCloseAgedConnections", node);
        long maxAgeDate = System.currentTimeMillis() - config.getMaxAge();
        int minNumConnectedPeers = peerGroupService.getMinNumConnectedPeers();
        node.getAllActiveConnections()
                .filter(this::allowDisconnect)
                .sorted(comparingForSkip())
                .filter(connection -> connection.createdBefore(maxAgeDate))
                .skip(minNumConnectedPeers)
                .peek(connection -> log.info("{}: Send CloseConnectionMessage as the connection age " +
                                "is too old.",
                        connection.getPeerAddress()))
                .forEach(connection -> node.closeConnectionGracefully(connection, CloseReason.AGED_CONNECTION));
    }

    private void maybeCloseExceedingInboundConnections() {
        log.debug("{} called maybeCloseExceedingInboundConnections", node);
        node.getActiveInboundConnections()
                .filter(this::allowDisconnect)
                .sorted(comparingForSkip())
                .skip(peerGroupService.getMaxInboundConnections())
                .peek(connection -> log.info("{}: Send CloseConnectionMessage as we have too many inbound connections.",
                        connection.getPeerAddress()))
                .forEach(connection -> node.closeConnectionGracefully(connection, CloseReason.TOO_MANY_INBOUND_CONNECTIONS));
    }

    private void maybeCloseExceedingConnections() {
        log.debug("{} called maybeCloseExceedingConnections", node);
        node.getAllActiveConnections()
                .filter(this::allowDisconnect)
                .sorted(comparingForSkip())
                .skip(peerGroupService.getMaxNumConnectedPeers())
                .peek(connection -> log.info("{}: Send CloseConnectionMessage as we have too many connections.",
                        connection.getPeerAddress()))
                .forEach(connection -> node.closeConnectionGracefully(connection, CloseReason.TOO_MANY_CONNECTIONS));
    }

    private void maybeCreateConnections() {
        log.debug("{} called maybeCreateConnections", node);
        int minNumConnectedPeers = peerGroupService.getMinNumConnectedPeers();
        if (getMissingOutboundConnections() <= 0) {
            // We have enough outbound connections, lets check if we have sufficient connections in total
            if (node.getNumConnections() >= minNumConnectedPeers) {
                log.debug("{} has sufficient connections", node);
                CompletableFuture.completedFuture(null);
                return;
            }
        }

        // We use the peer exchange protocol for establishing new connections.
        // The calculation how many connections we need is done inside PeerExchangeService/PeerExchangeStrategy
        log.info("We have not sufficient connections and call peerExchangeService.extendPeerGroup");
        // It is an async call. We do not wait for the result.
        peerExchangeService.extendPeerGroupAsync();
    }

    private void maybeRemoveReportedPeers() {
        List<Peer> reportedPeers = new ArrayList<>(peerGroupService.getReportedPeers());
        int exceeding = reportedPeers.size() - config.getMaxReported();
        if (exceeding > 0) {
            reportedPeers.sort(Comparator.comparing(Peer::getDate));
            List<Peer> outDated = reportedPeers.subList(0, Math.min(exceeding, reportedPeers.size()));
            log.info("Remove {} reported peers: {}", outDated.size(), StringUtils.truncate(outDated.toString(), 500));
            peerGroupService.removeReportedPeers(outDated);
        }
    }

    private void maybeRemovePersistedPeers() {
        List<Peer> outDated = peerGroupService.getPersistedPeers().stream()
                .sorted()
                .skip(config.getMaxPersisted())
                .collect(Collectors.toList());
        if (!outDated.isEmpty()) {
            log.info("Remove {} persisted peers: {}",
                    outDated.size(),
                    outDated.stream()
                            .sorted()
                            .map(e -> "Age: " + StringUtils.formatTime(e.getAge()))
                            .collect(Collectors.toList()));
            peerGroupService.removePersistedPeers(outDated);
        }
    }

    public void addListener(PeerGroupManager.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(PeerGroupManager.Listener listener) {
        listeners.remove(listener);
    }

    private void setState(PeerGroupManager.State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
        runAsync(() -> listeners.forEach(listener -> {
            try {
                listener.onStateChanged(newState);
            } catch (Exception e) {
                log.error("Calling onStateChanged at listener {} failed", listener, e);
            }
        }), NetworkService.DISPATCHER);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Used for Stream.skip, therefor we sort by descending numPendingRequests and descending creationDate so that we close
    // the oldest connections which have the least pending requests first.
    private Comparator<Connection> comparingForSkip() {
        return Connection.comparingNumPendingRequests().reversed()
                .thenComparing(Connection.comparingDate().reversed());
    }

    private boolean allowDisconnect(Connection connection) {
        return isNotBootstrapping(connection) && connection.isRunning();
    }

    private boolean hasNoPendingRequests(Connection connection) {
        return !connection.getRequestResponseManager().hasPendingRequests();
    }

    private boolean isNotBootstrapping(Connection connection) {
        return connection.getConnectionMetrics().getAge() > config.getBootstrapTime();
    }

    private int getMissingOutboundConnections() {
        return peerGroupService.getMinOutboundConnections() - (int) node.getActiveOutboundConnections().count();
    }
}