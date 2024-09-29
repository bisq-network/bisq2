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

import bisq.common.observable.Pin;
import bisq.common.threading.ThreadName;
import bisq.common.util.ExceptionUtil;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.NetworkService.NETWORK_IO_POOL;

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
    private CountDownLatch minSuccessReachedLatch;
    private Pin minSuccessReachedPin;
    private volatile boolean isShutdownInProgress;

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
        retryPeerExchangeAttempt.get().ifPresent(PeerExchangeAttempt::shutdown);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof PeerExchangeRequest request) {
            Address peerAddress = connection.getPeerAddress();
            List<Peer> myPeers = new ArrayList<>(peerExchangeStrategy.getPeersForReporting(peerAddress));
            peerExchangeStrategy.addReportedPeers(new HashSet<>(request.getPeers()), peerAddress);
            NETWORK_IO_POOL.submit(() -> {
                ThreadName.set(this, "response");
                node.send(new PeerExchangeResponse(request.getNonce(), myPeers), connection);
            });
            log.debug("Sent PeerExchangeResponse with my myPeers {}", myPeers);
        }
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

    public void startInitialPeerExchange() {
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
                        retryPeerExchangeAsync();
                    }
                });

        try {
            // Block here until we got at least one peer exchange completed
            boolean hadTimeout = !minSuccessReachedLatch.await(90, TimeUnit.SECONDS);
            if (hadTimeout) {
                log.info("Initial peer exchange did not reach min success in 90 sec. " +
                        "We retry asynchronously and return to the caller.");
                retryPeerExchangeAsync();
            }
        } catch (InterruptedException e) {
            log.warn("minSuccessReachedLatch.await failed. {}", ExceptionUtil.getRootCauseMessage(e));
            retryPeerExchangeAsync();
        }
    }

    public Future<Void> extendPeerGroupAsync() {
        return CompletableFuture.runAsync(() -> {
            ThreadName.set(this, "extendPeerGroup");
            extendPeerGroup();
        }, NETWORK_IO_POOL);
    }

    private void extendPeerGroup() {
        if (isShutdownInProgress) {
            return;
        }
        if (extendPeerGroupPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending extendPeerGroupPeerExchangeAttempt. We ignore the extendPeerGroup call.");
            return;
        }
        log.info("Extend peer group");
        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "extendPeerGroupPeerExchangeAttempt");
        extendPeerGroupPeerExchangeAttempt.set(Optional.of(attempt));
        List<Address> candidates = peerExchangeStrategy.getAddressesForExtendingPeerGroup();
        try {
            boolean success = attempt.startAsync(candidates.size() / 2, candidates).get();
            if (!success) {
                log.warn("extendPeerGroupPeerExchangeAttempt completed unsuccessful.");
            }
        } catch (Exception e) {
            log.warn("extendPeerGroupPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));
        }
        extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
    }

    private Future<Void> retryPeerExchangeAsync() {
        return CompletableFuture.runAsync(() -> {
            ThreadName.set(this, "retryPeerExchange");
            retryPeerExchange();
        }, NETWORK_IO_POOL);
    }

    private void retryPeerExchange() {
        if (isShutdownInProgress) {
            return;
        }
        if (retryPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending retryPeerExchangeAttempt. We ignore the retryPeerExchange call.");
            return;
        }
        if (numRetryAttempts.get() > MAX_RETRY_ATTEMPTS) {
            log.warn("We have retried the peer exchange {} times without success and give up.", MAX_RETRY_ATTEMPTS);
            return;
        }
        long delay = 1000L * numRetryAttempts.get() * numRetryAttempts.get();
        if (delay > 0) {
            try {
                log.info("Retry peer exchange after {} sec.", delay / 1000);
                Thread.sleep(delay);
            } catch (InterruptedException ignore) {
            }
        } else {
            log.info("Retry peer exchange");
        }
        numRetryAttempts.incrementAndGet();

        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "retryPeerExchangeAttempt");
        retryPeerExchangeAttempt.set(Optional.of(attempt));
        List<Address> candidates = peerExchangeStrategy.getAddressesForRetryPeerExchange();
        try {
            boolean success = attempt.startAsync(candidates.size() / 2, candidates).get();
            if (!success) {
                log.warn("retryPeerExchangeAttempt completed unsuccessful.");
                retryPeerExchangeAttempt.set(Optional.empty());
                retryPeerExchangeAsync();
            }
        } catch (Exception e) {
            log.warn("retryPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));
            retryPeerExchangeAttempt.set(Optional.empty());
            retryPeerExchangeAsync();
        }
    }
}