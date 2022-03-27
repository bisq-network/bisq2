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

package bisq.network.p2p.node;

import bisq.network.p2p.message.NetworkEnvelope;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@ToString
public class Metrics {

    private final long created;
    private final AtomicLong lastUpdate = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong numMessagesSent = new AtomicLong();
    private final AtomicLong numMessagesReceived = new AtomicLong();
    private final List<Long> rrtList = new CopyOnWriteArrayList<>();

    public Metrics() {
        created = new Date().getTime();
    }

    public Date getCreationDate() {
        return new Date(created);
    }

    public long getAge() {
        return System.currentTimeMillis() - created;
    }

    public void onSent(NetworkEnvelope networkEnvelope) {
        lastUpdate.set(System.currentTimeMillis());
        sentBytes.addAndGet(networkEnvelope.toProto().getSerializedSize());
        numMessagesSent.incrementAndGet();
    }

    public void onReceived(NetworkEnvelope networkEnvelope) {
        lastUpdate.set(System.currentTimeMillis());
        receivedBytes.addAndGet(networkEnvelope.toProto().getSerializedSize());
        numMessagesReceived.incrementAndGet();
    }

    public void addRtt(long value) {
        this.rrtList.add(value);
    }

    public double getAverageRtt() {
        return rrtList.stream().mapToLong(e -> e).average().orElse(0d);
    }
}