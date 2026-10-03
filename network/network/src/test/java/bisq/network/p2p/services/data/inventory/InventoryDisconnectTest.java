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

package bisq.network.p2p.services.data.inventory;

import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryDisconnectTest {
    @Test
    void disconnectCancelsOnlyThatConnectionsPendingRequestAndFreesItsSlot() {
        Node node = mock(Node.class);
        when(node.sendAsync(any(), any(Connection.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(1)));
        Connection disconnected = connection("disconnected");
        Connection remaining = connection("remaining");
        when(node.getNumConnections()).thenReturn(1);
        TestInventoryService service = new TestInventoryService(node);
        try {
            CompletableFuture<InventoryResponse> disconnectedRequest = service.startRequest(disconnected);
            CompletableFuture<InventoryResponse> remainingRequest = service.startRequest(remaining);
            assertEquals(2, service.getNumPendingRequests());

            service.onDisconnect(disconnected, CloseReason.SHUTDOWN);

            assertTrue(disconnectedRequest.isCancelled());
            assertFalse(remainingRequest.isDone());
            assertFalse(service.hasPendingRequest(disconnected.getId()));
            assertTrue(service.hasPendingRequest(remaining.getId()));
            assertEquals(1, service.getNumPendingRequests());
            assertEquals(1, service.getNumPendingInventoryRequests().get());
            service.onDisconnect(disconnected, CloseReason.SHUTDOWN);
            assertEquals(1, service.getNumPendingRequests());
        } finally {
            service.shutdown();
        }
    }

    private static Connection connection(String id) {
        Connection connection = mock(Connection.class, RETURNS_DEEP_STUBS);
        when(connection.getId()).thenReturn(id);
        return connection;
    }

    private static class TestInventoryService extends InventoryService {
        TestInventoryService(Node node) {
            super(new Config(2000, 600_000, 2, 4, 5, 2, 2, List.of(InventoryFilterType.HASH_SET_V2)),
                    node, mock(PeerGroupManager.class, RETURNS_DEEP_STUBS), mock(DataService.class), Set.of());
        }

        CompletableFuture<InventoryResponse> startRequest(Connection connection) {
            InventoryRequest request = mock(InventoryRequest.class);
            String id = connection.getId();
            when(request.getRequestId()).thenReturn(id);
            return request(connection, request);
        }
    }
}
