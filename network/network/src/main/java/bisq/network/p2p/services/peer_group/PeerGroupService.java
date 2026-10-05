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

package bisq.network.p2p.services.peer_group;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.util.CollectionUtil;
import bisq.common.util.StringUtils;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintains different collections of peers and connections.
 * The reported and the persisted peers come from other nodes. This service keeps only peers of its own transport, and
 * keeps at most the configured number of peers in each collection. See docs/specifications/network/peer-exchange.md.
 */
@Slf4j
public class PeerGroupService extends RateLimitedPersistenceClient<PeerGroupStore> {
    private static final long MIN_PRINT_INTERVAL = TimeUnit.MINUTES.toMillis(10);
    // A reporter dates the peer of a connection with the creation date of the connection plus up to 3 seconds, so a
    // date can be a little in our future, also because the clocks differ. The limits keep the newest peers, so a peer
    // dated further in the future would rank above all honest peers. We do not keep such peers.
    @VisibleForTesting
    static final long MAX_DATE_IN_FUTURE = TimeUnit.MINUTES.toMillis(1);

    @Getter
    public static class Config {
        private final int minNumConnectedPeers;
        private final int minNumOutboundConnectedPeers;
        private final int maxNumConnectedPeers;
        private final int minNumReportedPeers;

        public Config() {
            this(8, 3, 12, 1);
        }

        public Config(int minNumConnectedPeers,
                      int minNumOutboundConnectedPeers,
                      int maxNumConnectedPeers,
                      int minNumReportedPeers) {
            this.minNumConnectedPeers = minNumConnectedPeers;
            this.minNumOutboundConnectedPeers = minNumOutboundConnectedPeers;
            this.maxNumConnectedPeers = maxNumConnectedPeers;
            this.minNumReportedPeers = minNumReportedPeers;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig,
                                  TransportType transportType,
                                  Set<TransportType> supportedTransportTypes) {
            com.typesafe.config.Config config;
            if (supportedTransportTypes.size() > 1 && typesafeConfig.hasPath("multipleTransports")) {
                String transportTypeName = transportType.name().toLowerCase(Locale.ROOT);
                com.typesafe.config.Config multipleTransports = typesafeConfig.getConfig("multipleTransports");
                // If a transport specific node and field is available we override the base field
                config = multipleTransports.hasPath(transportTypeName)
                        ? multipleTransports.getConfig(transportTypeName).withFallback(typesafeConfig)
                        : typesafeConfig;
            } else {
                config = typesafeConfig;
            }
            int maxNumConnectedPeers = config.getInt("maxNumConnectedPeers");
            int minNumConnectedPeers = config.getInt("minNumConnectedPeers");
            int minNumOutboundConnectedPeers = config.getInt("minNumOutboundConnectedPeers");
            int minNumReportedPeers = config.getInt("minNumReportedPeers");
            return new PeerGroupService.Config(
                    minNumConnectedPeers,
                    minNumOutboundConnectedPeers,
                    maxNumConnectedPeers,
                    minNumReportedPeers);
        }
    }

    @Getter
    private final Persistence<PeerGroupStore> persistence;
    @Getter
    private final PeerGroupStore persistableStore = new PeerGroupStore();
    private final Config config;
    @Getter
    private final Set<Address> seedNodeAddresses;
    private final BanList banList;
    private final TransportType transportType;
    private final int maxReportedPeers;
    private final int maxPersistedPeers;
    private final Map<Address, Peer> reportedPeersByAddress = new ConcurrentHashMap<>();
    private long lastReportTs;
    private volatile boolean persistAfterLoad;

