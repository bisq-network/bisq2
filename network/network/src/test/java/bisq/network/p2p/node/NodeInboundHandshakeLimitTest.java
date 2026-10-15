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
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rule 1 of docs/specifications/network/inbound-handshakes.md.
 */
class NodeInboundHandshakeLimitTest {
    private static final int MAX_NUM_CONNECTED_PEERS = 2;

    private MockedStatic<NetworkExecutors> networkExecutors;
    private Node node;
    private final List<SilentPeerInput> inputs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ThreadPoolExecutor notifyExecutor = mock(ThreadPoolExecutor.class);
        when(notifyExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });
        networkExecutors = mockStatic(NetworkExecutors.class);
        networkExecutors.when(NetworkExecutors::getNotifyExecutor).thenReturn(notifyExecutor);

        Node.Config config = mock(Node.Config.class);
        when(config.getMaxNumConnectedPeers()).thenReturn(MAX_NUM_CONNECTED_PEERS);
        when(config.getTransportType()).thenReturn(TransportType.CLEAR);
        NetworkId networkId = mock(NetworkId.class);
        when(networkId.getAddressByTransportTypeMap()).thenReturn(new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, Address.from("127.0.0.1", 1000))));
        node = new Node(networkId,
                true,
                config,
                mock(BanList.class),
                mock(KeyBundleService.class),
                mock(TransportService.class),
                mock(NetworkLoadSnapshot.class),
                mock(AuthorizationService.class));
    }

    @AfterEach
    void tearDown() {
        inputs.forEach(SilentPeerInput::endOfStream);
        networkExecutors.close();
    }

    // A silent peer holds a thread until its socket times out. The executor also creates our outbound connections.
    @Test
    void inboundSocketBeyondTheLimitIsClosedAtOnce() throws Exception {
        List<Socket> sockets = new ArrayList<>();
        for (int i = 0; i < MAX_NUM_CONNECTED_PEERS; i++) {
            SilentPeerInput input = new SilentPeerInput();
            Socket socket = socketWith(input);
            sockets.add(socket);
            node.handleNewClientSocketAsync(socket, mock(Capability.class));
            input.awaitRead();
        }
        Socket socketBeyondTheLimit = socketWith(new SilentPeerInput());

        CompletableFuture<Void> result = node.handleNewClientSocketAsync(socketBeyondTheLimit, mock(Capability.class));

        assertThat(result).isCompletedExceptionally();
        verify(socketBeyondTheLimit).close();
        for (Socket socket : sockets) {
            verify(socket, never()).close();
        }
    }

    @Test
    void endedHandshakeReleasesItsPlace() throws Exception {
        List<CompletableFuture<Void>> handshakes = new ArrayList<>();
        for (int i = 0; i < MAX_NUM_CONNECTED_PEERS; i++) {
            SilentPeerInput input = new SilentPeerInput();
            handshakes.add(node.handleNewClientSocketAsync(socketWith(input), mock(Capability.class)));
            input.awaitRead();
        }
        // The peers close their sockets, so the handshakes fail and end
        inputs.forEach(SilentPeerInput::endOfStream);
        CompletableFuture.allOf(handshakes.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        SilentPeerInput input = new SilentPeerInput();
        Socket socket = socketWith(input);
        node.handleNewClientSocketAsync(socket, mock(Capability.class));

        input.awaitRead();
        verify(socket, never()).close();
    }

    private Socket socketWith(SilentPeerInput input) throws IOException {
        inputs.add(input);
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(input);
        when(socket.getOutputStream()).thenReturn(OutputStream.nullOutputStream());
        return socket;
    }

    // Sends nothing until the peer closes its socket
    private static class SilentPeerInput extends InputStream {
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            readStarted.countDown();
            try {
                closed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            return -1;
        }

        void awaitRead() throws InterruptedException {
            assertThat(readStarted.await(10, TimeUnit.SECONDS)).isTrue();
        }

        void endOfStream() {
            closed.countDown();
        }
    }
}
