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
import java.util.concurrent.Future;
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
    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;

    private final PeerExchangeAttempt initialPeerExchangeAttempt;
    private final AtomicReference<Optional<PeerExchangeAttempt>> extendPeerGroupPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<PeerExchangeAttempt>> retryPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicInteger numRetryAttempts = new AtomicInteger();
    private volatile boolean isShutdownInProgress;
    private Pin initialExchangePin, retryExchangePin;

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
        if (initialExchangePin != null) {
            initialExchangePin.unbind();
            initialExchangePin = null;
        }
        if (retryExchangePin != null) {
            retryExchangePin.unbind();
            retryExchangePin = null;
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
            NETWORK_IO_POOL.submit(() -> node.send(new PeerExchangeResponse(request.getNonce(), myPeers), connection));
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
        if (initialExchangePin != null) {
            initialExchangePin.unbind();
        }
        initialExchangePin = initialPeerExchangeAttempt.getCompleted().get().addObserver(completed -> {
            if (completed != null && completed) {
                initialExchangePin.unbind();
                initialExchangePin = null;
                if (initialPeerExchangeAttempt.getRequireRetry().get()) {
                    // We use here a blocking call to retry, so that peer manager does not continue before we got at least one peer exchange successful
                    retryPeerExchange(1);
                }
            }
        });

        // We block until at least 1 peer exchange succeeded
        List<Address> candidates = peerExchangeStrategy.getAddressesForInitialPeerExchange();
        try {
            initialPeerExchangeAttempt.start(1, candidates).join();
            log.info("Initial peer exchange succeeded with 1 exchange. " +
                    "We start a parallel peerExchangeAttempt for faster connection establishment.");

            NETWORK_IO_POOL.submit(() -> retryPeerExchange(-1));
        } catch (Exception e) {
            log.warn("Initial peer exchange failed. {}", ExceptionUtil.getRootCauseMessage(e));
            if (initialExchangePin != null) {
                initialExchangePin.unbind();
            }
            // We use here a blocking call to retry, so that peer manager does not continue before we got at least one peer exchange successful
            retryPeerExchange(1);
        }
    }

    public Future<Void> extendPeerGroupAsync() {
        return CompletableFuture.runAsync(this::extendPeerGroup, NETWORK_IO_POOL);
    }

    private void extendPeerGroup() {
        if (extendPeerGroupPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending extendPeerGroupPeerExchangeAttempt. We ignore the extendPeerGroup call.");
            return;
        }
        log.info("Extend peer group");
        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "extendPeerGroupPeerExchangeAttempt");
        extendPeerGroupPeerExchangeAttempt.set(Optional.of(attempt));
        List<Address> candidates = peerExchangeStrategy.getAddressesForExtendingPeerGroup();
        try {
            attempt.start(candidates.size(), candidates).join();
        } catch (Exception e) {
            log.warn("extendPeerGroupPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));
        } finally {
            extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
        }
    }

    private void retryPeerExchange(int minSuccess) {
        if (retryPeerExchangeAttempt.get().isPresent()) {
            log.info("We have a pending retryPeerExchangeAttempt. We ignore the retryPeerExchange call.");
            return;
        }
        if (numRetryAttempts.get() > 10) {
            log.warn("We have retried the peer exchange 10 times without success and give up.");
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

        if (retryExchangePin != null) {
            retryExchangePin.unbind();
        }
        retryExchangePin = attempt.getCompleted().get().addObserver(completed -> {
            if (completed != null && completed) {
                retryExchangePin.unbind();
                retryExchangePin = null;

                retryPeerExchangeAttempt.set(Optional.empty());
                if (attempt.getRequireRetry().get()) {
                    NETWORK_IO_POOL.submit(() -> retryPeerExchange(-1));
                }
            }
        });

        try {
            int numMinSuccess = minSuccess > 0 ? minSuccess : candidates.size();
            attempt.start(numMinSuccess, candidates).join();
        } catch (Exception e) {
            log.warn("retryPeerExchangeAttempt failed. {}", ExceptionUtil.getRootCauseMessage(e));

            if (retryExchangePin != null) {
                retryExchangePin.unbind();
            }
            retryPeerExchangeAttempt.set(Optional.empty());
            if (attempt.getRequireRetry().get()) {
                NETWORK_IO_POOL.submit(() -> retryPeerExchange(-1));
            }
        }
    }
}