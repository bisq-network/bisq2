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

package bisq.network.p2p.services.monitor;

import bisq.common.timer.Scheduler;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MonitorService {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toSeconds(30);
    private static final long INTERVAL = TimeUnit.MINUTES.toSeconds(3);

    private final ServiceNodesByTransport serviceNodesByTransport;
    private final DataService dataService;
    private final NetworkLoadService networkLoadService;
    private Optional<Scheduler> updateNetworkLoadScheduler = Optional.empty();

    public MonitorService(ServiceNodesByTransport serviceNodesByTransport,
                          DataService dataService,
                          NetworkLoadService networkLoadService) {
        this.serviceNodesByTransport = serviceNodesByTransport;
        this.dataService = dataService;
        this.networkLoadService = networkLoadService;
    }

    public void initialize() {
        updateNetworkLoadScheduler = Optional.of(Scheduler.run(this::updateNetworkLoad)
                .periodically(INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS)
                .name("NetworkLoadExchangeService.updateNetworkLoadScheduler"));
    }

    public CompletableFuture<Boolean> shutdown() {
        updateNetworkLoadScheduler.ifPresent(Scheduler::stop);
        return CompletableFuture.completedFuture(true);
    }

    private void updateNetworkLoad() {
        List<ConnectionMetrics> allConnectionMetrics = getAllConnections()
                .map(Connection::getConnectionMetrics)
                .collect(Collectors.toList());

        // Provide an empty filter so that we get all persisted network data
        DataFilter emptyFilter = new DataFilter(new ArrayList<>());
        Inventory inventory = dataService.getStorageService().getInventoryOfAllStores(emptyFilter);

        networkLoadService.updateMyLoad(allConnectionMetrics, inventory);
    }

    // All connections of all nodes on all transports
    public Stream<Connection> getAllConnections() {
        return serviceNodesByTransport.getMap().values().stream()
                .flatMap(serviceNode -> serviceNode.getNodesById().getAllNodes().stream())
                .flatMap(Node::getAllConnections);
    }


 /*   public String getPeerGroupInfo() {
        int numSeedConnections = (int) peerGroupService.getAllConnections()
                .filter(connection -> peerGroupService.isSeed(connection.getPeerAddress())).count();
        StringBuilder sb = new StringBuilder();
        sb.append(node.getTransportType().name()).append(": ")
                .append(node.findMyAddress().map(Address::toString).orElse(""))
                .append("\n").append("Num connections: ").append(peerGroupService.getNumConnections())
                .append("\n").append("Num all connections: ").append(peerGroupService.getNumConnections())
                .append("\n").append("Num outbound connections: ").append(peerGroupService.getOutboundConnections().count())
                .append("\n").append("Num inbound connections: ").append(peerGroupService.getInboundConnections().count())
                .append("\n").append("Num seed connections: ").append(numSeedConnections)
                .append("\n").append("Connections: ").append("\n");
        peerGroupService.getOutboundConnections()
                .sorted(peerGroupService.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, true));
        peerGroupService.getInboundConnections()
                .sorted(peerGroupService.getConnectionAgeComparator())
                .forEach(connection -> appendConnectionInfo(sb, connection, false));
        sb.append("\n").append("Reported peers (").append(peerGroupService.getReportedPeers().size()).append("): ").append(peerGroupService.getReportedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        sb.append("\n").append("Persisted peers: ").append(peerGroupService.getPersistedPeers().stream()
                .map(Peer::getAddress).sorted(Comparator.comparing(Address::getPort)).collect(Collectors.toList()));
        return sb.append("\n").toString();
    }*/

  /*  private void appendConnectionInfo(StringBuilder sb, Connection connection, boolean isOutbound) {
        String date = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(connection.getConnectionMetrics().getCreationDate());
        String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
        String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
        String dir = isOutbound ? " --> " : " <-- ";
        sb.append(node).append(dir).append(peerAddress).append(date).append("\n");
    }*/
}