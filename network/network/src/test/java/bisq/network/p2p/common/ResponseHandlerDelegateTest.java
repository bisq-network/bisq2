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

package bisq.network.p2p.common;

import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseHandlerDelegateTest {
    private static final long TIMEOUT = 60_000;

    // Client code which reacts to the cancellation of a request on a closed connection runs on the closing thread.
    // It must find the request already removed, and a retry on another connection must stay pending.
    @Test
    void stagesRunAfterRemovalOnDisconnect() {
        Node node = createNode();
        ResponseHandlerDelegate<Request, Response> delegate = new ResponseHandlerDelegate<>(node, TIMEOUT, Response.class);
        Connection closed = createConnection("closed");
        Connection retry = createConnection("retry");
        AtomicReference<Boolean> pendingSeenByStage = new AtomicReference<>();

        delegate.request(closed, createRequest()).whenComplete((response, throwable) -> {
            pendingSeenByStage.set(delegate.hasPendingRequest(closed.getId()));
            delegate.request(retry, createRequest());
        });
        delegate.processOnDisconnect(closed, CloseReason.CLOSE_MSG_RECEIVED);

        assertEquals(Boolean.FALSE, pendingSeenByStage.get());
        assertFalse(delegate.hasPendingRequest(closed.getId()));
        assertTrue(delegate.hasPendingRequest(retry.getId()));
    }

    @Test
    void stagesRunAfterRemovalOnShutdown() {
        Node node = createNode();
        ResponseHandlerDelegate<Request, Response> delegate = new ResponseHandlerDelegate<>(node, TIMEOUT, Response.class);
        Connection connection = createConnection("connection");
        AtomicReference<Integer> pendingSeenByStage = new AtomicReference<>();

        delegate.request(connection, createRequest())
                .whenComplete((response, throwable) -> pendingSeenByStage.set(delegate.getNumPendingRequests()));
        delegate.shutdown();

        assertEquals(0, pendingSeenByStage.get());
        assertEquals(0, delegate.getNumPendingRequests());
    }

    private static Node createNode() {
        Node node = mock(Node.class);
        when(node.sendAsync(any(), any(Connection.class))).thenReturn(new CompletableFuture<>());
        return node;
    }

    private static Connection createConnection(String id) {
        Connection connection = mock(Connection.class);
        when(connection.getId()).thenReturn(id);
        return connection;
    }

    private static Request createRequest() {
        Request request = mock(Request.class);
        when(request.getRequestId()).thenReturn("request-id");
        return request;
    }
}
