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
import network.misq.network.p2p.node.*;

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
    private final Quarantine quarantine;
    @Getter
    private final Set<Peer> reportedPeers = new CopyOnWriteArraySet<>();
    //todo persist
    @Getter
    private final Set<Peer> persistedPeers = new CopyOnWriteArraySet<>();

    public PeerGroup(Node node, Config config, List<Address> seedNodeAddresses, Quarantine quarantine) {
        this.node = node;
        this.config = config;
        this.seedNodeAddresses = seedNodeAddresses;
        this.quarantine = quarantine;
    }


    public void addReportedPeers(Set<Peer> peers) {
        reportedPeers.addAll(peers);
    }

    public void removeReportedPeers(Collection<Peer> peers) {
        reportedPeers.removeAll(peers);
    }

    public void removePersistedPeers(Collection<Peer> peers) {
        persistedPeers.removeAll(peers);
    }

    public Stream<Address> getAllConnectedPeerAddresses() {
        return getAllConnectedPeers().map(Peer::getAddress);
    }

    public Stream<Peer> getAllConnectedPeers() {
        return getAllConnectionsAsStream().map(con ->
                new Peer(con.getPeersCapability(), con.getPeersLoad(), con.isOutboundConnection()));
    }

    public Stream<Connection> getAllConnectionsAsStream() {
        return Stream.concat(getOutboundConnections().values().stream(), getInboundConnections().values().stream());
    }

    public boolean notMyself(Peer peer) {
        return notMyself(peer.getAddress());
    }

    public boolean notMyself(Address address) {
        return node.findMyAddress().stream().noneMatch(myAddress -> myAddress.equals(address));
    }

    public boolean isASeed(Address address) {
        return seedNodeAddresses.stream().anyMatch(seedAddress -> seedAddress.equals(address));
    }

    public boolean isNotInQuarantine(Peer peer) {
        return quarantine.isNotInQuarantine(peer.getAddress());
    }

    public String getInfo() {
        int numSeedConnections = (int) getAllConnectionsAsStream()
                .filter(connection -> isASeed(connection.getPeerAddress())).count();
        StringBuilder sb = new StringBuilder();
        Map<Address, OutboundConnection> outboundConnections = getOutboundConnections();
        Map<Address, InboundConnection> inboundConnections = getInboundConnections();
        sb.append("Num connections: ").append(getNumConnections())
                .append("\n").append("Num all connections: ").append(getNumConnections())
                .append("\n").append("Num outbound connections: ").append(outboundConnections.size())
                .append("\n").append("Num inbound connections: ").append(inboundConnections.size())
                .append("\n").append("Num seed connections: ").append(numSeedConnections)
                .append("\n").append("Connections: ").append("\n");
        outboundConnections.values().forEach(connection -> sb.append(node).append(" --> ").append(connection.getPeerAddress()).append("\n"));
        inboundConnections.values().forEach(connection -> sb.append(node).append(" <-- ").append(connection.getPeerAddress()).append("\n"));
        sb.append("\n").append("Reported peers: ").append(reportedPeers.stream().map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        sb.append("\n").append("Persisted peers: ").append(persistedPeers.stream().map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        return sb.append("\n").toString();
    }

    // Delegates
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


    public Map<Address, InboundConnection> getInboundConnections() {
        return node.getInboundConnections();
    }

    public Map<Address, OutboundConnection> getOutboundConnections() {
        return node.getOutboundConnections();
    }

    public int getNumConnections() {
        return node.getNumConnections();
    }
}
