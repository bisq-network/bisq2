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
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NetworkLoadService {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toSeconds(15);
    private static final long INTERVAL = TimeUnit.MINUTES.toSeconds(1);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.####");

    private final ServiceNode serviceNode;
    private final NetworkLoadSnapshot networkLoadSnapshot;
    private final int maxNumConnectedPeers;
    private final StorageService storageService;
    private final Map<String, ConnectionMetrics> connectionMetricsByConnectionId = new HashMap<>();
    @Setter
    private double difficultyAdjustmentFactor = NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT;
    private final Scheduler scheduler;
    private final Object lock = new Object();

    @Getter
    private long sentBytesOfLastHour, spentSendMessageTimeOfLastHour, numMessagesSentOfLastHour,
            receivedBytesOfLastHour, deserializeTimeOfLastHour, numMessagesReceivedOfLastHour;
    @Getter
    private TreeMap<String, AtomicLong> numSentMessagesByClassName, numReceivedMessagesByClassName,
            numSentDistributedDataByClassName, numReceivedDistributedDataByClassName;

    public NetworkLoadService(ServiceNode serviceNode,
                              StorageService storageService,
                              NetworkLoadSnapshot networkLoadSnapshot,
                              int maxNumConnectedPeers) {
        this.serviceNode = serviceNode;
        this.storageService = storageService;
        this.networkLoadSnapshot = networkLoadSnapshot;
        this.maxNumConnectedPeers = maxNumConnectedPeers;

        scheduler = Scheduler.run(this::updateNetworkLoad)
                .host(this)
                .runnableName("updateNetworkLoad")
                .periodically(INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.stop();
    }

    public NetworkLoad updateNetworkLoad() {
        Map<String, ConnectionMetrics> currentConnections = getConnectionMetricsByConnectionId();
        Set<ConnectionMetrics> allConnectionMetrics;
        synchronized (lock) {
            // We remove all metrics older than 1 hour. In case the connection is still alive we get it added
            // again in the putAll call.
            long maxAge = TimeUnit.HOURS.toMillis(1);
            Set<String> outDated = connectionMetricsByConnectionId.entrySet().stream()
                    .filter(e -> e.getValue().getAge() > maxAge)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            outDated.forEach(connectionMetricsByConnectionId::remove);

            this.connectionMetricsByConnectionId.putAll(currentConnections);
            allConnectionMetrics = new HashSet<>(connectionMetricsByConnectionId.values());
        }

        double load = calculateLoad(allConnectionMetrics);
        NetworkLoad networkLoad = new NetworkLoad(load, difficultyAdjustmentFactor);
        networkLoadSnapshot.updateNetworkLoad(networkLoad);
        return networkLoad;
    }

    private double calculateLoad(Set<ConnectionMetrics> allConnectionMetrics) {
        // For metrics of last hour we use metrics from the accumulated connections (closed of past hour).
        sentBytesOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getSentBytesOfLastHour)
                .sum();
        spentSendMessageTimeOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getSpentSendMessageTimeOfLastHour)
                .sum();
        numMessagesSentOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getNumMessagesSentOfLastHour)
                .sum();
        receivedBytesOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getReceivedBytesOfLastHour)
                .sum();
        deserializeTimeOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getDeserializeTimeOfLastHour)
                .sum();
        numMessagesReceivedOfLastHour = allConnectionMetrics.stream()
                .mapToLong(ConnectionMetrics::getNumMessagesReceivedOfLastHour)
                .sum();

        numSentMessagesByClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumSentMessagesByClassName)
                .forEach(map -> map.forEach((name, value) ->
                        numSentMessagesByClassName.computeIfAbsent(name, key -> new AtomicLong())
                                .addAndGet(value.get())));
        StringBuilder numSentMessagesByClassNameBuilder = new StringBuilder();
        numSentMessagesByClassName.forEach((key, value) -> {
            numSentMessagesByClassNameBuilder.append("\n    - ");
            numSentMessagesByClassNameBuilder.append(key);
            numSentMessagesByClassNameBuilder.append(": ");
            numSentMessagesByClassNameBuilder.append(value.get());
        });

        numReceivedMessagesByClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumReceivedMessagesByClassName)
                .forEach(map -> map.forEach((name, value) ->
                        numReceivedMessagesByClassName.computeIfAbsent(name, key -> new AtomicLong())
                                .addAndGet(value.get())));
        StringBuilder numReceivedMessagesByClassNameBuilder = new StringBuilder();
        numReceivedMessagesByClassName.forEach((key, value) -> {
            numReceivedMessagesByClassNameBuilder.append("\n    - ");
            numReceivedMessagesByClassNameBuilder.append(key);
            numReceivedMessagesByClassNameBuilder.append(": ");
            numReceivedMessagesByClassNameBuilder.append(value.get());
        });

        numSentDistributedDataByClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumSentDistributedDataByClassName)
                .forEach(map -> map.forEach((name, value) ->
                        numSentDistributedDataByClassName.computeIfAbsent(name, key -> new AtomicLong())
                                .addAndGet(value.get())));
        StringBuilder numSentDistributedDataByClassNameBuilder = new StringBuilder();
        numSentDistributedDataByClassName.forEach((key, value) -> {
            numSentDistributedDataByClassNameBuilder.append("\n    - ");
            numSentDistributedDataByClassNameBuilder.append(key);
            numSentDistributedDataByClassNameBuilder.append(": ");
            numSentDistributedDataByClassNameBuilder.append(value.get());
        });

        numReceivedDistributedDataByClassName = new TreeMap<>();
        allConnectionMetrics.stream()
                .map(ConnectionMetrics::getNumReceivedDistributedDataByClassName)
                .forEach(map -> map.forEach((name, value) ->
                        numReceivedDistributedDataByClassName.computeIfAbsent(name, key -> new AtomicLong())
                                .addAndGet(value.get())));
        StringBuilder numReceivedDistributedDataByClassNameBuilder = new StringBuilder();
        numReceivedDistributedDataByClassName.forEach((key, value) -> {
            numReceivedDistributedDataByClassNameBuilder.append("\n    - ");
            numReceivedDistributedDataByClassNameBuilder.append(key);
            numReceivedDistributedDataByClassNameBuilder.append(": ");
            numReceivedDistributedDataByClassNameBuilder.append(value.get());
        });

        long numConnections = getAllCurrentConnections().count();
        long networkDatabaseSize = storageService.getNetworkDatabaseSize(); // takes about 50 ms

        StringBuilder sb = new StringBuilder("\n\n////////////////////////////////////////////////////////////////////////////////////////////////////");
        sb.append("\nNetwork statistics").append(("\n////////////////////////////////////////////////////////////////////////////////////////////////////"))
                .append("\nSize of network DB: ").append(ByteUnit.BYTE.toMB(networkDatabaseSize)).append(" MB")
                .append("\nNumber of Connections: ").append(numConnections)

                .append("\nSent messages:")
                .append("\nData sent in last hour: ").append(ByteUnit.BYTE.toMB(sentBytesOfLastHour)).append(" MB")
                .append("\nTime for message sending in last hour: ").append(spentSendMessageTimeOfLastHour / 1000d).append(" sec.")
                .append("\nNumber of messages sent in last hour: ").append(numMessagesSentOfLastHour)
                .append("\nNumber of messages sent by class name:").append(numSentMessagesByClassNameBuilder)
                .append("\nNumber of distributed data sent by class name:").append(numSentDistributedDataByClassNameBuilder)

                .append("\nReceived messages:")
                .append("\nData received in last hour: ").append(ByteUnit.BYTE.toMB(receivedBytesOfLastHour)).append(" MB")
                .append("\nTime for message deserializing in last hour: ").append(deserializeTimeOfLastHour / 1000d).append(" sec.")
                .append("\nNumber of messages received in last hour: ").append(numMessagesReceivedOfLastHour)
                .append("\nNumber of messages received by class name:").append(numReceivedMessagesByClassNameBuilder)
                .append("\nNumber of distributed data received by class name:").append(numReceivedDistributedDataByClassNameBuilder)

                .append("\n////////////////////////////////////////////////////////////////////////////////////////////////////");

        // We apply a factor to each max value based on the maxNumConnectedPeers to reflect higher expected load
        // This is mainly important for seed nodes which are configured with higher maxNumConnectedPeers.
        double defaultMaxNumConnectedPeers = 12;
        double numConnectedPeersFactor = maxNumConnectedPeers / defaultMaxNumConnectedPeers;

        double MAX_NUM_CON = 20 * defaultMaxNumConnectedPeers;
        double NUM_CON_WEIGHT = 0.1;
        double numConnectionsImpact = numConnections / MAX_NUM_CON * NUM_CON_WEIGHT;

        double MAX_SENT_BYTES = ByteUnit.MB.toBytes(20) * numConnectedPeersFactor;
        double SENT_BYTES_WEIGHT = 0.2;
        double sentBytesImpact = sentBytesOfLastHour / MAX_SENT_BYTES * SENT_BYTES_WEIGHT;

        double MAX_SPENT_SEND_TIME = TimeUnit.MINUTES.toMillis(1) * numConnectedPeersFactor;
        double SPENT_SEND_TIME_WEIGHT = 0.1;
        double spentSendTimeImpact = spentSendMessageTimeOfLastHour / MAX_SPENT_SEND_TIME * SPENT_SEND_TIME_WEIGHT;

        double MAX_NUM_MSG_SENT = 5000 * numConnectedPeersFactor;
        double NUM_MSG_SENT_WEIGHT = 0.1;
        double numMessagesSentImpact = numMessagesSentOfLastHour / MAX_NUM_MSG_SENT * NUM_MSG_SENT_WEIGHT;

        // We receive about 5 MB when oracle node republishes its data
        double MAX_REC_BYTES = ByteUnit.MB.toBytes(20) * numConnectedPeersFactor;
        double REC_BYTES_WEIGHT = 0.2;
        double receivedBytesImpact = receivedBytesOfLastHour / MAX_REC_BYTES * REC_BYTES_WEIGHT;

        double MAX_DESERIALIZE_TIME = TimeUnit.MINUTES.toMillis(1) * numConnectedPeersFactor;
        double DESERIALIZE_TIME_WEIGHT = 0.1;
        double deserializeTimeImpact = deserializeTimeOfLastHour / MAX_DESERIALIZE_TIME * DESERIALIZE_TIME_WEIGHT;

        // When oracle node republishes its data we get about 2500 messages in about 10 minutes
        double MAX_NUM_MSG_REC = 5000 * numConnectedPeersFactor;
        double NUM_MSG_REC_WEIGHT = 0.1;
        double numMessagesReceivedImpact = numMessagesReceivedOfLastHour / MAX_NUM_MSG_REC * NUM_MSG_REC_WEIGHT;

        // 6MB at Aug 2024 -> 0.018
        double MAX_DB_SIZE = ByteUnit.MB.toBytes(100); // Has no correlation to maxNumConnectedPeers
        double DB_WEIGHT = 0.1;
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
                .append("\nnumConnectionsImpact=").append(DECIMAL_FORMAT.format(numConnectionsImpact))
                .append("\nsentBytesImpact=").append(DECIMAL_FORMAT.format(sentBytesImpact))
                .append("\nspentSendTimeImpact=").append(DECIMAL_FORMAT.format(spentSendTimeImpact))
                .append("\nnumMessagesSentImpact=").append(DECIMAL_FORMAT.format(numMessagesSentImpact))
                .append("\nreceivedBytesImpact=").append(DECIMAL_FORMAT.format(receivedBytesImpact))
                .append("\ndeserializeTimeImpact=").append(DECIMAL_FORMAT.format(deserializeTimeImpact))
                .append("\nnumMessagesReceivedImpact=").append(DECIMAL_FORMAT.format(numMessagesReceivedImpact))
                .append("\nnetworkDatabaseSizeImpact=").append(DECIMAL_FORMAT.format(networkDatabaseSizeImpact))
                .append("\nNetwork load=").append(DECIMAL_FORMAT.format(load))
                .append("\n----------------------------------------------------------------------------------------------------\n");
        log.info(sb.toString());

        return MathUtils.bounded(0, 1, load);
    }

    private Set<? extends DataRequest> getAllDataRequests() {
        return storageService.getAllDataRequestMapEntries()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    private Map<String, ConnectionMetrics> getConnectionMetricsByConnectionId() {
        return getAllCurrentConnections().collect(Collectors.toMap(Connection::getId, Connection::getConnectionMetrics));
    }

    private Stream<Connection> getAllCurrentConnections() {
        return serviceNode.getNodesById().getAllNodes().stream()
                .flatMap(Node::getAllConnections);
    }
}