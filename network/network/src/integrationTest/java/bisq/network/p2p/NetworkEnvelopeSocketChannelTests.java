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

package bisq.network.p2p;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.transport.Transport;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkEnvelopeSocketChannelTests {

    private final Path tmpDir = FileUtils.createTempDir();
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel clientSocketChannel;
    private SocketChannel serverToClientSocketChannel;
    private NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel;

    public NetworkEnvelopeSocketChannelTests() throws IOException {
    }

    @BeforeEach
    void setUp() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress socketAddress = new InetSocketAddress(
                InetAddress.getLocalHost(), NetworkUtils.findFreeSystemPort()
        );
        serverSocketChannel.socket().bind(socketAddress);

        clientSocketChannel = SocketChannel.open();
        clientSocketChannel.connect(socketAddress);
        networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(clientSocketChannel);

        serverToClientSocketChannel = serverSocketChannel.accept();
    }

    @AfterEach
    void tearDown() throws IOException {
        clientSocketChannel.close();
        serverToClientSocketChannel.close();
        serverSocketChannel.close();
    }

    @Test
    void parseOneMessage() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        OutputStream outputStream = Channels.newOutputStream(serverToClientSocketChannel);
        requestNetworkEnvelope.toProto().writeDelimitedTo(outputStream);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    @Test
    void parseTwoFullMessages() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        OutputStream outputStream = Channels.newOutputStream(serverToClientSocketChannel);
        requestNetworkEnvelope.toProto().writeDelimitedTo(outputStream);
        requestNetworkEnvelope.toProto().writeDelimitedTo(outputStream);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes)
                .hasSize(2)
                .containsExactly(requestNetworkEnvelope, requestNetworkEnvelope);
    }

    @Test
    void parsePartialMessageFirstAndRestLater() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        requestNetworkEnvelope.toProto().writeDelimitedTo(byteArrayOutputStream);

        byte[] messageInBytes = byteArrayOutputStream.toByteArray();
        // 176 bytes
        ByteBuffer firstHundredBytesBuffer = ByteBuffer.wrap(messageInBytes, 0, 100);
        serverToClientSocketChannel.write(firstHundredBytesBuffer);

        networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

        ByteBuffer remainingBytesBuffer = ByteBuffer.wrap(messageInBytes, 100, messageInBytes.length - 100);
        serverToClientSocketChannel.write(remainingBytesBuffer);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    @Test
    void parsePartialMessageInThreeRounds() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        requestNetworkEnvelope.toProto().writeDelimitedTo(byteArrayOutputStream);

        byte[] messageInBytes = byteArrayOutputStream.toByteArray();
        // 176 bytes
        ByteBuffer firstFiftyBytesBuffer = ByteBuffer.wrap(messageInBytes, 0, 50);
        serverToClientSocketChannel.write(firstFiftyBytesBuffer);

        networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

        ByteBuffer secondFiftyBytesBuffer = ByteBuffer.wrap(messageInBytes, 50, 50);
        serverToClientSocketChannel.write(secondFiftyBytesBuffer);

        networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

        ByteBuffer remainingBytesBuffer = ByteBuffer.wrap(messageInBytes, 100, messageInBytes.length - 100);
        serverToClientSocketChannel.write(remainingBytesBuffer);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    @Test
    void endOfStreamTest() throws IOException {
        serverToClientSocketChannel.close();
        networkEnvelopeSocketChannel.receiveNetworkEnvelopes();

        assertThat(clientSocketChannel.isOpen()).isFalse();
    }

    @Test
    void parseTooLargeForBufferMessage() throws IOException {
        NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = new NetworkEnvelopeSocketChannel(clientSocketChannel, 100);
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        OutputStream outputStream = Channels.newOutputStream(serverToClientSocketChannel);
        requestNetworkEnvelope.toProto().writeDelimitedTo(outputStream);

        // read first 100 bytes
        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).isEmpty();

        // read remaining
        receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    private NetworkEnvelope createHandshakeRequestMessage() {
        List<Transport.Type> supportedTransportTypes = new ArrayList<>(1);
        supportedTransportTypes.add(Transport.Type.CLEAR);

        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Load.INITIAL_LOAD);
        AuthorizationService authorizationService = createAuthorizationService();

        Capability responderCapability = new Capability(Address.localHost(1234), supportedTransportTypes);

        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                responderCapability.getAddress().getFullAddress(),
                0);
        return new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
    }

    private AuthorizationService createAuthorizationService() {
        String baseDir = tmpDir.toAbsolutePath().toString();
        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService);
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }
}
