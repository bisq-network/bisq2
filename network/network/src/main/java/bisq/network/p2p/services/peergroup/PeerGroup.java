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

package bisq.network.p2p.services.peergroup;

import bisq.common.util.MathUtils;
import bisq.network.p2p.node.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * Maintains different collections of peers and connections
 */
@Slf4j
public class PeerGroup {

    @Getter
    public static class Config {
        private final int minNumConnectedPeers;
        private final int maxNumConnectedPeers;
        private final int minNumReportedPeers;

        public Config() {
            this(8, 12, 1);
        }

        public Config(int minNumConnectedPeers,
                      int maxNumConnectedPeers,
                      int minNumReportedPeers) {
            this.minNumConnectedPeers = minNumConnectedPeers;
            this.maxNumConnectedPeers = maxNumConnectedPeers;
            this.minNumReportedPeers = minNumReportedPeers;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new PeerGroup.Config(
                    typesafeConfig.getInt("minNumConnectedPeers"),
                    typesafeConfig.getInt("maxNumConnectedPeers"),
                    typesafeConfig.getInt("minNumReportedPeers"));
        }
    }

    private final Node node;
    private final Config config;
    @Getter
    private final Set<Address> seedNodeAddresses;
    private final BanList banList;
    private final PersistedPeersHandler persistedPeersHandler;
    @Getter
    private final Set<Peer> reportedPeers = new CopyOnWriteArraySet<>();

    public PeerGroup(Node node, Config config, Set<Address> seedNodeAddresses, BanList banList, PersistedPeersHandler persistedPeersHandler) {
        this.node = node;
        this.config = config;
        this.seedNodeAddresses = seedNodeAddresses;
        this.banList = banList;
        this.persistedPeersHandler = persistedPeersHandler;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Reported peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addReportedPeers(Set<Peer> peers) {
        reportedPeers.addAll(peers);
        persistedPeersHandler.addPersistedPeers(peers);
    }

    public void removeReportedPeers(Collection<Peer> peers) {
        reportedPeers.removeAll(peers);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connections
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<OutboundConnection> getOutboundConnections() {
        return node.getOutboundConnectionsByAddress().values().stream().filter(Connection::isRunning);
    }

    public Stream<InboundConnection> getInboundConnections() {
        return node.getInboundConnectionsByAddress().values().stream().filter(Connection::isRunning);
    }

    public Stream<Connection> getAllConnections() {
        return Stream.concat(getOutboundConnections(), getInboundConnections());
    }

    public int getNumConnections() {
        return (int) getAllConnections().count();
    }

    public boolean isASeed(Connection connection) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(connection.getPeerAddress()));
    }

    public int getMinOutboundConnections() {
        return MathUtils.roundDoubleToInt(config.getMinNumConnectedPeers() * 0.4);
    }

    public int getMaxInboundConnections() {
        return config.getMaxNumConnectedPeers() - getMinOutboundConnections();
    }

    public Comparator<Connection> getConnectionAgeComparator() {
        return Comparator.comparing(connection -> connection.getMetrics().getCreationDate());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<Peer> getAllConnectedPeers() {
        return getAllConnections().map(con ->
                new Peer(con.getPeersCapability(), con.getPeersLoad(), con.isOutboundConnection()));
    }

    public boolean isNotInQuarantine(Peer peer) {
        return banList.isNotBanned(peer.getAddress());
    }

    public boolean notMyself(Peer peer) {
        return notMyself(peer.getAddress());
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

    public Stream<Address> getAllConnectedPeerAddresses() {
        return getAllConnectedPeers().map(Peer::getAddress);
    }

    public boolean notMyself(Address address) {
        return node.findMyAddress().stream().noneMatch(myAddress -> myAddress.equals(address));
    }

    public boolean isASeed(Address address) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(address));
    }
}
