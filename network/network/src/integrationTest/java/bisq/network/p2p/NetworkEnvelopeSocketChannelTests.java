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

import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileUtils;
import bisq.common.network.DefaultClearNetLocalAddressFacade;
import bisq.common.util.NetworkUtils;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
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
import java.util.Optional;
import java.util.Set;

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
        requestNetworkEnvelope.writeDelimitedTo(outputStream);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    @Test
    void parseTwoFullMessages() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        OutputStream outputStream = Channels.newOutputStream(serverToClientSocketChannel);
        requestNetworkEnvelope.writeDelimitedTo(outputStream);
        requestNetworkEnvelope.writeDelimitedTo(outputStream);

        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes)
                .hasSize(2)
                .containsExactly(requestNetworkEnvelope, requestNetworkEnvelope);
    }

    @Test
    void parsePartialMessageFirstAndRestLater() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createHandshakeRequestMessage();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        requestNetworkEnvelope.writeDelimitedTo(byteArrayOutputStream);

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
        requestNetworkEnvelope.writeDelimitedTo(byteArrayOutputStream);

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
        requestNetworkEnvelope.writeDelimitedTo(outputStream);

        // read first 100 bytes
        List<NetworkEnvelope> receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).isEmpty();

        // read remaining
        receivedNetworkEnvelopes = networkEnvelopeSocketChannel.receiveNetworkEnvelopes();
        assertThat(receivedNetworkEnvelopes).containsExactly(requestNetworkEnvelope);
    }

    private NetworkEnvelope createHandshakeRequestMessage() {
        List<TransportType> supportedTransportTypes = new ArrayList<>(1);
        supportedTransportTypes.add(TransportType.CLEAR);

        Capability peerCapability = createCapability(DefaultClearNetLocalAddressFacade.toLocalHostAddress(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationService authorizationService = createAuthorizationService();

        Capability responderCapability = createCapability(DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234), supportedTransportTypes);

        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                responderCapability.getAddress().getFullAddress(),
                0, new ArrayList<>());
        return new NetworkEnvelope(token, request);
    }

   /* private AuthorizationService createAuthorizationService() {
        String baseDir = tmpDir.toAbsolutePath().toString();
        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService, mock(SecurityService.Config.class));
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }*/

    private AuthorizationService createAuthorizationService() {
        return new AuthorizationService(new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH));
    }

    private static Capability createCapability(Address address, List<TransportType> supportedTransportTypes) {
        return new Capability(Capability.VERSION, address, supportedTransportTypes, new ArrayList<>(), ApplicationVersion.getVersion().getVersionAsString());
    }
}
