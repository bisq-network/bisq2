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

package bisq.network.p2p.services.data.broadcast;

import bisq.common.timer.Scheduler;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Broadcasts queued messages one at a time, with a pause between two messages, and only while a broadcaster has a
 * connection.
 * <p>
 * A connection sends with a queue of 100 messages and a few threads. If the queue is full, the connection rejects the
 * message and closes (see Connection and Node.sendAsync), and the messages in its queue are lost. A broadcast sends a
 * message on every connection, so a node must not broadcast many messages at once.
 */
@Slf4j
public class ThrottledBroadcastQueue {
    private final Collection<Broadcaster> broadcasters;
    private final long intervalMs;
    private final Queue<BroadcastMessage> queue = new ConcurrentLinkedQueue<>();
    @Nullable
    private Scheduler scheduler;

    // broadcasters: the current broadcasters of the node. The queue reads the collection at each broadcast.
    public ThrottledBroadcastQueue(Collection<Broadcaster> broadcasters, long intervalMs) {
        this.broadcasters = broadcasters;
        this.intervalMs = intervalMs;
    }

    public synchronized void addAll(Collection<? extends BroadcastMessage> messages) {
        queue.addAll(messages);
        if (scheduler == null) {
            scheduler = Scheduler.run(this::broadcastNext)
                    .host(this)
                    .runnableName("broadcastNext")
                    .periodically(intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        queue.clear();
    }

    // A broadcast without connection reaches no peer, so the message waits until a broadcaster has a connection
    @VisibleForTesting
    void broadcastNext() {
        if (broadcasters.stream().noneMatch(Broadcaster::hasConnections)) {
            return;
        }
        BroadcastMessage message = queue.poll();
        if (message != null) {
            broadcasters.forEach(broadcaster -> broadcaster.broadcast(message));
        }
    }

    @VisibleForTesting
    int size() {
        return queue.size();
    }
}
