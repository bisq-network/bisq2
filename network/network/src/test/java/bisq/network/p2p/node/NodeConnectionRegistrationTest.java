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

import bisq.common.network.Address;
import bisq.network.NetworkExecutors;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NodeConnectionRegistrationTest {
    private static final Address ADDRESS = Address.from("127.0.0.1", 1000);

    private MockedStatic<NetworkExecutors> networkExecutors;
    private Node node;
    private Node.Listener listener;

    @BeforeEach
    void setUp() {
        // Run listener notifications on the calling thread
        ThreadPoolExecutor notifyExecutor = mock(ThreadPoolExecutor.class);
        when(notifyExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });
        networkExecutors = mockStatic(NetworkExecutors.class);
        networkExecutors.when(NetworkExecutors::getNotifyExecutor).thenReturn(notifyExecutor);

        node = new Node(mock(NetworkId.class),
                true,
                mock(Node.Config.class),
                mock(BanList.class),
                mock(KeyBundleService.class),
                mock(TransportService.class),
                mock(NetworkLoadSnapshot.class),
                mock(AuthorizationService.class));
        listener = mock(Node.Listener.class);
        node.addListener(listener);
    }

    @AfterEach
    void tearDown() {
        networkExecutors.close();
    }

    // A connection whose entry was already replaced must not remove the entry of the newer connection when it closes.
    // Otherwise the newer connection keeps running outside of the map, and its close is never reported.
    @Test
    void closingReplacedConnectionKeepsEntryOfNewerConnection() {
        InboundConnection replaced = createInboundConnection();
        InboundConnection newer = createInboundConnection();
        node.getInboundConnectionsByAddress().put(ADDRESS, newer);

        node.handleConnectionClosed(replaced, CloseReason.CLOSE_MSG_RECEIVED);

        assertSame(newer, node.getInboundConnectionsByAddress().get(ADDRESS));
        verify(listener, never()).onDisconnect(any(), any());

        node.handleConnectionClosed(newer, CloseReason.CLOSE_MSG_RECEIVED);

        assertSame(null, node.getInboundConnectionsByAddress().get(ADDRESS));
        verify(listener).onDisconnect(newer, CloseReason.CLOSE_MSG_RECEIVED);
    }

    @Test
    void newInboundConnectionReplacesPreviousAndReportsItOnce() {
        InboundConnection previous = createInboundConnection();
        InboundConnection newer = createInboundConnection();
        node.registerInboundConnection(previous);

        node.registerInboundConnection(newer);

        assertSame(newer, node.getInboundConnectionsByAddress().get(ADDRESS));
        verify(previous).shutdown(CloseReason.MAYBE_STALE_CONNECTION);
        var order = inOrder(listener);
        order.verify(listener).onConnection(previous);
        order.verify(listener).onDisconnect(previous, CloseReason.MAYBE_STALE_CONNECTION);
        order.verify(listener).onConnection(newer);
        verify(listener, times(1)).onDisconnect(any(), any());
    }

    @Test
    void duplicateOutboundConnectionIsClosedAndExistingIsKept() {
        OutboundConnection existing = createOutboundConnection();
        OutboundConnection duplicate = createOutboundConnection();
        assertSame(existing, node.registerOutboundConnection(ADDRESS, existing));

        assertSame(existing, node.registerOutboundConnection(ADDRESS, duplicate));

        assertSame(existing, node.getOutboundConnectionsByAddress().get(ADDRESS));
        verify(duplicate).shutdown(CloseReason.DUPLICATE_CONNECTION);
        verify(listener).onConnection(existing);
        verify(listener, never()).onConnection(duplicate);
        verify(listener, never()).onDisconnect(any(), any());
    }

    @Test
    void repeatedCloseOfRegisteredConnectionIsReportedOnce() {
        OutboundConnection connection = createOutboundConnection();
        node.registerOutboundConnection(ADDRESS, connection);

        node.handleConnectionClosed(connection, CloseReason.EXCEPTION);
        node.handleConnectionClosed(connection, CloseReason.EXCEPTION);

        assertSame(null, node.getOutboundConnectionsByAddress().get(ADDRESS));
        verify(listener, times(1)).onDisconnect(connection, CloseReason.EXCEPTION);
    }

    // Mirrors Connection.shutdown, which reports the close to its node
    private InboundConnection createInboundConnection() {
        InboundConnection connection = mock(InboundConnection.class);
        when(connection.getPeerAddress()).thenReturn(ADDRESS);
        doAnswer(invocation -> {
            node.handleConnectionClosed(connection, invocation.getArgument(0));
            return null;
        }).when(connection).shutdown(any());
        return connection;
    }

    private OutboundConnection createOutboundConnection() {
        OutboundConnection connection = mock(OutboundConnection.class);
        when(connection.getPeerAddress()).thenReturn(ADDRESS);
        doAnswer(invocation -> {
            node.handleConnectionClosed(connection, invocation.getArgument(0));
            return null;
        }).when(connection).shutdown(any());
        return connection;
    }
}
