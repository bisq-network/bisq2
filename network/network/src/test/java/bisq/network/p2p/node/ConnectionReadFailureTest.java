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

import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.envelope.ParseMemoryBudget;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.protobuf.NetworkEnvelope;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * A read failure ends the read loop of a connection and closes it. After a framing error the start of the next message
 * is unknown, so the connection must not try to read further (see docs/specifications/network/message-framing.md).
 */
class ConnectionReadFailureTest {
    private final Socket socket = mock(Socket.class);
    private final Connection.Handler handler = mock(Connection.Handler.class);
    private final ConnectionThrottle connectionThrottle = mock(ConnectionThrottle.class);
    private final CountDownLatch startReading = new CountDownLatch(1);
    private final CompletableFuture<Exception> readFailure = new CompletableFuture<>();
    @Nullable
    private Connection connection;

    @AfterEach
    void tearDown() {
        startReading.countDown();
        if (connection != null) {
            connection.shutdown(CloseReason.SHUTDOWN);
        }
    }

    @Test
    void shutsDownOnSocketErrorWithoutRetrying() throws Exception {
        InputStream inputStream = mock(InputStream.class);
        SocketException exception = new SocketException("Connection reset");
        when(inputStream.read()).thenThrow(exception).thenReturn(-1);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(exception).thenReturn(-1);

        startConnection(inputStream);

        assertSame(exception, awaitReadFailure());
    }

    @Test
    void shutsDownOnOversizedFrameWithoutReadingItsPayload() throws Exception {
        byte[] payload = new byte[bisq.network.p2p.message.NetworkEnvelope.MAX_SERIALIZED_SIZE + 1];
        ByteArrayOutputStream embeddedMessage = new ByteArrayOutputStream();
        NetworkEnvelope.newBuilder().setVersion(1).build().writeDelimitedTo(embeddedMessage);
        System.arraycopy(embeddedMessage.toByteArray(), 0, payload, 0, embeddedMessage.size());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(frame(payload));

        startConnection(inputStream);

        assertInstanceOf(InvalidProtocolBufferException.class, awaitReadFailure());
        assertEquals(payload.length, inputStream.available());
    }

    @Test
    void shutsDownOnMalformedFrameWithUnreadPayload() throws Exception {
        byte[] payload = new byte[5000];
        Arrays.fill(payload, (byte) 0x08);
        payload[0] = 0; // Invalid protobuf tag before the rest of the frame is read.
        ByteArrayInputStream inputStream = new ByteArrayInputStream(frame(payload));

        startConnection(inputStream);

        assertInstanceOf(InvalidProtocolBufferException.class, awaitReadFailure());
    }

    private void startConnection(InputStream inputStream) throws IOException {
        when(socket.getInputStream()).thenReturn(new FilterInputStream(inputStream) {
            @Override
            public int read() throws IOException {
                awaitStart();
                return super.read();
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                awaitStart();
                return super.read(bytes, offset, length);
            }

            // Let the constructor finish assigning the read task before it can fail.
            private void awaitStart() throws IOException {
                try {
                    startReading.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException(exception);
                }
            }
        });
        when(socket.getOutputStream()).thenReturn(OutputStream.nullOutputStream());
        Capability capability = new Capability(Capability.VERSION,
                LocalHostAddressTypeFacade.toLocalHostAddress(1234),
                new ArrayList<>(List.of(TransportType.CLEAR)),
                new ArrayList<>(),
                "2.1.9");
        connection = new Connection(mock(AuthorizationService.class),
                "test-connection",
                ConcurrentHashMap.newKeySet(),
                socket,
                capability,
                new NetworkLoadSnapshot(),
                new ConnectionMetrics(),
                connectionThrottle,
                new ParseMemoryBudget(),
                handler,
                (failedConnection, exception) -> readFailure.complete(exception)) {
        };
        startReading.countDown();
    }

    private Exception awaitReadFailure() throws Exception {
        Exception exception = readFailure.get(5, TimeUnit.SECONDS);
        assertFalse(connection.isRunning());
        verify(socket).close();
        verify(handler).handleConnectionClosed(connection, CloseReason.EXCEPTION);
        verifyNoMoreInteractions(handler);
        verifyNoInteractions(connectionThrottle);
        return exception;
    }

    private byte[] frame(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(outputStream);
        codedOutput.writeUInt32NoTag(payload.length);
        codedOutput.writeRawBytes(payload);
        codedOutput.flush();
        return outputStream.toByteArray();
    }
}
