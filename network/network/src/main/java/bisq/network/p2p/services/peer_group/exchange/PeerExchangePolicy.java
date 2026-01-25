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
import bisq.common.util.CollectionUtil;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Defines the policy and logic for selecting, managing, and retrying peer exchanges within the peer-to-peer network.
 * <p>
 * This class encapsulates the decision-making criteria for which peer addresses to use during initial exchanges,
 * retries, and peer group extension phases. It also manages address usage tracking to avoid repeated attempts,
 * and applies limits and thresholds for retry delays and peer reporting.
 * </p>
 *
 * <h2>Policy Overview:</h2>
 * <ul>
 *   <li><b>Address Selection:</b> Provides prioritized lists of peer addresses for initial peer exchange, retry attempts,
 *       and extending the peer group. These lists combine seed nodes, reported peers, persisted peers, and currently connected peers,
 *       filtered and shuffled to maximize connection diversity and efficiency.</li>
 *   <li><b>Address Usage Tracking:</b> Maintains a set of addresses already used in peer exchange attempts to avoid redundant retries.
 *       If no new candidates are available, the set is cleared to allow retrying previously used peers, assuming network state changes.</li>
 *   <li><b>Retry and Extension Conditions:</b> Determines whether to extend the peer group after initial exchange based on
 *       success results and candidate counts, and computes minimum success thresholds for retries and group extensions.</li>
 *   <li><b>Retry Delay Strategy:</b> Uses a capped quadratic backoff delay for retry attempts to avoid excessive network load.</li>
 *   <li><b>Peer Reporting:</b> Manages which peers are shared with requesters during peer reporting, excluding oracle nodes and respecting configured limits.</li>
 *   <li><b>Filtering Criteria:</b> Validates peers to exclude seeds, banned nodes, outdated peers, or self-addresses to ensure healthy peer sets.</li>
 * </ul>
 *
 * <h2>Configuration and Limits:</h2>
 * <ul>
 *   <li>Uses configurable parameters for numbers of seed nodes, reported peers, and persisted peers to include during bootstrap.</li>
 *   <li>Limits the number of reported peers shared during peer reporting to a maximum of 500.</li>
 *   <li>Restricts retry delays to a maximum of 60 seconds.</li>
 *   <li>Considers peers outdated if their age exceeds 5 days.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <ol>
 *   <li>Obtain prioritized peer address lists via {@link #getAddressesForInitialPeerExchange()},
 *       {@link #getAddressesForRetryPeerExchange()}, and {@link #getAddressesForExtendingPeerGroup()}.</li>
 *   <li>Check whether to extend the peer group after exchanges using {@link #shouldExtendAfterInitialExchange(Boolean, Throwable, List)}.</li>
 *   <li>Determine minimum success counts for retries or extensions with {@link #getMinSuccessForRetry(List)} and {@link #getMinSuccessForExtendPeerGroup(List)}.</li>
 *   <li>Use {@link #getRetryDelay(int)} to compute delays between retries based on retry counts.</li>
 *   <li>Retrieve and add peers for reporting using {@link #getPeersForReporting(Address)} and {@link #addReportedPeers(List, Address)}.</li>
 * </ol>
 *
 * <h2>Dependencies:</h2>
 * <ul>
 *   <li>{@link PeerGroupService} provides peer-related operations, such as retrieving seed nodes, reported peers, and banning checks.</li>
 *   <li>{@link Node} offers context about the current node, its connections, and identity.</li>
 *   <li>{@link PeerExchangeService.Config} contains configuration settings relevant to peer exchange behavior.</li>
 * </ul>
 */
@Slf4j
class PeerExchangePolicy {
    public static final long REPORTED_PEERS_LIMIT = 500;
    private static final long MAX_RETRY_DELAY = SECONDS.toMillis(60);
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(5);

    private final PeerGroupService peerGroupService;
    private final Node node;
    private final PeerExchangeService.Config config;
    private final PeerGroupService.Config peerGroupConfig;
    private final Set<Address> usedAddresses = new CopyOnWriteArraySet<>();

    PeerExchangePolicy(PeerGroupService peerGroupService, Node node,
                       PeerExchangeService.Config config,
                       PeerGroupService.Config peerGroupConfig) {
        this.peerGroupService = peerGroupService;
        this.node = node;
        this.config = config;
        this.peerGroupConfig = peerGroupConfig;

        PeerExchangeRequest.setMaxNumPeers(REPORTED_PEERS_LIMIT);
        PeerExchangeResponse.setMaxNumPeers(REPORTED_PEERS_LIMIT);
    }


    /* --------------------------------------------------------------------- */
    // Peer exchange
    /* --------------------------------------------------------------------- */

    List<Address> getAddressesForInitialPeerExchange() {
        List<Address> candidates = getCandidates(getPriorityListForInitialPeerExchange());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now.
            log.info("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForInitialPeerExchange());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    List<Address> getAddressesForRetryPeerExchange() {
        List<Address> candidates = getCandidates(getPriorityListForRetryPeerExchange());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to
            // send now.
            log.info("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForRetryPeerExchange());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    // After bootstrap, we might want to add more connections and use the peer exchange protocol for that.
    // We do not want to use seed nodes or already existing connections in that case.
    // Only if we do not have any candidates we add seed nodes.
    List<Address> getAddressesForExtendingPeerGroup() {
        List<Address> candidates = getCandidates(getPriorityListForExtendingPeerGroup());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now. We also add the seed nodes to get better chances for fresh nodes.
            log.debug("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForExtendingPeerGroup());
            candidates.addAll(getSeedAddresses());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    boolean shouldExtendAfterInitialExchange(Boolean result,
                                             Throwable throwable,
                                             List<Address> candidates) {
        // minNumConnectedPeers is 8 by default
        return result != null && result && candidates.size() < peerGroupConfig.getMinNumConnectedPeers();
    }

    int getMinSuccessForExtendPeerGroup(List<Address> candidates) {
        return Math.max(1, candidates.size() / 2);
    }

    int getMinSuccessForRetry(List<Address> candidates) {
        return Math.max(1, candidates.size() / 2);
    }

    long getRetryDelay(int numRetries) {
        return Math.min(MAX_RETRY_DELAY, 1000L * numRetries * numRetries);
    }


    /* --------------------------------------------------------------------- */
    // Reporting
    /* --------------------------------------------------------------------- */

    List<Peer> getPeersForReporting(Address requesterAddress) {
        // Oracle nodes run only a default node but receive requests from user-level nodes. If we include the connected
        // peers of oracle nodes we would mix up the gossip network with those user-level nodes. Therefor we set the
        // flag to supportPeerReporting false for oracle nodes and do not share our peers in peer reporting.
        if (!config.isSupportPeerReporting()) {
            return new ArrayList<>();
        }

        Set<Peer> connectedPeers = getSortedAllConnectedPeers()
                .filter(peer -> notSameAddress(requesterAddress, peer))
                .collect(Collectors.toSet());
        long maxSizeReportedPeers = Math.max(0L, REPORTED_PEERS_LIMIT - connectedPeers.size());
        Set<Peer> reportedPeers = getSortedReportedPeers()
                .filter(peer -> notSameAddress(requesterAddress, peer))
                .limit(maxSizeReportedPeers)
                .collect(Collectors.toSet());

        Set<Peer> peers = new HashSet<>(connectedPeers);
        peers.addAll(reportedPeers);
        return new ArrayList<>(peers);
    }

    void addReportedPeers(List<Peer> reportedPeers, Address reporterAddress) {
        Set<Peer> peers = new HashSet<>(reportedPeers).stream()
                .filter(peer -> notSameAddress(reporterAddress, peer))
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted()
                .limit(REPORTED_PEERS_LIMIT)
                .collect(Collectors.toSet());
        peerGroupService.addReportedPeers(peers);
        peerGroupService.addPersistedPeers(peers);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private List<Address> getCandidates(List<Address> priorityList) {
        return priorityList.stream()
                .filter(this::isNotUsed)
                .distinct()
                .limit(getPeerExchangeLimit())
                .collect(Collectors.toList());
    }

    private int getPeerExchangeLimit() {
        int minNumConnectedPeers = peerGroupService.getMinNumConnectedPeers(); // default 8
        // We want at least 25% of minNumConnectedPeers
        int minValue = minNumConnectedPeers / 4;
        int missing = Math.max(0, peerGroupService.getTargetNumConnectedPeers() - node.getNumConnections());
        int limit = Math.max(minValue, missing);

        // In case we have enough connections but do not have received at least 25% of our
        // numReportedPeersAtBootstrap (default 10) target we still try to connect to 50% of minNumConnectedPeers.
        if (limit == minValue && peerGroupService.getReportedPeers().size() < config.getNumReportedPeersAtBootstrap() / 4) {
            return minNumConnectedPeers / 2;
        }
        return limit;
    }

    private List<Address> getPriorityListForInitialPeerExchange() {
        List<Address> priorityList = new ArrayList<>(getSeedAddresses());
        priorityList.addAll(getReportedPeerAddresses());
        priorityList.addAll(getPersistedAddresses());
        priorityList.addAll(getAllConnectedPeerAddresses());
        return priorityList;
    }

    private List<Address> getPriorityListForRetryPeerExchange() {
        List<Address> priorityList = new ArrayList<>(getSeedAddresses());
        priorityList.addAll(getReportedPeerAddresses());
        priorityList.addAll(getAllConnectedPeerAddresses());
        return priorityList;
    }

    private List<Address> getPriorityListForExtendingPeerGroup() {
        List<Address> priorityList = new ArrayList<>(getReportedPeerAddresses());
        priorityList.addAll(getPersistedAddresses());
        return priorityList;
    }

    private List<Address> getSeedAddresses() {
        return CollectionUtil.toShuffledList(peerGroupService.getSeedNodeAddresses()).stream()
                .filter(node::notMyself)
                .filter(peerGroupService::isNotBanned)
                .limit(config.getNumSeedNodesAtBootstrap())
                .collect(Collectors.toList());
    }

    private List<Address> getReportedPeerAddresses() {
        return getSortedReportedPeers()
                .limit(config.getNumReportedPeersAtBootstrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getPersistedAddresses() {
        return peerGroupService.getPersistedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted()
                .limit(config.getNumPersistedPeersAtBootstrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getAllConnectedPeerAddresses() {
        return getSortedAllConnectedPeers()
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    /* --------------------------------------------------------------------- */
    // Utils
    /* --------------------------------------------------------------------- */

    private boolean notASeed(Address address) {
        return !peerGroupService.isSeed(address);
    }

    private boolean notASeed(Peer peer) {
        return notASeed(peer.getAddress());
    }

    private boolean isValidNonSeedPeer(Address address) {
        return notASeed(address) &&
                peerGroupService.isNotBanned(address) &&
                node.notMyself(address);
    }

    private boolean isValidNonSeedPeer(Peer peer) {
        return isValidNonSeedPeer(peer.getAddress());
    }

    private boolean isNotOutDated(Peer peer) {
        return peer.getAge() < MAX_AGE;
    }

    private boolean isNotUsed(Peer peer) {
        return isNotUsed(peer.getAddress());
    }

    private boolean isNotUsed(Address address) {
        return !usedAddresses.contains(address);
    }

    private boolean notSameAddress(Address address, Peer peer) {
        return !peer.getAddress().equals(address);
    }

    private Stream<Peer> getSortedAllConnectedPeers() {
        return peerGroupService.getAllConnectedPeers(node)
                .filter(this::isValidNonSeedPeer)
                .sorted();
    }

    private Stream<Peer> getSortedReportedPeers() {
        return peerGroupService.getReportedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted();
    }
}
