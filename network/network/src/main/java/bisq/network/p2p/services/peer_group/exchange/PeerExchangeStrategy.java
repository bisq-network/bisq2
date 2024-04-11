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

import bisq.network.common.Address;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PeerExchangeStrategy {
    public static final long REPORTED_PEERS_LIMIT = 200;
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(5);

    @Getter
    public static class Config {
        private final int numSeedNodesAtBoostrap;
        private final int numPersistedPeersAtBoostrap;
        private final int numReportedPeersAtBoostrap;

        public Config() {
            this(2, 40, 20);
        }

        public Config(int numSeedNodesAtBoostrap,
                      int numPersistedPeersAtBoostrap,
                      int numReportedPeersAtBoostrap) {
            this.numSeedNodesAtBoostrap = numSeedNodesAtBoostrap;
            this.numPersistedPeersAtBoostrap = numPersistedPeersAtBoostrap;
            this.numReportedPeersAtBoostrap = numReportedPeersAtBoostrap;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new PeerExchangeStrategy.Config(
                    typesafeConfig.getInt("numSeedNodesAtBoostrap"),
                    typesafeConfig.getInt("numPersistedPeersAtBoostrap"),
                    typesafeConfig.getInt("numReportedPeersAtBoostrap"));
        }
    }

    private final PeerGroupService peerGroupService;
    private final Node node;
    private final Config config;
    private final Set<Address> usedAddresses = new CopyOnWriteArraySet<>();

    public PeerExchangeStrategy(PeerGroupService peerGroupService, Node node, Config config) {
        this.peerGroupService = peerGroupService;
        this.node = node;
        this.config = config;

        PeerExchangeRequest.setMaxNumPeers(REPORTED_PEERS_LIMIT);
        PeerExchangeResponse.setMaxNumPeers(REPORTED_PEERS_LIMIT);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Peer exchange
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    // After bootstrap, we might want to add more connections and use the peer exchange protocol for that.
    // We do not want to use seed nodes or already existing connections in that case.
    List<Address> getAddressesForExtendingPeerGroup() {
        List<Address> candidates = getCandidates(getPriorityListForExtendingPeerGroup());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now.
            log.debug("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForExtendingPeerGroup());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    boolean shouldRedoInitialPeerExchange(int numSuccess, int numRequests) {
        int numFailed = numRequests - numSuccess;
        int maxFailures = numRequests / 2;
        return numFailed > maxFailures ||
                peerGroupService.getAllConnectedPeers(node).count() < peerGroupService.getTargetNumConnectedPeers() ||
                peerGroupService.getReportedPeers().size() < peerGroupService.getMinNumReportedPeers();
    }

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
        // numReportedPeersAtBoostrap (default 10) target we still try to connect to 50% of minNumConnectedPeers.
        if (limit == minValue && peerGroupService.getReportedPeers().size() < config.getNumReportedPeersAtBoostrap() / 4) {
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

    private List<Address> getPriorityListForExtendingPeerGroup() {
        List<Address> priorityList = new ArrayList<>(getReportedPeerAddresses());
        priorityList.addAll(getPersistedAddresses());
        return priorityList;
    }

    private List<Address> getSeedAddresses() {
        return getShuffled(peerGroupService.getSeedNodeAddresses()).stream()
                .filter(node::notMyself)
                .filter(peerGroupService::isNotBanned)
                .limit(config.getNumSeedNodesAtBoostrap())
                .collect(Collectors.toList());
    }

    private List<Address> getReportedPeerAddresses() {
        return getReportedPeers()
                .limit(config.getNumReportedPeersAtBoostrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getPersistedAddresses() {
        return peerGroupService.getPersistedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted()
                .limit(config.getNumPersistedPeersAtBoostrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getAllConnectedPeerAddresses() {
        return getAllConnectedPeers()
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Reporting
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Set<Peer> getPeersForReporting(Address requesterAddress) {
        long hiPriorityLimit = Math.round(REPORTED_PEERS_LIMIT * 0.75);
        Set<Peer> connectedPeers = getAllConnectedPeers()
                .filter(peer -> notSameAddress(requesterAddress, peer))
                .limit(hiPriorityLimit)
                .collect(Collectors.toSet());

        long lowPriorityLimit = Math.round(REPORTED_PEERS_LIMIT * 0.25);
        Set<Peer> reportedPeers = getReportedPeers()
                .filter(peer -> notSameAddress(requesterAddress, peer))
                .limit(lowPriorityLimit)
                .collect(Collectors.toSet());

        Set<Peer> peers = new HashSet<>(connectedPeers);
        peers.addAll(reportedPeers);
        return peers;
    }

    void addReportedPeers(Set<Peer> reportedPeers, Address reporterAddress) {
        Set<Peer> peers = reportedPeers.stream()
                .filter(peer -> notSameAddress(reporterAddress, peer))
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted()
                .limit(REPORTED_PEERS_LIMIT)
                .collect(Collectors.toSet());
        peerGroupService.addReportedPeers(peers);
        peerGroupService.addPersistedPeers(peers);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    private Stream<Peer> getAllConnectedPeers() {
        return peerGroupService.getAllConnectedPeers(node)
                .filter(this::isValidNonSeedPeer)
                .sorted();
    }

    private Stream<Peer> getReportedPeers() {
        return peerGroupService.getReportedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotOutDated)
                .sorted();
    }

    private List<Address> getShuffled(Collection<Address> addresses) {
        List<Address> list = new ArrayList<>(addresses);
        Collections.shuffle(list);
        return list;
    }
}
