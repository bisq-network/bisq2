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

package network.misq.network.p2p.node;

import lombok.Getter;
import network.misq.network.p2p.message.Message;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Metrics {

    private final long created;
    private final AtomicLong lastUpdate = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong numMessagesSent = new AtomicLong();
    private final AtomicLong numMessagesReceived = new AtomicLong();

    public Metrics() {
        created = new Date().getTime();
    }

    public Date getCreationDate() {
        return new Date(created);
    }

    public long getAge() {
        return System.currentTimeMillis() - created;
    }

    public void sent(Message message) {
        lastUpdate.set(System.currentTimeMillis());
        sentBytes.addAndGet(message.serialize().length);
        numMessagesSent.incrementAndGet();
    }

    public void received(Message message) {
        lastUpdate.set(System.currentTimeMillis());
        receivedBytes.addAndGet(message.serialize().length);
        numMessagesReceived.incrementAndGet();
    }
}