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
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Getter
@ToString
public class ConnectionMetrics {
    private final long created;
    private final AtomicLong lastUpdate = new AtomicLong();
    // onSent is called from the connection's send thread pool (which grows to several threads), onReceived
    // from its read thread and clear from whichever thread triggers the shutdown, while readers
    // (NetworkLoadService, the desktop UI, the api services and toString) traverse the maps concurrently.
    // ConcurrentSkipListMap makes computeIfAbsent atomic and gives weakly consistent iterators, and it keeps
    // the ascending key order sumOfLastMinute relies on.
    private final ConcurrentNavigableMap<Integer, AtomicLong> numMessagesSentPerMinute = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> sentBytesPerMinute = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> spentSendMessageTimePerMinute = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> deserializeTimePerMinute = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> numMessagesReceivedPerMinute = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> receivedBytesPerMinute = new ConcurrentSkipListMap<>();
    private final Map<String, AtomicLong> numSentMessagesByClassName = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> numReceivedMessagesByClassName = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> numSentDistributedDataByClassName = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> numReceivedDistributedDataByClassName = new ConcurrentHashMap<>();

    private final List<Long> rrtList = new CopyOnWriteArrayList<>();

    public ConnectionMetrics() {
        created = System.currentTimeMillis();
    }

    public Date getCreationDate() {
        return new Date(created);
    }

    public long getAge() {
        return System.currentTimeMillis() - created;
    }

    public void onSent(NetworkEnvelope networkEnvelope, long spentTime) {
        long now = System.currentTimeMillis();
        lastUpdate.set(now);

        int ageInMinutes = getAgeInMinutes(now);
        sentBytesPerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .addAndGet(networkEnvelope.getSerializedSize());

        numMessagesSentPerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .incrementAndGet();

        spentSendMessageTimePerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .addAndGet(spentTime);

        EnvelopePayloadMessage envelopePayloadMessage = networkEnvelope.getEnvelopePayloadMessage();
        String name = ClassUtils.getClassName(envelopePayloadMessage.getClass());
        numSentMessagesByClassName.computeIfAbsent(name, key -> new AtomicLong())
                .incrementAndGet();

        if (envelopePayloadMessage instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            String distributedDataName = addAuthenticatedDataRequest.getDistributedData().getClassName();
            numSentDistributedDataByClassName.computeIfAbsent(distributedDataName, key -> new AtomicLong())
                    .incrementAndGet();
        }
    }

    public void onReceived(NetworkEnvelope networkEnvelope, long deserializeTime) {
        long now = System.currentTimeMillis();
        lastUpdate.set(now);

        int ageInMinutes = getAgeInMinutes(now);
        receivedBytesPerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .addAndGet(networkEnvelope.getSerializedSize());

        numMessagesReceivedPerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .incrementAndGet();

        deserializeTimePerMinute.computeIfAbsent(ageInMinutes, key -> new AtomicLong())
                .addAndGet(deserializeTime);

        EnvelopePayloadMessage envelopePayloadMessage = networkEnvelope.getEnvelopePayloadMessage();
        String name = ClassUtils.getClassName(envelopePayloadMessage.getClass());
        numReceivedMessagesByClassName.computeIfAbsent(name, key -> new AtomicLong())
                .incrementAndGet();

        if (envelopePayloadMessage instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            String distributedDataName = addAuthenticatedDataRequest.getDistributedData().getClassName();
            numReceivedDistributedDataByClassName.computeIfAbsent(distributedDataName, key -> new AtomicLong())
                    .incrementAndGet();
        }
    }

    public void addRtt(long value) {
        this.rrtList.add(value);
    }

    public double getAverageRtt() {
        return rrtList.stream().mapToLong(e -> e).average().orElse(0d);
    }

    public long getSentBytes() {
        return sumOf(sentBytesPerMinute);
    }

    public long getNumMessagesSent() {
        return sumOf(numMessagesSentPerMinute);
    }

