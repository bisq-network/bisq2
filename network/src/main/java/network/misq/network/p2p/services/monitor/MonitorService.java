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

package network.misq.network.p2p.services.monitor;

import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.Peer;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MonitorService {
    private final Node node;
    private final PeerGroup peerGroup;

    public MonitorService(Node node, PeerGroupService peerGroupService) {
        this.node = node;
        this.peerGroup = peerGroupService.getPeerGroup();
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }


    public String getPeerGroupInfo() {
        int numSeedConnections = (int) peerGroup.getAllConnections()
                .filter(connection -> peerGroup.isASeed(connection.getPeerAddress())).count();
        StringBuilder sb = new StringBuilder();
        sb.append(node.getTransportType().name()).append(": ")
                .append(node.findMyAddress().map(Address::toString).orElse(""))
                .append("\n").append("Num connections: ").append(peerGroup.getNumConnections())
                .append("\n").append("Num all connections: ").append(peerGroup.getNumConnections())
                .append("\n").append("Num outbound connections: ").append(peerGroup.getOutboundConnections().count())
                .append("\n").append("Num inbound connections: ").append(peerGroup.getInboundConnections().count())
                .append("\n").append("Num seed connections: ").append(numSeedConnections)
                .append("\n").append("Connections: ").append("\n");
        peerGroup.getOutboundConnections()
                .sorted(peerGroup.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, true));
        peerGroup.getInboundConnections()
                .sorted(peerGroup.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, false));
        sb.append("\n").append("Reported peers (").append(peerGroup.getReportedPeers().size()).append("): ").append(peerGroup.getReportedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        sb.append("\n").append("Persisted peers: ").append(peerGroup.getPersistedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        return sb.append("\n").toString();
    }

    private void appendConnectionInfo(StringBuilder sb, Connection connection, boolean isOutbound) {
        String date = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(connection.getMetrics().getCreationDate());
        String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
        String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
        String dir = isOutbound ? " --> " : " <-- ";
        sb.append(node).append(dir).append(peerAddress).append(date).append("\n");
    }
}