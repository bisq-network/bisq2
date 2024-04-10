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

import bisq.common.util.CollectionUtil;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Maintains different collections of peers and connections
 */
@Slf4j
public class PeerGroupService implements PersistenceClient<PeerGroupStore> {

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

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new PeerGroupService.Config(
                    typesafeConfig.getInt("minNumConnectedPeers"),
                    typesafeConfig.getInt("minNumOutboundConnectedPeers"),
                    typesafeConfig.getInt("maxNumConnectedPeers"),
                    typesafeConfig.getInt("minNumReportedPeers"));
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
    private final Map<Address, Peer> reportedPeersByAddress = new ConcurrentHashMap<>();

    public PeerGroupService(PersistenceService persistenceService,
                            TransportType transportType,
                            Config config,
                            Set<Address> seedNodeAddresses,
                            BanList banList) {
        this.config = config;
        this.seedNodeAddresses = seedNodeAddresses;
        this.banList = banList;

        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.SETTINGS,
                transportType.name().toLowerCase() + persistableStore.getClass().getSimpleName(),
                persistableStore);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Persisted peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<Address, Peer> getPersistedPeersByAddress() {
        return persistableStore.getPersistedPeersByAddress();
    }

    public Set<Peer> getPersistedPeers() {
        return new HashSet<>(getPersistedPeersByAddress().values());
    }

    public boolean addPersistedPeer(Peer peer) {
        boolean wasAdded = doAddPersistedPeer(peer);
        if (wasAdded) {
            persist();
        }
        return wasAdded;
    }

    private boolean doAddPersistedPeer(Peer peer) {
        return doAddPeer(peer, getPersistedPeersByAddress());
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
        AtomicBoolean wasAdded = new AtomicBoolean();
        peers.forEach(peer -> wasAdded.set(doAddPersistedPeer(peer) || wasAdded.get()));
        if (wasAdded.get()) {
            persist();
        }
        return wasAdded.get();
    }

    public void removePersistedPeers(Collection<Peer> peers) {
        Map<Address, Peer> persistedPeersById = getPersistedPeersByAddress();
        peers.forEach(peer -> persistedPeersById.remove(peer.getAddress()));
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Reported peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Set<Peer> getReportedPeers() {
        return new HashSet<>(reportedPeersByAddress.values());
    }

    private boolean addReportedPeer(Peer peer) {
        return doAddPeer(peer, reportedPeersByAddress);
    }

    public boolean addReportedPeers(Set<Peer> peers) {
        AtomicBoolean wasAdded = new AtomicBoolean();
        peers.forEach(peer -> wasAdded.set(addReportedPeer(peer) || wasAdded.get()));
        return wasAdded.get();
    }

    public void removeReportedPeers(Collection<Peer> peers) {
        peers.forEach(peer -> reportedPeersByAddress.remove(peer.getAddress()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connections
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isSeed(Connection connection) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(connection.getPeerAddress()));
    }

    public int getMinOutboundConnections() {
        return config.getMinNumOutboundConnectedPeers();
    }

    public int getMaxInboundConnections() {
        return config.getMaxNumConnectedPeers() - getMinOutboundConnections();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<Peer> getAllConnectedPeers(Node node) {
        return node.getAllActiveConnections().map(connection ->
                new Peer(connection.getPeersCapability(),
                        connection.getPeersNetworkLoadSnapshot().getCurrentNetworkLoad(),
                        connection.isOutboundConnection()));
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Address
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSeedNodeAddress(Address seedNodeAddress) {
        this.seedNodeAddresses.add(seedNodeAddress);
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
}
