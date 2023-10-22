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

import bisq.common.util.ByteUnit;
import bisq.common.util.MathUtils;
import bisq.network.p2p.services.data.inventory.Inventory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class NetworkLoadService {
    private NetworkLoad currentNetworkLoad;
    @Nullable
    private NetworkLoad previousNetworkLoad = null;
    private long lastUpdated = 0;

    public NetworkLoadService() {
        currentNetworkLoad = new NetworkLoad();
    }

    public NetworkLoadService(NetworkLoad networkLoad) {
        currentNetworkLoad = networkLoad;
    }

    public void updatePeersNetworkLoad(NetworkLoad networkLoad) {
        synchronized (this) {
            lastUpdated = System.currentTimeMillis();
            previousNetworkLoad = currentNetworkLoad;
            currentNetworkLoad = networkLoad;
        }
    }

    public void updateMyLoad(List<ConnectionMetrics> allConnectionMetrics, Inventory inventory) {
        double load = calculateLoad(allConnectionMetrics, inventory);
        NetworkLoad networkLoad = new NetworkLoad(load);
        synchronized (this) {
            lastUpdated = System.currentTimeMillis();
            previousNetworkLoad = currentNetworkLoad;
            currentNetworkLoad = networkLoad;
        }
    }

    private double calculateLoad(List<ConnectionMetrics> allConnectionMetrics, Inventory inventory) {
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
        long networkDatabaseSize = inventory.getEntries().stream().mapToLong(e -> e.toProto().getSerializedSize()).sum();

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
        StringBuilder sb = new StringBuilder("\n##############################################\n");
        sb.append("numConnectionsImpact=" + numConnectionsImpact);
        sb.append("\nsentBytesImpact=" + sentBytesImpact);
        sb.append("\nspentSendTimeImpact=" + spentSendTimeImpact);
        sb.append("\nnumMessagesSentImpact=" + numMessagesSentImpact);
        sb.append("\nreceivedBytesImpact=" + receivedBytesImpact);
        sb.append("\ndeserializeTimeImpact=" + deserializeTimeImpact);
        sb.append("\nnumMessagesReceivedImpact=" + numMessagesReceivedImpact);
        sb.append("\nnetworkDatabaseSizeImpact=" + networkDatabaseSizeImpact);
        sb.append("\nsum=" + sum);
        sb.append("\n##############################################\n");
        log.info(sb.toString());

        return MathUtils.bounded(0, 1, sum);
    }
}