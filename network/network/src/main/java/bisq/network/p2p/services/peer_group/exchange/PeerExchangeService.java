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

package bisq.network.p2p.services.peer_group.exchange;

import bisq.common.network.Address;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Delay;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.p2p.common.RequestResponseHandler;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class PeerExchangeService extends RequestResponseHandler<PeerExchangeRequest, PeerExchangeResponse> {
    private static final long TIMEOUT = SECONDS.toMillis(30);

    @Getter
    public static class Config {
        private final int numSeedNodesAtBootstrap;
        private final int numPersistedPeersAtBootstrap;
        private final int numReportedPeersAtBootstrap;
        private final boolean supportPeerReporting;

        public Config(int numSeedNodesAtBootstrap,
                      int numPersistedPeersAtBootstrap,
                      int numReportedPeersAtBootstrap,
                      boolean supportPeerReporting) {
            this.numSeedNodesAtBootstrap = numSeedNodesAtBootstrap;
            this.numPersistedPeersAtBootstrap = numPersistedPeersAtBootstrap;
            this.numReportedPeersAtBootstrap = numReportedPeersAtBootstrap;
            this.supportPeerReporting = supportPeerReporting;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new Config(
                    typesafeConfig.getInt("numSeedNodesAtBootstrap"),
                    typesafeConfig.getInt("numPersistedPeersAtBootstrap"),
                    typesafeConfig.getInt("numReportedPeersAtBootstrap"),
                    typesafeConfig.getBoolean("supportPeerReporting"));
        }
    }

    private ExecutorService executor;
    private final PeerGroupService peerGroupService;
    private final PeerExchangePolicy policy;
    private final AtomicInteger numRetryAttempts = new AtomicInteger();
    private volatile boolean isShutdownInProgress;

    public PeerExchangeService(Node node,
                               PeerGroupService peerGroupService,
                               Config config,
                               PeerGroupService.Config peerGroupConfig) {
        super(node, TIMEOUT);

        this.peerGroupService = peerGroupService;

        policy = new PeerExchangePolicy(peerGroupService, node, config, peerGroupConfig);
    }

    public void initialize() {
        isShutdownInProgress = false;
        if (executor == null || executor.isShutdown()) {
            executor = ExecutorFactory.newSingleThreadExecutor("PeerExchangeService");
        }
        super.initialize();
    }

    public void shutdown() {
        isShutdownInProgress = true;
        super.shutdown();
        if (executor != null) {
            ExecutorFactory.shutdownAndAwaitTermination(executor);
            executor = null;
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> startInitialPeerExchange() {
        List<Address> candidates = policy.getAddressesForInitialPeerExchange();
        int minSuccess = 1;
        return doBatchedPeerExchange(candidates, minSuccess, true)
                .whenComplete((result, throwable) -> {
                    if (policy.shouldExtendAfterInitialExchange(result, throwable, candidates)) {
                        extendPeerGroup();
                    }
                });
    }

    public CompletableFuture<Boolean> extendPeerGroup() {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(false);
        }
        log.info("Extend peerGroup");
        List<Address> candidates = policy.getAddressesForExtendingPeerGroup();
        int minSuccess = policy.getMinSuccessForExtendPeerGroup(candidates);
        return doBatchedPeerExchange(candidates, minSuccess, false);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private CompletableFuture<Boolean> retryPeerExchangeWithDelay() {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(false);
        }
        int numRetries = numRetryAttempts.incrementAndGet();
        long delay = policy.getRetryDelay(numRetries);
        log.info("retryPeerExchangeWithDelay. delay={}", delay);
        return Delay.run(this::retryPeerExchange)
                .withExecutor(executor)
                .after(delay);
    }

    private CompletableFuture<Boolean> retryPeerExchange() {
        List<Address> candidates = policy.getAddressesForRetryPeerExchange();
        int minSuccess = policy.getMinSuccessForRetry(candidates);
        return doBatchedPeerExchange(candidates, minSuccess, true);
    }

    private CompletableFuture<Boolean> doBatchedPeerExchange(List<Address> candidates,
                                                             int minSuccess,
                                                             boolean doRetryIfNeeded) {
        if (candidates.isEmpty() || isShutdownInProgress) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        BatchedPeerExchangePolicy batchedPeerExchangePolicy = new BatchedPeerExchangePolicy(peerGroupService, node, minSuccess, candidates.size());
        Stream<CompletableFuture<PeerExchangeResponse>> futures = candidates.stream()
                .map(address -> {
                    CompletableFuture<PeerExchangeResponse> requestFuture = requestPeerExchange(address);
                    return requestFuture
                            .whenComplete((response, throwable) -> {
                                batchedPeerExchangePolicy.trackSuccess(throwable);
                                if (batchedPeerExchangePolicy.wasMinSuccessReached()) {
                                    resultFuture.complete(true);
                                }
                            });
                });

        int timeout = 90;
        CompletableFutureUtils.failureTolerantAllOf(futures)
                .orTimeout(timeout, SECONDS)
                .whenComplete((result, throwable) -> {
                    if (doRetryIfNeeded) {
                        if (batchedPeerExchangePolicy.requiresRetry(throwable)) {
                            retryPeerExchangeWithDelay();
                        }
                    }
                    if (throwable != null && !resultFuture.isDone()) {
                        String errorMessage = throwable instanceof TimeoutException ?
                                "TimeoutException. doBatchedPeerExchange did not complete after " + timeout + " sec." :
                                throwable.getClass().getSimpleName() + ": doBatchedPeerExchange failed.";
                        log.warn(errorMessage);
                        if (!isShutdownInProgress) {
                            resultFuture.completeExceptionally(throwable);
                        } else {
                            // During shutdown, suppress exceptions but still complete to avoid hanging callers
                            resultFuture.complete(false);
                        }
                    } else if (!resultFuture.isDone()) {
                        resultFuture.complete(false);
                    }
                });
        return resultFuture;
    }

    @Override
    protected PeerExchangeResponse createResponse(Connection connection, PeerExchangeRequest request) {
        List<Peer> myPeers = policy.getPeersForReporting(connection.getPeerAddress());
        return new PeerExchangeResponse(request.getNonce(), myPeers);
    }

    @Override
    protected Class<PeerExchangeRequest> getRequestClass() {
        return PeerExchangeRequest.class;
    }

    @Override
    protected Class<PeerExchangeResponse> getResponseClass() {
        return PeerExchangeResponse.class;
    }

    @Override
    protected void onRequest(Connection connection, PeerExchangeRequest request) {
        Address peerAddress = connection.getPeerAddress();
        policy.addReportedPeers(request.getPeers(), peerAddress);
    }

    private CompletableFuture<PeerExchangeResponse> requestPeerExchange(Address peerAddress) {
        return node.getOrCreateConnectionAsync(peerAddress)
                .thenCompose(this::requestPeerExchange);
    }

    private CompletableFuture<PeerExchangeResponse> requestPeerExchange(Connection connection) {
        Address peerAddress = connection.getPeerAddress();
        List<Peer> myPeers = policy.getPeersForReporting(peerAddress);
        PeerExchangeRequest request = new PeerExchangeRequest(createNonce(), myPeers);
        return request(connection, request)
                .whenComplete((response, throwable) -> {
                    if (throwable == null) {
                        policy.addReportedPeers(response.getPeers(), peerAddress);
                    }
                });
    }
}