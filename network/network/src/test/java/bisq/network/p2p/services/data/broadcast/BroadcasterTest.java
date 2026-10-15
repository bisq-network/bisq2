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

import bisq.common.network.TransportType;
import bisq.network.p2p.node.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BroadcasterTest {
    // NetworkService completes its initialization when the node of the first transport is ready. A broadcast on the
    // node of another transport, which is still starting, must not throw, because the callers broadcast on all
    // transports in one loop.
    @Test
    void broadcastOnNodeWhichIsNotInitializedReachesNoPeerAndDoesNotThrow() {
        Node node = mock(Node.class);
        when(node.isInitialized()).thenReturn(false);
        when(node.getTransportType()).thenReturn(TransportType.I2P);
        Broadcaster broadcaster = new Broadcaster(node);

        BroadcastResult result = broadcaster.broadcast(mock(BroadcastMessage.class)).join();
        BroadcastResult resultOfRelay = broadcaster.reBroadcast(mock(BroadcastMessage.class)).join();

        assertEquals(new BroadcastResult(0, 0, 0), result);
        assertEquals(new BroadcastResult(0, 0, 0), resultOfRelay);
        verify(node, never()).sendAsync(any(), any(bisq.network.p2p.node.Connection.class));
    }
}
