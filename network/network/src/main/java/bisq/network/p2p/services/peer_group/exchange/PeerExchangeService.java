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
import bisq.common.observable.Pin;
import bisq.common.timer.Scheduler;
import bisq.common.util.ExceptionUtil;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Responsible for executing the peer exchange protocol with set of peers.
 * We use the PeerExchangeStrategy for the selection of nodes.
 */
@Slf4j
@Getter
public class PeerExchangeService implements Node.Listener {
    private static final int MAX_RETRY_ATTEMPTS = 10;

    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;

    private final PeerExchangeAttempt initialPeerExchangeAttempt;
    private final AtomicReference<Optional<PeerExchangeAttempt>> extendPeerGroupPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<PeerExchangeAttempt>> retryPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicInteger numRetryAttempts = new AtomicInteger();
    @Nullable
    private CountDownLatch minSuccessReachedLatch;
    @Nullable
    private Pin minSuccessReachedPin;
    private volatile boolean isShutdownInProgress;
    private Scheduler retryScheduler;

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;

        node.addListener(this);
        initialPeerExchangeAttempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "initialPeerExchangeAttempt");
    }

    public void shutdown() {
        if (isShutdownInProgress) {
            return;
        }
        if (retryScheduler != null) {
            retryScheduler.stop();
            retryScheduler = null;
        }

        isShutdownInProgress = true;
        node.removeListener(this);
        if (minSuccessReachedLatch != null && minSuccessReachedLatch.getCount() > 0) {
            minSuccessReachedLatch.countDown();
            minSuccessReachedLatch = null;
        }
        if (minSuccessReachedPin != null) {
            minSuccessReachedPin.unbind();
            minSuccessReachedPin = null;
        }
        initialPeerExchangeAttempt.shutdown();
        extendPeerGroupPeerExchangeAttempt.get().ifPresent(PeerExchangeAttempt::shutdown);
        extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
        retryPeerExchangeAttempt.get().ifPresent(PeerExchangeAttempt::shutdown);
        retryPeerExchangeAttempt.set(Optional.empty());
    }


    /* --------------------------------------------------------------------- */
    // Node.Listener implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof PeerExchangeRequest request) {
            Address peerAddress = connection.getPeerAddress();
            List<Peer> myPeers = new ArrayList<>(peerExchangeStrategy.getPeersForReporting(peerAddress));
            peerExchangeStrategy.addReportedPeers(new HashSet<>(request.getPeers()), peerAddress);
            PeerExchangeResponse response = new PeerExchangeResponse(request.getNonce(), myPeers);
            node.sendAsync(response, connection)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.warn("Sending {} to {} failed. {}", response.getClass().getSimpleName(), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                        } else {
                            log.debug("Sent PeerExchangeResponse with my myPeers {}", myPeers);
                        }
                    });
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void startInitialPeerExchange() {
        // Runs in NetworkNode thread
        log.info("Start initial peer exchange");

        List<Address> candidates = peerExchangeStrategy.getAddressesForInitialPeerExchange();
        minSuccessReachedLatch = new CountDownLatch(1);

        if (minSuccessReachedPin != null) {
            minSuccessReachedPin.unbind();
        }
        minSuccessReachedPin = initialPeerExchangeAttempt.getMinSuccessReached().get().addObserver(minSuccessReached -> {
            if (minSuccessReached) {
                minSuccessReachedPin.unbind();
                minSuccessReachedPin = null;
                minSuccessReachedLatch.countDown();

                // At fist startup we only have the seed nodes. It might take up to 90 sec. before we get completed.
                // To improve bootstrap behaviour we start another attempt using only reported and persisted peers,
                // thus excluding the seeds and already connected peers.
                if (candidates.size() < 8) {
                    extendPeerGroupAsync();
                }
            }
        });

        initialPeerExchangeAttempt.startAsync(1, candidates)
                .whenComplete((success, throwable) -> {
                    if (throwable != null || !success) {
                        log.info("Initial peer exchange completed. " +
                                "We start a parallel peerExchangeAttempt for faster connection establishment.");

                        // We release the block in case the minSuccessReached was never reached
                        if (minSuccessReachedLatch.getCount() > 0) {
                            minSuccessReachedLatch.countDown();
                        }
                        maybeRetryPeerExchange();
                    }
                });

        try {
            // Block here until we got at least one peer exchange completed
            boolean hadTimeout = !minSuccessReachedLatch.await(90, TimeUnit.SECONDS);
            if (hadTimeout) {
                log.info("Initial peer exchange did not reach min success in 90 sec. " +
                        "We retry asynchronously and return to the caller.");
                maybeRetryPeerExchange();
            }
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at minSuccessReachedLatch.await in the startInitialPeerExchange method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state

            maybeRetryPeerExchange();
        }
    }

    public CompletableFuture<Boolean> extendPeerGroupAsync() {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(false);
        }
        if (extendPeerGroupPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending extendPeerGroupPeerExchangeAttempt. We ignore the extendPeerGroup call.");
            return CompletableFuture.completedFuture(false);
        }
        log.info("Extend peer group");
        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "extendPeerGroupPeerExchangeAttempt");
        extendPeerGroupPeerExchangeAttempt.set(Optional.of(attempt));
        List<Address> candidates = peerExchangeStrategy.getAddressesForExtendingPeerGroup();
        try {
            return attempt.startAsync(candidates.size() / 2, candidates)
                    .whenComplete((success, throwable) -> {
                        if (throwable != null || success == null || !success) {
                            log.warn("extendPeerGroupPeerExchangeAttempt completed unsuccessful.");
                        }
                        extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
                    });
        } catch (Exception e) {
            log.warn("extendPeerGroupPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));
            extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
            return CompletableFuture.failedFuture(e);
        }
    }

    private void maybeRetryPeerExchange() {
        if (shouldRetryPeerExchange()) {
            retryPeerExchange();
        }
    }

    private boolean shouldRetryPeerExchange() {
        if (isShutdownInProgress) {
            return false;
        }
        if (retryPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending retryPeerExchangeAttempt. We ignore the retryPeerExchange call.");
            return false;
        }
        if (numRetryAttempts.get() > MAX_RETRY_ATTEMPTS) {
            log.warn("We have retried the peer exchange {} times without success and give up.", MAX_RETRY_ATTEMPTS);
            return false;
        }
        return true;
    }

    private CompletableFuture<Boolean> retryPeerExchange() {
        return nonBlockingDelay()
                .thenCompose(nil -> {
                    try {
                        numRetryAttempts.incrementAndGet();
                        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "retryPeerExchangeAttempt");
                        retryPeerExchangeAttempt.set(Optional.of(attempt));
                        List<Address> candidates = peerExchangeStrategy.getAddressesForRetryPeerExchange();
                        return attempt.startAsync(candidates.size() / 2, candidates)
                                .whenComplete((success, throwable) -> {
                                    retryPeerExchangeAttempt.set(Optional.empty());

                                    if (throwable != null || success == null || !success) {
                                        log.warn("retryPeerExchangeAttempt completed unsuccessful.");
                                        maybeRetryPeerExchange();
                                    }
                                });
                    } catch (Exception e) {
                        log.warn("retryPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));
                        retryPeerExchangeAttempt.set(Optional.empty());
                        maybeRetryPeerExchange();
                        return CompletableFuture.failedFuture(e);
                    }
                });
    }

    private CompletableFuture<Void> nonBlockingDelay() {
        long delay = 1000L * numRetryAttempts.get() * numRetryAttempts.get();
        if (delay == 0) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (retryScheduler == null) {
            retryScheduler = Scheduler.run(() -> {
                        log.info("Retry peer exchange{}", delay > 0 ? " after " + (delay / 1000) + " sec." : "");
                        future.complete(null);
                        retryScheduler = null;
                    })
                    .host(this)
                    .runnableName("retryPeerExchange")
                    .after(delay);
        }
        return future;
    }
}