    public PeerGroupService(PersistenceService persistenceService,
                            TransportType transportType,
                            Config config,
                            Set<Address> seedNodeAddresses,
                            BanList banList,
                            int maxReportedPeers,
                            int maxPersistedPeers) {
        this.config = config;
        this.seedNodeAddresses = seedNodeAddresses.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
        this.banList = banList;
        this.transportType = transportType;
        this.maxReportedPeers = maxReportedPeers;
        this.maxPersistedPeers = maxPersistedPeers;

        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.SETTINGS,
                transportType.name().toLowerCase(Locale.ROOT) + persistableStore.getClass().getSimpleName(),
                persistableStore);
    }

    // Earlier versions kept peers of all transports and did not limit the number of persisted clearnet peers. So the
    // persisted store can be larger than the limit, and we apply the rules when we load it.
    @Override
    public PeerGroupStore prunePersisted(PeerGroupStore persisted) {
        Map<Address, Peer> persistedPeersByAddress = persisted.getPersistedPeersByAddress();
        Map<Address, Peer> prunedPeersByAddress = persistedPeersByAddress.values().stream()
                .filter(this::isStorable)
                .sorted()
                .limit(maxPersistedPeers)
                .collect(Collectors.toMap(Peer::getAddress, peer -> peer));
        int numRemoved = persistedPeersByAddress.size() - prunedPeersByAddress.size();
        if (numRemoved > 0) {
            log.info("We removed {} of {} persisted {} peers which are not of our transport, are dated in the future " +
                    "or exceed the limit of {} peers", numRemoved, persistedPeersByAddress.size(), transportType, maxPersistedPeers);
            persistAfterLoad = true;
        }
        return new PeerGroupStore(prunedPeersByAddress);
    }

    @Override
    public void onPersistedApplied(PeerGroupStore persisted) {
        // We write the pruned store, so that the file does not keep the removed peers until the next change
        if (persistAfterLoad) {
            persistAfterLoad = false;
            persist();
        }
    }

    /* --------------------------------------------------------------------- */
    // Persisted peers
    /* --------------------------------------------------------------------- */

    public Map<Address, Peer> getPersistedPeersByAddress() {
        return persistableStore.getPersistedPeersByAddress();
    }

    public Set<Peer> getPersistedPeers() {
        return new HashSet<>(getPersistedPeersByAddress().values());
    }

    public boolean addPersistedPeer(Peer peer) {
        return addPersistedPeers(Set.of(peer));
    }

    // Adds the peers which we can store, and keeps the newest maxSize peers of the map
    private boolean addPeers(Set<Peer> peers, Map<Address, Peer> map, int maxSize) {
        synchronized (map) {
            boolean wasAdded = false;
            for (Peer peer : peers) {
                if (isStorable(peer) && doAddPeer(peer, map)) {
                    wasAdded = true;
                }
            }
            if (map.size() > maxSize) {
                map.values().stream()
                        .sorted()
                        .skip(maxSize)
                        .toList()
                        .forEach(oldest -> map.remove(oldest.getAddress()));
            }
            return wasAdded;
        }
    }

    // A peer of another transport is useless for the nodes of our transport. A peer dated in the future could push
    // out honest peers, because the limits keep the newest peers.
    private boolean isStorable(Peer peer) {
        return peer.getAddress().getTransportType() == transportType &&
                peer.getCreated() <= System.currentTimeMillis() + MAX_DATE_IN_FUTURE;
    }

    private boolean doAddPeer(Peer peerToAdd, Map<Address, Peer> map) {
        Address address = peerToAdd.getAddress();
        if (map.containsKey(address)) {
            if (peerToAdd.getCreated() > map.get(address).getCreated()) {
                map.put(address, peerToAdd);
                return true;
            } else {
                return false;
            }
        } else {
            map.put(address, peerToAdd);
            return true;
        }
    }

    public boolean addPersistedPeers(Set<Peer> peers) {
        boolean wasAdded = addPeers(peers, getPersistedPeersByAddress(), maxPersistedPeers);
        if (wasAdded) {
            maybePrintReportedPeers(getPersistedPeersByAddress(), "Persisted");
            persist();
        }
        return wasAdded;
    }

    public void removePersistedPeers(Collection<Peer> peers) {
        Map<Address, Peer> persistedPeersById = getPersistedPeersByAddress();
        peers.forEach(peer -> persistedPeersById.remove(peer.getAddress()));
        if (!peers.isEmpty()) {
            persist();
        }
    }

    public void clearPersistedPeers() {
        getPersistedPeersByAddress().clear();
        persist();
    }


    /* --------------------------------------------------------------------- */
    // Reported peers
    /* --------------------------------------------------------------------- */

    public Set<Peer> getReportedPeers() {
        return new HashSet<>(reportedPeersByAddress.values());
    }

    public boolean addReportedPeers(Set<Peer> peers) {
        boolean wasAdded = addPeers(peers, reportedPeersByAddress, maxReportedPeers);
        if (wasAdded) {
            maybePrintReportedPeers(reportedPeersByAddress, "Reported");
        }
        return wasAdded;
    }

    public void removeReportedPeers(Collection<Peer> peers) {
        peers.forEach(peer -> reportedPeersByAddress.remove(peer.getAddress()));
    }

    public void clearReportedPeers() {
        reportedPeersByAddress.clear();
    }


    /* --------------------------------------------------------------------- */
    // Connections
    /* --------------------------------------------------------------------- */

    public boolean isSeed(Connection connection) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(connection.getPeerAddress()));
    }

    public int getMinOutboundConnections() {
        return config.getMinNumOutboundConnectedPeers();
    }

    public int getMaxInboundConnections() {
        return config.getMaxNumConnectedPeers() - getMinOutboundConnections();
    }


    /* --------------------------------------------------------------------- */
    // Peers
    /* --------------------------------------------------------------------- */

    public Stream<Peer> getAllConnectedPeers(Node node) {
        return node.getAllActiveConnections().map(connection -> {
            long created = connection.getConnectionMetrics().getCreated();
            // We add some random time to avoid leaking information
            created += new Random().nextInt(3000);
            return new Peer(connection.getPeersCapability(),
                    connection.getPeersNetworkLoadSnapshot().getCurrentNetworkLoad(),
                    connection.isOutboundConnection(),
                    created);
        });
    }

    public Stream<Connection> getShuffledSeedConnections(Node node) {
        return CollectionUtil.toShuffledList(node.getAllActiveConnections()).stream()
                .filter(this::isSeed);
    }

    public Stream<Connection> getShuffledNonSeedConnections(Node node) {
        return CollectionUtil.toShuffledList(node.getAllActiveConnections()).stream()
                .filter(connection -> !isSeed(connection));
    }

    public boolean isNotBanned(Peer peer) {
        return isNotBanned(peer.getAddress());
    }

    public boolean isNotBanned(Address address) {
        return banList.isNotBanned(address);
    }

    public int getMinNumReportedPeers() {
        return config.getMinNumReportedPeers();
    }

    public int getMinNumConnectedPeers() {
        return config.getMinNumConnectedPeers();
    }

    public int getMaxNumConnectedPeers() {
        return config.getMaxNumConnectedPeers();
    }

    public int getTargetNumConnectedPeers() {
        return getMinNumConnectedPeers() + (getMaxNumConnectedPeers() - getMinNumConnectedPeers()) / 2;
    }


    /* --------------------------------------------------------------------- */
    // Address
    /* --------------------------------------------------------------------- */

    public void addSeedNodeAddress(Address seedNodeAddress) {
        if (seedNodeAddress != null) {
            this.seedNodeAddresses.add(seedNodeAddress);
        }
    }

    public void removeSeedNodeAddress(Address seedNodeAddress) {
        this.seedNodeAddresses.remove(seedNodeAddress);
    }

    public boolean isSeed(Address address) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(address));
    }

    public boolean isSeed(Peer peer) {
        return isSeed(peer.getAddress());
    }


    public boolean notASeed(Peer peer) {
        return !isSeed(peer);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void maybePrintReportedPeers(Map<Address, Peer> map, String info) {
        if (System.currentTimeMillis() - lastReportTs < MIN_PRINT_INTERVAL) {
            return;
        }
        String transportType = map.keySet().stream().map(e -> e.getTransportType().name()).findAny().orElse("N/A");
        String headerInfo = transportType + "-" + info;
        lastReportTs = System.currentTimeMillis();
        List<Peer> sortedList = map.values().stream()
                .sorted()
                .toList();
        long liveAge = TimeUnit.MINUTES.toMillis(10);
        long numPeers = sortedList.size();
        long numLivePeers = sortedList.stream().filter(peer -> peer.getAge() < liveAge).count();
        long numNonLivePeers = sortedList.stream().filter(peer -> peer.getAge() >= liveAge).count();
        String range = StringUtils.formatTime(sortedList.get(0).getAge()) +
                " to " +
                StringUtils.formatTime(sortedList.get(sortedList.size() - 1).getAge());
        log.info("\n##########################################################################################\n" +
                        "{} peers\n" +
                        "##########################################################################################\n" +
                        "Number of peers: {}\n" +
                        "Number of peers with age < 10 min: {}\n" +
                        "Number of peers with age >= 10 min {}\n" +
                        "Age range from {}\n" +
                        "##########################################################################################",
                headerInfo, numPeers, numLivePeers, numNonLivePeers, range);

        String peerAddressesByAge = Joiner.on("\n").join(sortedList.stream()
                .map(peer -> "Age: " + StringUtils.formatTime(peer.getAge()) + "; Address: " + peer.getAddress())
                .collect(Collectors.toList()));
        log.debug("\n##########################################################################################\n{} peers\n" +
                        "##########################################################################################\n" +
                        "Number of peers: {}\n" +
                        "Number of peers with age < 10 min: {}\n" +
                        "Number of peers with age >= 10 min {}\n" +
                        "Age range from {}\n" +
                        "Peer addressesByAge:\n" +
                        "{}\n" +
                        "##########################################################################################",
                headerInfo, numPeers, numLivePeers, numNonLivePeers, range, peerAddressesByAge);
    }
}
