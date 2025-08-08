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

import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the policy and tracking logic for a batched peer exchange based on the outcomes of individual peer exchanges.
 * <p>
 * This class keeps track of the number of successful and failed peer exchanges, evaluates whether
 * a minimum success threshold has been reached, and determines if further retries of peer exchange are needed.
 * </p>
 *
 * <h2>Policy Summary:</h2>
 * <ul>
 *   <li><b>Success Tracking:</b> Each peer exchange result is tracked as a success or failure using {@link #trackSuccess(Throwable)}.</li>
 *   <li><b>Minimum Success Threshold:</b> The policy maintains whether a configured minimum number of successful peer exchanges
 *       has been reached. Once reached, it updates an observable flag to signal this state.</li>
 *   <li><b>Retry Decision:</b> The policy decides whether to retry peer exchange operations based on:
 *     <ul>
 *       <li>Failures exceeding allowed limits.</li>
 *       <li>Whether more peer connections or reported peers are needed.</li>
 *       <li>Whether the minimum success threshold has been met.</li>
 *     </ul>
 *   </li>
 *   <li><b>Logging:</b> Detailed logs are produced at important decision points to aid in debugging and operational insight.</li>
 * </ul>
 *
 * <h2>Key Fields:</h2>
 * <ul>
 *   <li>{@code numSuccess} - Tracks the count of successful peer exchanges.</li>
 *   <li>{@code numFailures} - Tracks the count of failed peer exchanges.</li>
 *   <li>{@code numMinSuccess} - The minimum number of successes required to consider the exchange satisfactory.</li>
 *   <li>{@code candidatesSize} - The total number of peer candidates considered in the exchange.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <ol>
 *   <li>Call {@link #trackSuccess(Throwable)} for each peer exchange result.</li>
 *   <li>Use {@link #wasMinSuccessReached()} to check if minimum success has been reached.</li>
 *   <li>Invoke {@link #requiresRetry(Throwable)} to determine if peer exchange should be retried based on current state.</li>
 * </ol>
 */
@Slf4j
class BatchedPeerExchangePolicy {
    private final AtomicInteger numSuccess = new AtomicInteger();
    private final AtomicInteger numFailures = new AtomicInteger();
    private final PeerGroupService peerGroupService;
    private final Node node;
    private final int numMinSuccess;
    private final int candidatesSize;

    BatchedPeerExchangePolicy(PeerGroupService peerGroupService, Node node, int numMinSuccess, int candidatesSize) {
        this.peerGroupService = peerGroupService;
        this.node = node;
        this.numMinSuccess = numMinSuccess;
        this.candidatesSize = candidatesSize;
    }

    void trackSuccess(Throwable throwable) {
        if (throwable == null) {
            numSuccess.incrementAndGet();
            boolean isMinSuccessReached = numSuccess.get() == numMinSuccess;
            if (isMinSuccessReached) {
                log.info("Min. success reached at initial peer exchange.\nnumSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}.",
                        numSuccess.get(), numFailures.get(), numMinSuccess, candidatesSize);
            }
        } else {
            numFailures.incrementAndGet();
        }
    }

    boolean wasMinSuccessReached() {
        return numSuccess.get() >= numMinSuccess;
    }

    boolean requiresRetry(Throwable throwable) {
        if (throwable != null) {
            log.debug("Peer exchange failed. numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}",
                    numSuccess.get(), numFailures.get(), numMinSuccess, candidatesSize, throwable);
            return true;
        }

        log.info("Peer exchange completed. numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}.",
                numSuccess.get(), numFailures.get(), numMinSuccess, candidatesSize);

        boolean tooManyFailures = tooManyFailures(numSuccess.get(), numFailures.get());
        if (tooManyFailures) {
            log.info("Require retry of peer exchange because of: tooManyFailures.");
            return true;
        }

        boolean needsMoreConnections = needsMoreConnections();
        if (needsMoreConnections) {
            log.info("Require retry of peer exchange because of: needsMoreConnections.");
            return true;
        }

        boolean needsMoreReportedPeers = needsMoreReportedPeers();
        if (needsMoreReportedPeers) {
            log.info("Require retry of peer exchange because of: needsMoreReportedPeers.");
            return true;
        }

        boolean isMinSuccessReached = numSuccess.get() >= numMinSuccess;
        if (!isMinSuccessReached) {
            log.info("All peer exchange completed but minSuccessReached not reached.\n" +
                            "numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}.",
                    numSuccess.get(), numFailures.get(), numMinSuccess, candidatesSize);
            return true;
        }

        log.info("We stop our peer exchange as we have sufficient connections established.");
        return false;
    }

    private boolean tooManyFailures(int numSuccess, int numFailures) {
        int numRequests = numSuccess + numFailures;
        int maxFailures = numRequests / 2;
        return numFailures > maxFailures;
    }

    private boolean needsMoreReportedPeers() {
        return peerGroupService.getReportedPeers().size() < peerGroupService.getMinNumReportedPeers();
    }

    private boolean needsMoreConnections() {
        return peerGroupService.getAllConnectedPeers(node).count() < peerGroupService.getTargetNumConnectedPeers();
    }
}
