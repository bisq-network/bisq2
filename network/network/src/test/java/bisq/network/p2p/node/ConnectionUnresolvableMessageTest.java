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
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.envelope.ParseMemoryBudget;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.pow.ProofOfWork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A connection drops a message which it cannot parse and keeps reading. The peer is often only a relay of such a
 * message, so closing the connection would let the creator of the message disconnect honest relays (see
 * docs/specifications/network/validation-at-parsing.md).
 */
class ConnectionUnresolvableMessageTest {
    private final BlockingQueue<EnvelopePayloadMessage> handledMessages = new LinkedBlockingQueue<>();
    private final List<CloseReason> closeReasons = new ArrayList<>();
    private ServerSocket serverSocket;
    private Socket peerSocket;
    private final ConnectionThrottle connectionThrottle = mock(ConnectionThrottle.class);
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
                connectionThrottle,
                new ParseMemoryBudget(),
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
    void messageWhichCannotBeParsedIsDroppedAndTheConnectionStaysOpen() throws IOException, InterruptedException {
        bisq.network.protobuf.NetworkEnvelope validEnvelope = createEnvelope(1).toProto(false);
        // Valid protobuf, but no message type: NetworkEnvelope.fromProto fails
        bisq.network.protobuf.NetworkEnvelope envelopeWithoutMessage = validEnvelope.toBuilder()
                .setNetworkMessage(bisq.network.protobuf.EnvelopePayloadMessage.newBuilder())
                .build();

        OutputStream outputStream = peerSocket.getOutputStream();
        envelopeWithoutMessage.writeDelimitedTo(outputStream);
        createEnvelope(2).toProto(false).writeDelimitedTo(outputStream);
        outputStream.flush();

        EnvelopePayloadMessage handled = handledMessages.poll(10, TimeUnit.SECONDS);
        assertInstanceOf(Ping.class, handled);
        assertEquals(2, ((Ping) handled).getNonce());
        assertEquals(1, connection.getNumUnresolvableMessages().get());
        assertFalse(connection.isStopped());
        assertEquals(List.of(), closeReasons);
        // The dropped message is throttled like the Ping, so a peer can send such messages only at the throttled rate
        verify(connectionThrottle, times(2)).throttleReceiveMessage();
    }

    private static NetworkEnvelope createEnvelope(int nonce) {
        ProofOfWork proofOfWork = new ProofOfWork(new byte[20], 0, null, 0, new byte[72], 0);
        return new NetworkEnvelope(new HashCashV2Token(proofOfWork, 0), new Ping(nonce));
    }
}
