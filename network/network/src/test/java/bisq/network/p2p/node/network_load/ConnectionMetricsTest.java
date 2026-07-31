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

import bisq.common.util.ClassUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.pow.ProofOfWork;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the aggregation over the per-minute buckets. All events of a test land in the same bucket, as the bucket
 * key is the age of the ConnectionMetrics in minutes, so the selection across several buckets in sumOfLastMinute
 * is not covered here. That would require the creation time to be injectable.
 */
public class ConnectionMetricsTest {
    private static final String PING_CLASS_NAME = ClassUtils.getClassName(Ping.class);

    @Test
    public void testSentMetrics() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();
        NetworkEnvelope networkEnvelope = createNetworkEnvelope();
        long serializedSize = networkEnvelope.getSerializedSize();

        connectionMetrics.onSent(networkEnvelope, 10);
        connectionMetrics.onSent(networkEnvelope, 20);

        assertThat(connectionMetrics.getNumMessagesSent()).isEqualTo(2);
        assertThat(connectionMetrics.getSentBytes()).isEqualTo(2 * serializedSize);
        assertThat(connectionMetrics.getSpentSendMessageTimePerMinute()).isEqualTo(30);
        assertThat(connectionMetrics.getNumSentMessagesByClassName().get(PING_CLASS_NAME).get()).isEqualTo(2);

        // Nothing was received, so the received maps stay empty.
        assertThat(connectionMetrics.getNumMessagesReceived()).isZero();
        assertThat(connectionMetrics.getReceivedBytes()).isZero();
    }

    @Test
    public void testReceivedMetrics() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();
        NetworkEnvelope networkEnvelope = createNetworkEnvelope();
        long serializedSize = networkEnvelope.getSerializedSize();

        connectionMetrics.onReceived(networkEnvelope, 5);
        connectionMetrics.onReceived(networkEnvelope, 7);

        assertThat(connectionMetrics.getNumMessagesReceived()).isEqualTo(2);
        assertThat(connectionMetrics.getReceivedBytes()).isEqualTo(2 * serializedSize);
        assertThat(connectionMetrics.getDeserializeTimePerMinute()).isEqualTo(12);
        assertThat(connectionMetrics.getNumReceivedMessagesByClassName().get(PING_CLASS_NAME).get()).isEqualTo(2);

        assertThat(connectionMetrics.getNumMessagesSent()).isZero();
        assertThat(connectionMetrics.getSentBytes()).isZero();
    }

    @Test
    public void testSumOfLastMinutesWithSingleBucket() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();
        NetworkEnvelope networkEnvelope = createNetworkEnvelope();
        long serializedSize = networkEnvelope.getSerializedSize();

        connectionMetrics.onSent(networkEnvelope, 10);
        connectionMetrics.onSent(networkEnvelope, 20);
        connectionMetrics.onReceived(networkEnvelope, 5);

        // All events are in the bucket of the current minute, so the windowed sums match the totals.
        assertThat(connectionMetrics.getNumMessagesSentOfLast5Minutes()).isEqualTo(2);
        assertThat(connectionMetrics.getSentBytesOfLast5Minutes()).isEqualTo(2 * serializedSize);
        assertThat(connectionMetrics.getSpentSendMessageTimeOfLast5Minutes()).isEqualTo(30);
        assertThat(connectionMetrics.getNumMessagesReceivedOfLast5Minutes()).isEqualTo(1);
        assertThat(connectionMetrics.getReceivedBytesOfLast5Minutes()).isEqualTo(serializedSize);
        assertThat(connectionMetrics.getDeserializeTimeOfLast5Minutes()).isEqualTo(5);

        assertThat(connectionMetrics.getNumMessagesSentOfLastHour()).isEqualTo(2);
        assertThat(connectionMetrics.getSentBytesOfLastHour()).isEqualTo(2 * serializedSize);
    }

    @Test
    public void testSumOfLastMinutesWithoutData() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();

        assertThat(connectionMetrics.getSentBytesOfLast5Minutes()).isZero();
        assertThat(connectionMetrics.getNumMessagesReceivedOfLastHour()).isZero();
        assertThat(connectionMetrics.getSentBytes()).isZero();
        assertThat(connectionMetrics.getAverageRtt()).isZero();
    }

    @Test
    public void testAverageRtt() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();

        assertThat(connectionMetrics.getAverageRtt()).isZero();

        connectionMetrics.addRtt(10);
        connectionMetrics.addRtt(20);

        assertThat(connectionMetrics.getAverageRtt()).isEqualTo(15d);
    }

    @Test
    public void testClear() {
        ConnectionMetrics connectionMetrics = new ConnectionMetrics();
        NetworkEnvelope networkEnvelope = createNetworkEnvelope();

        connectionMetrics.onSent(networkEnvelope, 10);
        connectionMetrics.onReceived(networkEnvelope, 5);
        connectionMetrics.addRtt(10);

        connectionMetrics.clear();

        assertThat(connectionMetrics.getNumMessagesSent()).isZero();
        assertThat(connectionMetrics.getSentBytes()).isZero();
        assertThat(connectionMetrics.getSpentSendMessageTimePerMinute()).isZero();
        assertThat(connectionMetrics.getNumMessagesReceived()).isZero();
        assertThat(connectionMetrics.getReceivedBytes()).isZero();
        assertThat(connectionMetrics.getDeserializeTimePerMinute()).isZero();
        assertThat(connectionMetrics.getSentBytesOfLast5Minutes()).isZero();
        assertThat(connectionMetrics.getAverageRtt()).isZero();
        assertThat(connectionMetrics.getNumSentMessagesByClassName()).isEmpty();
        assertThat(connectionMetrics.getNumReceivedMessagesByClassName()).isEmpty();
    }

    private static NetworkEnvelope createNetworkEnvelope() {
        // The token is never verified here, we only need a serializable envelope, so dummy arrays are sufficient.
        // Creating a real token would run the proof of work mining and take seconds.
        ProofOfWork proofOfWork = new ProofOfWork(new byte[20], 0, null, 0, new byte[72], 0);
        return new NetworkEnvelope(new HashCashV2Token(proofOfWork, 0), new Ping(1));
    }
}
