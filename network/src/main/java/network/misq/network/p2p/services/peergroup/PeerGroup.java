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

package network.misq.network.p2p.services.peergroup;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.MathUtils;
import network.misq.network.p2p.node.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
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
    }

    private final Node node;
    private final Config config;
    @Getter
    private final List<Address> seedNodeAddresses;
    private final BannList bannList;
    @Getter
    private final Set<Peer> reportedPeers = new CopyOnWriteArraySet<>();
    //todo persist
    @Getter
    private final Set<Peer> persistedPeers = new CopyOnWriteArraySet<>();

    public PeerGroup(Node node, Config config, List<Address> seedNodeAddresses, BannList bannList) {
        this.node = node;
        this.config = config;
        this.seedNodeAddresses = seedNodeAddresses;
        this.bannList = bannList;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Reported peers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addReportedPeers(Set<Peer> peers) {
        reportedPeers.addAll(peers);
    }

    public void removeReportedPeers(Collection<Peer> peers) {
        reportedPeers.removeAll(peers);
    }

    public void removePersistedPeers(Collection<Peer> peers) {
        persistedPeers.removeAll(peers);
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
        return bannList.isNotBanned(peer.getAddress());
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

    public Stream<Address> getAllConnectedPeerAddresses() {
        return getAllConnectedPeers().map(Peer::getAddress);
    }

    public boolean notMyself(Address address) {
        return node.findMyAddress().stream().noneMatch(myAddress -> myAddress.equals(address));
    }

    public boolean isASeed(Address address) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(address));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Monitor
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public String getMonitorInfo() {
        int numSeedConnections = (int) getAllConnections()
                .filter(connection -> isASeed(connection.getPeerAddress())).count();
        StringBuilder sb = new StringBuilder();
        sb.append("Num connections: ").append(getNumConnections())
                .append("\n").append("Num all connections: ").append(getNumConnections())
                .append("\n").append("Num outbound connections: ").append(getOutboundConnections().count())
                .append("\n").append("Num inbound connections: ").append(getInboundConnections().count())
                .append("\n").append("Num seed connections: ").append(numSeedConnections)
                .append("\n").append("Connections: ").append("\n");
        getOutboundConnections()
                .sorted(getConnectionAgeComparator())
                .forEach(connection -> printConnectionInfo(sb, connection, true));
        getInboundConnections()
                .sorted(getConnectionAgeComparator())
                .forEach(connection -> printConnectionInfo(sb, connection, false));
        sb.append("\n").append("Reported peers: ").append(reportedPeers.stream().map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        sb.append("\n").append("Persisted peers: ").append(persistedPeers.stream().map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        return sb.append("\n").toString();
    }

    private void printConnectionInfo(StringBuilder sb, Connection connection, boolean isOutbound) {
        String date = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(connection.getMetrics().getCreationDate());
        String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
        String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
        String dir = isOutbound ? " --> " : " <-- ";
        sb.append(node).append(dir).append(peerAddress).append(date).append("\n");
    }
}
