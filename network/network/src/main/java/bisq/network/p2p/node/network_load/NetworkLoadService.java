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

import bisq.common.data.ByteUnit;
import bisq.common.proto.Proto;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.network.p2p.ServiceNodesByTransport;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class NetworkLoadService {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toSeconds(15);
    private static final long INTERVAL = TimeUnit.MINUTES.toSeconds(3);

    private final ServiceNodesByTransport serviceNodesByTransport;
    private final NetworkLoadSnapshot networkLoadSnapshot;
    private final StorageService storageService;
    @Setter
    private double difficultyAdjustmentFactor = NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT;
    private Optional<Scheduler> updateNetworkLoadScheduler = Optional.empty();

    public NetworkLoadService(ServiceNodesByTransport serviceNodesByTransport,
                              StorageService storageService,
                              NetworkLoadSnapshot networkLoadSnapshot) {
        this.serviceNodesByTransport = serviceNodesByTransport;
        this.storageService = storageService;
        this.networkLoadSnapshot = networkLoadSnapshot;
    }

    public void initialize() {
        log.info("initialize");
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
        NetworkLoad networkLoad = new NetworkLoad(load, difficultyAdjustmentFactor);
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
        long networkDatabaseSize = dataRequests.stream().mapToLong(Proto::getSerializedSize).sum();

        Map<String, AtomicLong> numSentMessagesByMessageClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumSentMessagesByMessageClassName)
                .forEach(map -> {
                    map.forEach((name, value) -> {
                        numSentMessagesByMessageClassName.putIfAbsent(name, new AtomicLong());
                        numSentMessagesByMessageClassName.get(name).addAndGet(value.get());
                    });
                });
        StringBuilder numSentMsgPerClassName = new StringBuilder();
        numSentMessagesByMessageClassName.forEach((key, value) -> {
            numSentMsgPerClassName.append("\n - ");
            numSentMsgPerClassName.append(key);
            numSentMsgPerClassName.append(": ");
            numSentMsgPerClassName.append(value.get());
        });

        Map<String, AtomicLong> numReceivedMessagesByMessageClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumReceivedMessagesByMessageClassName)
                .forEach(map -> {
                    map.forEach((name, value) -> {
                        numReceivedMessagesByMessageClassName.putIfAbsent(name, new AtomicLong());
                        numReceivedMessagesByMessageClassName.get(name).addAndGet(value.get());
                    });
                });
        StringBuilder numRecMsgPerClassName = new StringBuilder();
        numReceivedMessagesByMessageClassName.forEach((key, value) -> {
            numRecMsgPerClassName.append("\n - ");
            numRecMsgPerClassName.append(key);
            numRecMsgPerClassName.append(": ");
            numRecMsgPerClassName.append(value.get());
        });

        StringBuilder sb = new StringBuilder("\n\n////////////////////////////////////////////////////////////////////////////////////////////////////");
        sb.append("\nNetwork statistics").append(("\n////////////////////////////////////////////////////////////////////////////////////////////////////"))
                .append("\nNumber of Connections: ").append(numConnections)
                .append("\nNumber of messages sent in last hour: ").append(numMessagesSentOfLastHour)
                .append("\nNumber of messages sent by class name:").append(numSentMsgPerClassName)
                .append("\nNumber of messages received in last hour: ").append(numMessagesReceivedOfLastHour)
                .append("\nNumber of messages received by class name:").append(numRecMsgPerClassName)
                .append("\nSize of network DB: ").append(ByteUnit.BYTE.toMB(networkDatabaseSize)).append(" MB")
                .append("\nData sent in last hour: ").append(ByteUnit.BYTE.toMB(sentBytesOfLastHour)).append(" MB")
                .append("\nData received in last hour: ").append(ByteUnit.BYTE.toMB(receivedBytesOfLastHour)).append(" MB")
                .append("\nTime for message sending in last hour: ").append(spentSendMessageTimeOfLastHour / 1000d).append(" sec.")
                .append("\nTime for message deserializing in last hour: ").append(deserializeTimeOfLastHour / 1000d).append(" sec.")
                .append("\n////////////////////////////////////////////////////////////////////////////////////////////////////");

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

        double load = numConnectionsImpact +
                sentBytesImpact +
                spentSendTimeImpact +
                numMessagesSentImpact +
                receivedBytesImpact +
                deserializeTimeImpact +
                numMessagesReceivedImpact +
                networkDatabaseSizeImpact;
        sb.append("\n\n----------------------------------------------------------------------------------------------------")
                .append("\nCalculated network load:")
                .append(("\n----------------------------------------------------------------------------------------------------"))
                .append("\nnumConnectionsImpact=").append(numConnectionsImpact)
                .append("\nsentBytesImpact=").append(sentBytesImpact)
                .append("\nspentSendTimeImpact=").append(spentSendTimeImpact)
                .append("\nnumMessagesSentImpact=").append(numMessagesSentImpact)
                .append("\nreceivedBytesImpact=").append(receivedBytesImpact)
                .append("\ndeserializeTimeImpact=").append(deserializeTimeImpact)
                .append("\nnumMessagesReceivedImpact=").append(numMessagesReceivedImpact)
                .append("\nnetworkDatabaseSizeImpact=").append(networkDatabaseSizeImpact)
                .append("\nNetwork load=").append(load)
                .append("\n----------------------------------------------------------------------------------------------------\n");
        log.info(sb.toString());

        //TODO load calculation has some bugs at spentSendTimeImpact. Until fixed we limit load to 0.1 to avoid high difficulty
        return MathUtils.bounded(0, 0.1, load);
        //return MathUtils.bounded(0, 1, load);
    }
}