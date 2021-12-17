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

package network.misq.network.p2p.services.peergroup;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.timer.Scheduler;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.MathUtils;
import network.misq.network.p2p.node.*;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeService;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;
import network.misq.network.p2p.services.peergroup.validateaddress.AddressValidationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static network.misq.network.p2p.node.CloseConnectionMessage.Reason.*;

@Slf4j
public class PeerGroupService {

    private final Node node;
    private final Config config;
    @Getter
    private final PeerGroup peerGroup;
    private final PeerExchangeService peerExchangeService;
    private final Quarantine quarantine;
    private final KeepAliveService keepAliveService;
    private final AddressValidationService addressValidationService;
    private final ExecutorService executor;
    private Scheduler scheduler;

    public static record Config(PeerGroup.Config peerGroupConfig,
                                PeerExchangeStrategy.Config peerExchangeConfig,
                                KeepAliveService.Config keepAliveServiceConfig,
                                long bootstrapTime,
                                long interval,
                                long timeout,
                                int maxReported,
                                int maxPersisted,
                                int maxSeeds) {
    }

    public PeerGroupService(Node node, Config config, List<Address> seedNodeAddresses) {
        this.node = node;
        this.config = config;
        quarantine = new Quarantine();
        peerGroup = new PeerGroup(node, config.peerGroupConfig, seedNodeAddresses, quarantine);
        peerExchangeService = new PeerExchangeService(node, new PeerExchangeStrategy(peerGroup, config.peerExchangeConfig()));
        keepAliveService = new KeepAliveService(node, peerGroup, config.keepAliveServiceConfig());
        addressValidationService = new AddressValidationService(node, quarantine);
        executor = ExecutorFactory.getSingleThreadExecutor("PeerGroupService");
    }

    public CompletableFuture<Boolean> initialize() {
        return peerExchangeService.doInitialPeerExchange()
                .thenCompose(__ -> {
                    scheduler = Scheduler.run(this::runBlockingTasks).withExecutor(executor).periodically(config.interval());
                    keepAliveService.initialize();
                    return CompletableFuture.completedFuture(true);
                });
    }

    private void runBlockingTasks() {
        closeQuarantined().join();
        verifyInboundConnections().join();
        CompletableFutureUtils.wait(1000).thenCompose(__ -> maybeCloseDuplicateConnections()).join();
        maybeCloseConnectionsToSeeds().join();
        maybeCloseConnections().join();
        maybeCreateConnections().join();
        maybeRemoveReportedPeers();
        maybeRemovePersistedPeers();
    }