    public long getSpentSendMessageTimePerMinute() {
        return sumOf(spentSendMessageTimePerMinute);
    }

    public long getReceivedBytes() {
        return sumOf(receivedBytesPerMinute);
    }

    public long getNumMessagesReceived() {
        return sumOf(numMessagesReceivedPerMinute);
    }

    public long getDeserializeTimePerMinute() {
        return sumOf(deserializeTimePerMinute);
    }


    public long getNumMessagesSentOfLast5Minutes() {
        return getNumMessagesSentOfLastMinutes(5);
    }

    public long getSentBytesOfLast5Minutes() {
        return getSentBytesOfLastMinutes(5);
    }

    public long getSpentSendMessageTimeOfLast5Minutes() {
        return getSpentSendMessageTimeOfLastMinutes(5);
    }

    public long getReceivedBytesOfLast5Minutes() {
        return getReceivedBytesOfLastMinutes(5);
    }

    public long getDeserializeTimeOfLast5Minutes() {
        return getDeserializeTimeOfLastMinutes(5);
    }

    public long getNumMessagesReceivedOfLast5Minutes() {
        return getNumMessagesReceivedOfLastMinutes(5);
    }


    public long getNumMessagesSentOfLastHour() {
        return getNumMessagesSentOfLastMinutes(60);
    }

    public long getSentBytesOfLastHour() {
        return getSentBytesOfLastMinutes(60);
    }

    public long getSpentSendMessageTimeOfLastHour() {
        return getSpentSendMessageTimeOfLastMinutes(60);
    }

    public long getReceivedBytesOfLastHour() {
        return getReceivedBytesOfLastMinutes(60);
    }

    public long getDeserializeTimeOfLastHour() {
        return getDeserializeTimeOfLastMinutes(60);
    }

    public long getNumMessagesReceivedOfLastHour() {
        return getNumMessagesReceivedOfLastMinutes(60);
    }

    public long getNumMessagesSentOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(numMessagesSentPerMinute, lastMinutes);
    }

    public long getSentBytesOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(sentBytesPerMinute, lastMinutes);
    }

    public long getSpentSendMessageTimeOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(spentSendMessageTimePerMinute, lastMinutes);
    }

    public long getNumMessagesReceivedOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(numMessagesReceivedPerMinute, lastMinutes);
    }

    public long getReceivedBytesOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(receivedBytesPerMinute, lastMinutes);
    }

    public long getDeserializeTimeOfLastMinutes(int lastMinutes) {
        return sumOfLastMinute(deserializeTimePerMinute, lastMinutes);
    }

    public void clear() {
        numMessagesSentPerMinute.clear();
        sentBytesPerMinute.clear();
        spentSendMessageTimePerMinute.clear();
        deserializeTimePerMinute.clear();
        numMessagesReceivedPerMinute.clear();
        receivedBytesPerMinute.clear();
        numSentMessagesByClassName.clear();
        numReceivedMessagesByClassName.clear();
        numSentDistributedDataByClassName.clear();
        numReceivedDistributedDataByClassName.clear();
        rrtList.clear();
    }

    private long sumOf(ConcurrentNavigableMap<Integer, AtomicLong> map) {
        return map.values().stream().mapToLong(AtomicLong::get).sum();
    }

    private long sumOfLastMinute(ConcurrentNavigableMap<Integer, AtomicLong> map, int lastMinutes) {
        // The buckets are keyed by the age in minutes, so the descending map returns the most recent ones first.
        return map.descendingMap().values().stream()
                .limit(lastMinutes)
                .mapToLong(AtomicLong::get)
                .sum();
    }

    private int getAgeInMinutes(long now) {
        // The cast has to be applied after the division, otherwise the millisecond delta gets truncated to int
        // and wraps after about 25 days, which would make the bucket keys negative.
        return (int) ((now - created) / 60000);
    }
}