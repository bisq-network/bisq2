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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ThrottledBroadcastQueueTest {
    // The scheduler of the queue does not run during a test, so only the test calls broadcastNext
    private static final long INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

    private final Set<Broadcaster> broadcasters = new CopyOnWriteArraySet<>();
    private final ThrottledBroadcastQueue queue = new ThrottledBroadcastQueue(broadcasters, INTERVAL_MS);

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    void broadcastsOneMessageAtATimeInTheOrderOfTheQueueOnAllBroadcasters() {
        RecordingBroadcaster tor = new RecordingBroadcaster(true);
        RecordingBroadcaster i2p = new RecordingBroadcaster(true);
        broadcasters.add(tor);
        broadcasters.add(i2p);
        BroadcastMessage first = mock(BroadcastMessage.class);
        BroadcastMessage second = mock(BroadcastMessage.class);
        queue.addAll(List.of(first, second));

        assertTrue(tor.broadcastMessages.isEmpty());
        queue.broadcastNext();
        assertEquals(List.of(first), tor.broadcastMessages);
        assertEquals(List.of(first), i2p.broadcastMessages);

        queue.broadcastNext();
        queue.broadcastNext();
        assertEquals(List.of(first, second), tor.broadcastMessages);
        assertEquals(List.of(first, second), i2p.broadcastMessages);
        assertEquals(0, queue.size());
    }

    // A broadcast without connection reaches no peer
    @Test
    void messageWaitsUntilABroadcasterHasAConnection() {
        RecordingBroadcaster broadcaster = new RecordingBroadcaster(false);
        broadcasters.add(broadcaster);
        BroadcastMessage message = mock(BroadcastMessage.class);
        queue.addAll(List.of(message));

        queue.broadcastNext();
        assertTrue(broadcaster.broadcastMessages.isEmpty());
        assertEquals(1, queue.size());

        broadcaster.hasConnections = true;
        queue.broadcastNext();
        assertEquals(List.of(message), broadcaster.broadcastMessages);
    }

    private static class RecordingBroadcaster extends Broadcaster {
        private final List<BroadcastMessage> broadcastMessages = new ArrayList<>();
        private boolean hasConnections;

        RecordingBroadcaster(boolean hasConnections) {
            super(null);
            this.hasConnections = hasConnections;
        }

        @Override
        public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
            broadcastMessages.add(broadcastMessage);
            return CompletableFuture.completedFuture(new BroadcastResult(1, 0, 0));
        }

        @Override
        public boolean hasConnections() {
            return hasConnections;
        }
    }
}
