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
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.envelope.NetworkEnvelopeSocketChannel;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.handshake.ConnectionHandshakeResponder;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionHandshakeResponderTest {

    private final Path tmpDir = FileUtils.createTempDir();
    private final BanList banList = mock(BanList.class);
    private final List<TransportType> supportedTransportTypes = new ArrayList<>(1);
    private final Capability responderCapability;
    private final AuthorizationService authorizationService;
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = mock(NetworkEnvelopeSocketChannel.class);
    private final ConnectionHandshakeResponder handshakeResponder;

    public ConnectionHandshakeResponderTest() throws IOException {
        supportedTransportTypes.add(TransportType.CLEAR);
        this.responderCapability = new Capability(Address.localHost(1234), supportedTransportTypes, new ArrayList<>());
        this.authorizationService = createAuthorizationService();
        this.handshakeResponder = new ConnectionHandshakeResponder(
                banList,
                responderCapability,
                new NetworkLoad(),
                authorizationService,
                networkEnvelopeSocketChannel);
    }

    private AuthorizationService createAuthorizationService() {
        return new AuthorizationService(new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH));
    }

    @Test
    void emptyInitialEnvelopes() throws IOException {
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(Collections.emptyList());
        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);
        assertThat(exception.getMessage()).contains("empty");
    }

    @Test
    void tooManyInitialEnvelopes() throws IOException {
        NetworkEnvelope requestNetworkEnvelope = createValidRequest();
        List<NetworkEnvelope> initialMessages = List.of(requestNetworkEnvelope, requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(initialMessages);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);
        assertThat(exception.getMessage())
                .contains("multiple")
                .contains("requests");
    }

    @Test
    void wrongEnvelopeVersion() throws IOException {
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                Address.localHost(1234).toString(),
                0, new ArrayList<>());
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.networkVersion + 1000, token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);
    }

    @Test
    void wrongNetworkMessage() throws IOException {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(responderCapability, new NetworkLoad());
        AuthorizationToken token = authorizationService.createToken(
                response,
                new NetworkLoad(),
                Address.localHost(1234).toString(),
                0, new ArrayList<>());
        NetworkEnvelope responseEnvelope = new NetworkEnvelope(token, response);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(responseEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);

        assertThat(exception.getMessage())
                .containsIgnoringCase("not")
                .containsIgnoringCase("request");
    }

    @Test
    void bannedPeer() throws IOException {
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                Address.localHost(1234).toString(),
                0, new ArrayList<>());
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        when(banList.isBanned(Address.localHost(1234))).thenReturn(true);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);

        assertThat(exception.getMessage())
                .containsIgnoringCase("Peer")
                .containsIgnoringCase("quarantine");
    }

    @Test
    void invalidPoW() throws IOException {
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                Address.localHost(1234).toString(),
                5, new ArrayList<>());
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);

        assertThat(exception.getMessage())
                .containsIgnoringCase("authorization")
                .containsIgnoringCase("failed");
    }

    @Test
    void correctPoW() throws IOException {
        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes, new ArrayList<>());
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                responderCapability.getAddress().getFullAddress(),
                0, new ArrayList<>());
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        NetworkEnvelope responseNetworkEnvelope = handshakeResponder.verifyAndBuildRespond().getSecond();

        ConnectionHandshake.Response response = (ConnectionHandshake.Response) responseNetworkEnvelope.getEnvelopePayloadMessage();
        assertThat(response.getCapability()).isEqualTo(responderCapability);
        assertThat(response.getNetworkLoad()).isEqualTo(new NetworkLoad());
    }

    private NetworkEnvelope createValidRequest() {
        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes, new ArrayList<>());
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                responderCapability.getAddress().getFullAddress(),
                0, new ArrayList<>());
        return new NetworkEnvelope(token, request);
    }
}
