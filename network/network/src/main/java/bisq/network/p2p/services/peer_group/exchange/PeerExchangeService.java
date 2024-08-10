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
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.NetworkService.NETWORK_IO_POOL;

/**
 * Responsible for executing the peer exchange protocol with set of peers.
 * We use the PeerExchangeStrategy for the selection of nodes.
 */
@Slf4j
@Getter
public class PeerExchangeService {
    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;

    private final PeerExchangeAttempt initialPeerExchangeAttempt;
    private final AtomicReference<Optional<PeerExchangeAttempt>> extendPeerGroupPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<PeerExchangeAttempt>> retryPeerExchangeAttempt = new AtomicReference<>(Optional.empty());
    private final AtomicInteger numRetryAttempts = new AtomicInteger();
    private volatile boolean isShutdownInProgress;
    private Pin initialPeerExchangeCompletedPin;

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;

        initialPeerExchangeAttempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "initialPeerExchangeAttempt");
    }

    public void shutdown() {
        if (isShutdownInProgress) {
            return;
        }
        isShutdownInProgress = true;

        if (initialPeerExchangeCompletedPin != null) {
            initialPeerExchangeCompletedPin.unbind();
            initialPeerExchangeCompletedPin = null;
        }
        if (!isInitialPeerExchangeCompleted()) {
            initialPeerExchangeAttempt.shutdown();
        }
        extendPeerGroupPeerExchangeAttempt.get().filter(attempt -> !attempt.getCompleted().get().get()).ifPresent(PeerExchangeAttempt::shutdown);
        retryPeerExchangeAttempt.get().filter(attempt -> !attempt.getCompleted().get().get()).ifPresent(PeerExchangeAttempt::shutdown);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void startInitialPeerExchange() {
        log.info("Start initial peer exchange");
        if (initialPeerExchangeCompletedPin != null) {
            initialPeerExchangeCompletedPin.unbind();
        }
        initialPeerExchangeCompletedPin = initialPeerExchangeAttempt.getCompleted().get().addObserver(completed -> {
            if (completed != null && completed) {
                initialPeerExchangeCompletedPin.unbind();
                initialPeerExchangeCompletedPin = null;
                if (initialPeerExchangeAttempt.getRequireRetry().get()) {
                    NETWORK_IO_POOL.submit(this::retryPeerExchange);
                }
            }
        });

        // We block until at least 1 peer exchange succeeded
        List<Address> candidates = peerExchangeStrategy.getAddressesForInitialPeerExchange();
        try {
            initialPeerExchangeAttempt.start(2, candidates).join();
        } catch (Exception e) {
            log.warn("Initial peer exchange failed {}", ExceptionUtil.getRootCauseMessage(e));
            NETWORK_IO_POOL.submit(this::retryPeerExchange);
        }
    }

    public void extendPeerGroup() {
        if (!isInitialPeerExchangeCompleted()) {
            log.debug("initialPeerExchangeAttempt is not completed yet. We ignore the extendPeerGroup call.");
            return;
        }
        if (extendPeerGroupPeerExchangeAttempt.get().isPresent()) {
            log.debug("We have a pending extendPeerGroupPeerExchangeAttempt. We ignore the extendPeerGroup call.");
            return;
        }
        log.info("Extend peer group");
        PeerExchangeAttempt attempt = new PeerExchangeAttempt(node, peerExchangeStrategy, "extendPeerGroupPeerExchangeAttempt");
        extendPeerGroupPeerExchangeAttempt.set(Optional.of(attempt));
        List<Address> candidates = peerExchangeStrategy.getAddressesForExtendingPeerGroup();
        try {
            attempt.start(candidates.size(), candidates).join();
        } catch (Exception e) {
            log.warn("extendPeerGroupPeerExchangeAttempt failed {}", ExceptionUtil.getRootCauseMessage(e));
        } finally {
            extendPeerGroupPeerExchangeAttempt.set(Optional.empty());
        }
    }

    private void retryPeerExchange() {
        if (!isInitialPeerExchangeCompleted()) {
            log.debug("initialPeerExchangeAttempt is not completed yet. We ignore the retryPeerExchange call.");
            return;
        }
        if (retryPeerExchangeAttempt.get().isPresent()) {
            log.debug("We have a pending retryPeerExchangeAttempt. We ignore the retryPeerExchange call.");
            return;
        }
        long delay = 10L * numRetryAttempts.get();
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
            attempt.start(candidates.size(), candidates).join();
        } catch (Exception e) {
            log.warn("retryPeerExchangeAttempt failed {}", ExceptionUtil.getRootCauseMessage(e));
        } finally {
            boolean requireRetry = attempt.getRequireRetry().get();
            retryPeerExchangeAttempt.set(Optional.empty());
            if (requireRetry) {
                NETWORK_IO_POOL.submit(this::retryPeerExchange);
            }
        }
    }

    private boolean isInitialPeerExchangeCompleted() {
        return initialPeerExchangeAttempt.getCompleted().get().get();
    }
}