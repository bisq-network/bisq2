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
import bisq.common.network.TransportType;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A connection closes when the peer announces a message above the size limit. It does not wait for the bytes of the
 * message (see docs/specifications/network/message-framing.md).
 */
class ConnectionMessageSizeLimitTest {
    private final BlockingQueue<EnvelopePayloadMessage> handledMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<CloseReason> closeReasons = new LinkedBlockingQueue<>();
    private ServerSocket serverSocket;
    private Socket peerSocket;
    private Connection connection;

    @BeforeEach
    void setUp() throws IOException {
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        peerSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());
        Socket socket = serverSocket.accept();
        Capability peersCapability = new Capability(Capability.VERSION,
                Address.from("127.0.0.1", peerSocket.getLocalPort()),
                new ArrayList<>(List.of(TransportType.CLEAR)),
                new ArrayList<>(),
                "2.1.14");
        connection = new InboundConnection(mock(AuthorizationService.class),
                "connectionId",
                socket,
                peersCapability,
                new NetworkLoadSnapshot(),
                new ConnectionMetrics(),
                mock(ConnectionThrottle.class),
                new Connection.Handler() {
                    @Override
                    public boolean isMessageAuthorized(EnvelopePayloadMessage envelopePayloadMessage,
                                                       AuthorizationToken authorizationToken,
                                                       Connection connection) {
                        return true;
                    }

                    @Override
                    public void handleNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                                     Connection connection) {
                        handledMessages.add(envelopePayloadMessage);
                    }

                    @Override
                    public void handleConnectionClosed(Connection connection, CloseReason closeReason) {
                        closeReasons.add(closeReason);
                    }
                },
                (connection, exception) -> {
                });
    }

    @AfterEach
    void tearDown() throws IOException {
        connection.shutdown(CloseReason.SHUTDOWN);
        peerSocket.close();
        serverSocket.close();
    }

    @Test
    void lengthPrefixAboveTheSizeLimitClosesTheConnection() throws IOException, InterruptedException {
        // The peer sends only the length prefix and keeps the socket open.
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(peerSocket.getOutputStream());
        codedOutputStream.writeUInt32NoTag(NetworkEnvelope.MAX_SERIALIZED_SIZE + 1);
        codedOutputStream.flush();

        CloseReason closeReason = closeReasons.poll(10, TimeUnit.SECONDS);
        assertThat(closeReason).isEqualTo(CloseReason.EXCEPTION);
        assertThat(closeReason.getException()).containsInstanceOf(InvalidProtocolBufferException.class);
        assertThat(connection.isStopped()).isTrue();
        assertThat(handledMessages).isEmpty();
    }
}
