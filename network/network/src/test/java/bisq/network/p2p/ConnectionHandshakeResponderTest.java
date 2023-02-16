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
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionHandshakeResponderTest {

    private final Path tmpDir = FileUtils.createTempDir();
    private final BanList banList = mock(BanList.class);
    private final List<Transport.Type> supportedTransportTypes = new ArrayList<>(1);
    private final Capability responderCapability;
    private final AuthorizationService authorizationService;
    private final NetworkEnvelopeSocketChannel networkEnvelopeSocketChannel = mock(NetworkEnvelopeSocketChannel.class);
    private final ConnectionHandshakeResponder handshakeResponder;

    public ConnectionHandshakeResponderTest() throws IOException {
        supportedTransportTypes.add(Transport.Type.CLEAR);
        this.responderCapability = new Capability(Address.localHost(1234), supportedTransportTypes);
        this.authorizationService = createAuthorizationService();
        this.handshakeResponder = new ConnectionHandshakeResponder(
                banList,
                responderCapability,
                authorizationService,
                networkEnvelopeSocketChannel);
    }

    private AuthorizationService createAuthorizationService() {
        String baseDir = tmpDir.toAbsolutePath().toString();
        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService);
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
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
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                Address.localHost(1234).toString(),
                0);
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION + 1000, token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);
        assertThat(exception.getMessage()).containsIgnoringCase("Invalid version");
    }

    @Test
    void wrongNetworkMessage() throws IOException {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(responderCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(
                response,
                Load.INITIAL_LOAD,
                Address.localHost(1234).toString(),
                0);
        NetworkEnvelope responseEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, response);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(responseEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);

        assertThat(exception.getMessage())
                .containsIgnoringCase("not")
                .containsIgnoringCase("request");
    }

    @Test
    void bannedPeer() throws IOException {
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                Address.localHost(1234).toString(),
                0);
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
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
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(responderCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                Address.localHost(1234).toString(),
                5);
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        Exception exception = assertThrows(ConnectionException.class, handshakeResponder::verifyAndBuildRespond);

        assertThat(exception.getMessage())
                .containsIgnoringCase("authorization")
                .containsIgnoringCase("failed");
    }

    @Test
    void correctPoW() throws IOException {
        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                responderCapability.getAddress().getFullAddress(),
                0);
        NetworkEnvelope requestNetworkEnvelope = new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
        List<NetworkEnvelope> allEnvelopesToReceive = List.of(requestNetworkEnvelope);
        when(networkEnvelopeSocketChannel.receiveNetworkEnvelopes()).thenReturn(allEnvelopesToReceive);

        NetworkEnvelope responseNetworkEnvelope = handshakeResponder.verifyAndBuildRespond().getSecond();

        ConnectionHandshake.Response response = (ConnectionHandshake.Response) responseNetworkEnvelope.getNetworkMessage();
        assertThat(response.getCapability()).isEqualTo(responderCapability);
        assertThat(response.getLoad()).isEqualTo(Load.INITIAL_LOAD);
    }

    private NetworkEnvelope createValidRequest() {
        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                responderCapability.getAddress().getFullAddress(),
                0);
        return new NetworkEnvelope(NetworkEnvelope.VERSION, token, request);
    }
}