    public CompletableFuture<Void> shutdown() {
        peerExchangeService.shutdown();
        addressValidationService.shutdown();
        keepAliveService.shutdown();
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        executor.shutdownNow();
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<List<Void>> closeQuarantined() {
        log.debug("Node {} called closeQuarantined", node);
        return CompletableFutureUtils.allOf(peerGroup.getAllConnectionsAsStream()
                        .filter(Connection::isRunning)
                        .filter(connection -> quarantine.isInQuarantine(connection.getPeerAddress()))
                        .peek(connection -> log.info("{} -> {}: CloseQuarantined triggered close connection", node, connection.getPeerAddress()))
                        .map(node::closeConnection))
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }

    private CompletableFuture<List<Boolean>> verifyInboundConnections() {
        log.debug("Node {} called verifyInboundConnections", node);
        Map<Address, Connection> outboundMap = getOutboundConnectionsByAddress();
        return CompletableFutureUtils.allOf(peerGroup.getInboundConnections().values().stream()
                        .filter(Connection::isRunning)
                        .filter(addressValidationService::isNotInProgress)
                        .filter(inbound -> !inbound.isPeerAddressVerified())
                        .filter(inbound -> !outboundMap.containsKey(inbound.getPeerAddress()))
                        .peek(inbound -> log.info("{} -> {}: Start addressValidationProtocol", node, inbound.getPeerAddress()))
                        .map(addressValidationService::startAddressValidationProtocol))
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Remove duplicate connections (inbound connections which have an outbound connection with the same address)
     */
    private CompletableFuture<List<Connection>> maybeCloseDuplicateConnections() {
        log.debug("Node {} called maybeCloseDuplicateConnections", node);
        Map<Address, Connection> outboundMap = getOutboundConnectionsByAddress();
        return CompletableFutureUtils.allOf(peerGroup.getInboundConnections().values().stream()
                        .filter(Connection::isRunning)
                        .filter(addressValidationService::isNotInProgress)
                        .filter(inbound -> outboundMap.containsKey(inbound.getPeerAddress()))
                        .filter(this::isNotBootstrapping)
                        .peek(inbound -> log.info("{} -> {}: Send CloseConnectionMessage as we have an " +
                                        "outbound connection with the same address.",
                                node, inbound.getPeerAddress()))
                        .map(inbound -> node.send(new CloseConnectionMessage(DUPLICATE_CONNECTION), inbound)))
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }


    private CompletableFuture<List<Connection>> maybeCloseConnectionsToSeeds() {
        // log.debug("Node {} called maybeCloseConnectionsToSeeds", node);
        int numSeeds = (int) peerGroup.getAllConnectionsAsStream()
                .filter(Connection::isRunning)
                .filter(addressValidationService::isNotInProgress)
                .filter(connection -> peerGroup.isASeed(connection.getPeerAddress())).count();
        if (numSeeds > config.maxSeeds()) {
            log.info("Node {} has {} connections with seed nodes", node, numSeeds);
        }
        return CompletableFutureUtils.allOf(peerGroup.getAllConnectionsAsStream()
                        .filter(Connection::isRunning)
                        .filter(addressValidationService::isNotInProgress)
                        .filter(connection -> peerGroup.isASeed(connection.getPeerAddress()))
                        .skip(config.maxSeeds())
                        .peek(connection -> log.info("{} -> {}: Send CloseConnectionMessage as we have too " +
                                        "many connections to seeds.",
                                node, connection.getPeersCapability().address()))
                        .map(connection -> node.send(new CloseConnectionMessage(TOO_MANY_CONNECTIONS_TO_SEEDS), connection)))
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * If we exceed our maxNumConnectedPeers we try to find enough old inbound connections to remove
     * and if not sufficient we add also old outbound connections.
     */
    private CompletableFuture<List<Connection>> maybeCloseConnections() {
        // log.debug("Node {} called maybeCloseConnections", node);
        int targetNumConnectedPeers = peerGroup.getTargetNumConnectedPeers();
        int numAllConnections = peerGroup.getNumConnections();
        int exceeding = numAllConnections - targetNumConnectedPeers;
        if (exceeding <= 0) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        // Remove the oldest inbound connections
        List<InboundConnection> inbound = new ArrayList<>(peerGroup.getInboundConnections().values());
        inbound.sort(Comparator.comparing(c -> c.getMetrics().getCreationDate()));
        List<Connection> candidates = new ArrayList<>();
        if (!inbound.isEmpty()) {
            List<InboundConnection> list = inbound.subList(0, Math.min(exceeding, inbound.size())).stream()
                    .filter(this::isNotBootstrapping)
                    .collect(Collectors.toList());
            candidates.addAll(list);
        }

        int stillExceeding = exceeding - candidates.size();
        if (stillExceeding > 0) {
            List<Connection> outbound = new ArrayList<>(peerGroup.getOutboundConnections().values());
            outbound.sort(Comparator.comparing(c -> c.getMetrics().getCreationDate()));
            if (!outbound.isEmpty()) {
                List<Connection> list = outbound.subList(0, Math.min(stillExceeding, outbound.size())).stream()
                        .filter(this::isNotBootstrapping)
                        .collect(Collectors.toList());
                candidates.addAll(list);
            }
        }
        if (!candidates.isEmpty()) {
            log.info("Node {} has {} connections. Our connections target is {}. " +
                            "We close {} connections.",
                    node, numAllConnections, targetNumConnectedPeers, candidates.size());
        }
        return CompletableFutureUtils.allOf(candidates.stream()
                        .filter(Connection::isRunning)
                        .filter(addressValidationService::isNotInProgress)
                        .peek(connection -> log.info("Node {} send CloseConnectionMessage to peer {} as we have too many connections.",
                                node, connection.getPeersCapability().address().toString()))
                        .map(connection -> node.send(new CloseConnectionMessage(TOO_MANY_CONNECTIONS), connection)))
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }

    private CompletableFuture<Void> maybeCreateConnections() {
        // log.debug("Node {} called maybeCreateConnections", node);
        int minNumConnectedPeers = peerGroup.getMinNumConnectedPeers();
        int numOutboundConnections = peerGroup.getOutboundConnections().size();
        // We want to have at least 40% of our minNumConnectedPeers as outbound connections 
        int missingOutboundConnections = MathUtils.roundDoubleToInt(minNumConnectedPeers * 0.4) - numOutboundConnections;
        if (missingOutboundConnections <= 0) {
            // We have enough outbound connections, lets check if we have sufficient connections in total
            int numAllConnections = peerGroup.getNumConnections();
            int missing = minNumConnectedPeers - numAllConnections;
            if (missing <= 0) {
                return CompletableFuture.completedFuture(null);
            }
        }

        // We use the peer exchange protocol for establishing new connections.
        // The calculation how many connections we need is done inside PeerExchangeService/PeerExchangeStrategy

        log.info("Node {} has not sufficient connections and calls peerExchangeService.doFurtherPeerExchange", node);
        return peerExchangeService.doFurtherPeerExchange()
                .orTimeout(config.timeout(), TimeUnit.MILLISECONDS);
    }

    private void maybeRemoveReportedPeers() {
        // log.debug("Node {} called maybeRemoveReportedPeers", node);
        List<Peer> reportedPeers = new ArrayList<>(peerGroup.getReportedPeers());
        int exceeding = reportedPeers.size() - config.maxReported();
        if (exceeding > 0) {
            reportedPeers.sort(Comparator.comparing(Peer::getDate));
            List<Peer> candidates = reportedPeers.subList(0, Math.min(exceeding, reportedPeers.size()));
            log.info("Remove {} reported peers: {}", candidates.size(), candidates);
            peerGroup.removeReportedPeers(candidates);
        }
    }

    private void maybeRemovePersistedPeers() {
        // log.debug("Node {} called maybeRemovePersistedPeers", node);
        List<Peer> persistedPeers = new ArrayList<>(peerGroup.getPersistedPeers());
        int exceeding = persistedPeers.size() - config.maxPersisted();
        if (exceeding > 0) {
            persistedPeers.sort(Comparator.comparing(Peer::getDate));
            List<Peer> candidates = persistedPeers.subList(0, Math.min(exceeding, persistedPeers.size()));
            log.info("Remove {} persisted peers: {}", candidates.size(), candidates);
            peerGroup.removePersistedPeers(candidates);
        }
    }

    private boolean isNotBootstrapping(Connection connection) {
        return connection.getMetrics().getAge() > config.bootstrapTime();
    }

    private Map<Address, Connection> getOutboundConnectionsByAddress() {
        return peerGroup.getOutboundConnections().values().stream()
                .filter(Connection::isRunning)
                .filter(addressValidationService::isNotInProgress)
                .collect(Collectors.toMap(Connection::getPeerAddress, c -> c));
    }
}