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

package bisq.network.p2p.node.network_load;

import bisq.common.timer.Scheduler;
import bisq.common.util.ByteUnit;
import bisq.common.util.MathUtils;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NetworkLoadService {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toSeconds(5);
    private static final long INTERVAL = TimeUnit.MINUTES.toSeconds(3);

    private final ServiceNodesByTransport serviceNodesByTransport;
    private final NetworkLoadSnapshot networkLoadSnapshot;
    private final StorageService storageService;
    private Optional<Scheduler> updateNetworkLoadScheduler = Optional.empty();

    public NetworkLoadService(ServiceNodesByTransport serviceNodesByTransport,
                              DataService dataService,
                              NetworkLoadSnapshot networkLoadSnapshot) {
        this.serviceNodesByTransport = serviceNodesByTransport;
        storageService = dataService.getStorageService();
        this.networkLoadSnapshot = networkLoadSnapshot;
    }

    public void initialize() {
        updateNetworkLoadScheduler = Optional.of(Scheduler.run(this::updateNetworkLoad)
                .periodically(INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS)
                .name("NetworkLoadExchangeService.updateNetworkLoadScheduler"));
    }

    public void shutdown() {
        updateNetworkLoadScheduler.ifPresent(Scheduler::stop);
    }

    private void updateNetworkLoad() {
        List<? extends DataRequest> dataRequests = storageService.getAllDataRequestMapEntries()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        double load = calculateLoad(getAllConnectionMetrics(), dataRequests);
        NetworkLoad networkLoad = new NetworkLoad(load);
        networkLoadSnapshot.updateNetworkLoad(networkLoad);
    }

    private List<ConnectionMetrics> getAllConnectionMetrics() {
        return serviceNodesByTransport.getAllServices().stream()
                .flatMap(serviceNode -> serviceNode.getNodesById().getAllNodes().stream())
                .flatMap(Node::getAllConnections)
                .map(Connection::getConnectionMetrics)
                .collect(Collectors.toList());
    }

    private static double calculateLoad(List<ConnectionMetrics> allConnectionMetrics, List<? extends DataRequest> dataRequests) {
        long numConnections = allConnectionMetrics.size();
        long sentBytesOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getSentBytesOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long spentSendMessageTimeOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getSpentSendMessageTimeOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long numMessagesSentOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumMessagesSentOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long receivedBytesOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getReceivedBytesOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long deserializeTimeOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getDeserializeTimeOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long numMessagesReceivedOfLastHour = allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumMessagesReceivedOfLastHour)
                .mapToLong(e -> e)
                .sum();
        long networkDatabaseSize = dataRequests.stream().mapToLong(e -> e.toProto().getSerializedSize()).sum();

        StringBuilder sb = new StringBuilder("\n\n##########################################################################################");
        sb.append("\nNetwork statistics").append(("\n##########################################################################################"))
                .append("\nNumber of Connections: ").append(numConnections)
                .append("\nNumber of messages sent in last hour: ").append(numMessagesSentOfLastHour)
                .append("\nNumber of messages received in last hour: ").append(numMessagesReceivedOfLastHour)
                .append("\nSize of network DB: ").append(ByteUnit.BYTE.toMB(networkDatabaseSize)).append(" MB")
                .append("\nData sent in last hour: ").append(ByteUnit.BYTE.toKB(sentBytesOfLastHour)).append(" KB")
                .append("\nData received in last hour: ").append(ByteUnit.BYTE.toKB(receivedBytesOfLastHour)).append(" KB")
                .append("\nTime for message sending in last hour: ").append(spentSendMessageTimeOfLastHour / 1000d).append(" sec.")
                .append("\nTime for message deserializing in last hour: ").append(deserializeTimeOfLastHour / 1000d).append(" sec.")
                .append("\n##########################################################################################\n");
        log.info(sb.toString());

        double MAX_NUM_CON = 30;
        double NUM_CON_WEIGHT = 0.1;
        double numConnectionsImpact = numConnections / MAX_NUM_CON * NUM_CON_WEIGHT;

        double MAX_SENT_BYTES = ByteUnit.MB.toBytes(20);
        double SENT_BYTES_WEIGHT = 0.1;
        double sentBytesImpact = sentBytesOfLastHour / MAX_SENT_BYTES * SENT_BYTES_WEIGHT;

        double MAX_SPENT_SEND_TIME = TimeUnit.MINUTES.toMillis(1);
        double SPENT_SEND_TIME_WEIGHT = 0.1;
        double spentSendTimeImpact = spentSendMessageTimeOfLastHour / MAX_SPENT_SEND_TIME * SPENT_SEND_TIME_WEIGHT;

        double MAX_NUM_MSG_SENT = 2000;
        double NUM_MSG_SENT_WEIGHT = 0.1;
        double numMessagesSentImpact = numMessagesSentOfLastHour / MAX_NUM_MSG_SENT * NUM_MSG_SENT_WEIGHT;

        double MAX_REC_BYTES = ByteUnit.MB.toBytes(20);
        double REC_BYTES_WEIGHT = 0.1;
        double receivedBytesImpact = receivedBytesOfLastHour / MAX_REC_BYTES * REC_BYTES_WEIGHT;

        double MAX_DESERIALIZE_TIME = TimeUnit.MINUTES.toMillis(1);
        double DESERIALIZE_TIME_WEIGHT = 0.1;
        double deserializeTimeImpact = deserializeTimeOfLastHour / MAX_DESERIALIZE_TIME * DESERIALIZE_TIME_WEIGHT;

        double MAX_NUM_MSG_REC = 1000;
        double NUM_MSG_REC_WEIGHT = 0.1;
        double numMessagesReceivedImpact = numMessagesReceivedOfLastHour / MAX_NUM_MSG_REC * NUM_MSG_REC_WEIGHT;

        double MAX_DB_SIZE = ByteUnit.MB.toBytes(100);
        double DB_WEIGHT = 0.3;
        double networkDatabaseSizeImpact = networkDatabaseSize / MAX_DB_SIZE * DB_WEIGHT;

        double sum = numConnectionsImpact +
                sentBytesImpact +
                spentSendTimeImpact +
                numMessagesSentImpact +
                receivedBytesImpact +
                deserializeTimeImpact +
                numMessagesReceivedImpact +
                networkDatabaseSizeImpact;
        sb = new StringBuilder("\n");
        sb.append("numConnectionsImpact=").append(numConnectionsImpact);
        sb.append("\nsentBytesImpact=").append(sentBytesImpact);
        sb.append("\nspentSendTimeImpact=").append(spentSendTimeImpact);
        sb.append("\nnumMessagesSentImpact=").append(numMessagesSentImpact);
        sb.append("\nreceivedBytesImpact=").append(receivedBytesImpact);
        sb.append("\ndeserializeTimeImpact=").append(deserializeTimeImpact);
        sb.append("\nnumMessagesReceivedImpact=").append(numMessagesReceivedImpact);
        sb.append("\nnetworkDatabaseSizeImpact=").append(networkDatabaseSizeImpact);
        sb.append("\nsum=").append(sum);
        log.debug(sb.toString());

        return MathUtils.bounded(0, 1, sum);
    }
}