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

package bisq.network.p2p.services.peergroup.exchange;

import bisq.network.p2p.node.Address;
import bisq.network.p2p.services.peergroup.Peer;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PeerExchangeStrategy {
    private static final long REPORTED_PEERS_LIMIT = 200;
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

    private final PeerGroup peerGroup;
    private final Config config;
    private final PeerGroupStore peerGroupStore;
    private final Set<Address> usedAddresses = new CopyOnWriteArraySet<>();

    public PeerExchangeStrategy(PeerGroup peerGroup, Config config, PeerGroupStore peerGroupStore) {
        this.peerGroup = peerGroup;
        this.config = config;
        this.peerGroupStore = peerGroupStore;
    }

    void shutdown() {
        usedAddresses.clear();
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
    List<Address> getAddressesForFurtherPeerExchange() {
        List<Address> candidates = getCandidates(getPriorityListForFurtherPeerExchange());
        if (candidates.isEmpty()) {
            // It can be that we don't have peers anymore which we have not already connected in the past.
            // We reset the usedAddresses and try again. It is likely that some peers have different peers to 
            // send now.
            log.debug("We reset the usedAddresses and try again to connect to peers we tried in the past.");
            usedAddresses.clear();
            candidates = getCandidates(getPriorityListForFurtherPeerExchange());
        }
        usedAddresses.addAll(candidates);
        return candidates;
    }

    private List<Address> getPriorityListForInitialPeerExchange() {
        List<Address> priorityList = new ArrayList<>(getSeeds());
        priorityList.addAll(getReportedPeerAddresses());
        priorityList.addAll(getPersisted());
        priorityList.addAll(getAllConnectedPeerAddresses());
        return getDistictList(priorityList);
    }

    private List<Address> getPriorityListForFurtherPeerExchange() {
        List<Address> priorityList = new ArrayList<>(getReportedPeerAddresses());
        priorityList.addAll(getPersisted());
        return getDistictList(priorityList);
    }

    private List<Address> getSeeds() {
        return getShuffled(peerGroup.getSeedNodeAddresses()).stream()
                .filter(peerGroup::notMyself)
                .filter(peerGroup::isNotBanned)
                .filter(this::isNotUsed)
                .limit(config.getNumSeedNodesAtBoostrap())
                .collect(Collectors.toList());
    }

    private List<Address> getReportedPeerAddresses() {
        return getReportedPeers()
                .filter(this::isNotUsed)
                .limit(config.getNumReportedPeersAtBoostrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getPersisted() {
        return peerGroupStore.getPersistedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotUsed)
                .sorted(Comparator.comparing(Peer::getDate))
                .limit(config.getNumPersistedPeersAtBoostrap())
                .map(Peer::getAddress)
                .collect(Collectors.toList());
    }

    private List<Address> getAllConnectedPeerAddresses() {
        return getAllConnectedPeers()
                .filter(this::isNotUsed)
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
        Set<Peer> filtered = reportedPeers.stream()
                .filter(peer -> notSameAddress(reporterAddress, peer))
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotAged)
                .sorted(Comparator.comparing(Peer::getDate).reversed())
                .limit(REPORTED_PEERS_LIMIT)
                .collect(Collectors.toSet());
        peerGroup.addReportedPeers(filtered);
    }

    boolean shouldRedoInitialPeerExchange(int numSuccess, int numRequests) {
        boolean moreThenHalfFailed = numRequests - numSuccess > numRequests / 2;
        return moreThenHalfFailed ||
                !sufficientConnections() ||
                !sufficientReportedPeers();
    }

    private boolean sufficientConnections() {
        return peerGroup.getAllConnectedPeers().count() >= peerGroup.getMinNumConnectedPeers();
    }

    private boolean sufficientReportedPeers() {
        return peerGroup.getReportedPeers().size() >= peerGroup.getMinNumReportedPeers();
    }

    private boolean notASeed(Address address) {
        return !peerGroup.isASeed(address);
    }

    private boolean notASeed(Peer peer) {
        return notASeed(peer.getAddress());
    }

    private boolean isValidNonSeedPeer(Address address) {
        return notASeed(address) &&
                peerGroup.isNotBanned(address) &&
                peerGroup.notMyself(address);
    }

    private boolean isValidNonSeedPeer(Peer peer) {
        return isValidNonSeedPeer(peer.getAddress());
    }

    private boolean isNotAged(Peer peer) {
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
        return peerGroup.getAllConnectedPeers()
                .filter(this::isValidNonSeedPeer)
                .sorted(Comparator.comparing(peer -> peer.getLoad().getNumConnections()))
                .sorted(Comparator.comparing(Peer::getDate).reversed());
    }

    private Stream<Peer> getReportedPeers() {
        return peerGroup.getReportedPeers().stream()
                .filter(this::isValidNonSeedPeer)
                .filter(this::isNotAged)
                .sorted(Comparator.comparing(Peer::getDate).reversed());
    }

    private List<Address> getCandidates(List<Address> priorityList) {
        return priorityList.stream()
                .limit(getLimit())
                .collect(Collectors.toList());
    }

    private int getLimit() {
        int minNumConnectedPeers = peerGroup.getMinNumConnectedPeers();
        // We want at least 25% of minNumConnectedPeers
        int minValue = minNumConnectedPeers / 4;
        int limit = Math.max(minValue, peerGroup.getTargetNumConnectedPeers() - peerGroup.getNumConnections());

        // In case we have enough connections but do not have received at least 25% of our numReportedPeersAtBoostrap 
        // target we still try to connect to 50% of minNumConnectedPeers.
        if (limit == minValue && peerGroup.getReportedPeers().size() < config.getNumReportedPeersAtBoostrap() / 4) {
            return minNumConnectedPeers / 2;
        }
        return limit;
    }

    private List<Address> getShuffled(Collection<Address> addresses) {
        List<Address> list = new ArrayList<>(addresses);
        Collections.shuffle(list);
        return list;
    }

    private static List<Address> getDistictList(List<Address> list) {
        return list.stream().distinct().collect(Collectors.toList());
    }
}
