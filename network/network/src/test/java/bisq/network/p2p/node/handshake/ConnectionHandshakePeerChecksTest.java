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

package bisq.network.p2p.node.handshake;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.envelope.ParseMemoryBudget;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Both sides of the handshake end the connection before the authorization when the peer is banned or when the first
 * message of the peer is not the expected handshake message.
 */
class ConnectionHandshakePeerChecksTest {
    private static final AuthorizationToken TOKEN = new HashCashV2Token(
            new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0), 0);

    @Test
    void theResponderRejectsTheRequestOfABannedPeer() throws IOException {
        Capability peersCapability = capability(5678);
        BanList banList = mock(BanList.class);
        when(banList.isBanned(peersCapability.getAddress())).thenReturn(true);
        Socket socket = socketWhichReceives(request(peersCapability));
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        ConnectionHandshake handshake = handshake(socket, banList, authorizationService);

        assertThatThrownBy(() -> handshake.onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .satisfies(e -> assertThat(((ConnectionException) e).getReason())
                        .isEqualTo(ConnectionException.Reason.ADDRESS_BANNED));
        verify(socket).close();
        verifyNoInteractions(authorizationService);
    }

    @Test
    void theResponderRejectsAFirstMessageWhichIsNotARequest() throws IOException {
        Socket socket = socketWhichReceives(new ConnectionHandshake.Response(capability(5678), new NetworkLoad()));
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        ConnectionHandshake handshake = handshake(socket, mock(BanList.class), authorizationService);

        assertThatThrownBy(() -> handshake.onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("not type of Request");
        verify(socket).close();
        verifyNoInteractions(authorizationService);
    }

    @Test
    void theInitiatorRejectsTheResponseOfABannedPeer() throws IOException {
        Capability peersCapability = capability(5678);
        BanList banList = mock(BanList.class);
        when(banList.isBanned(peersCapability.getAddress())).thenReturn(true);
        Socket socket = socketWhichReceives(new ConnectionHandshake.Response(peersCapability, new NetworkLoad()));
        AuthorizationService authorizationService = authorizationServiceWhichCreatesTokens();
        ConnectionHandshake handshake = handshake(socket, banList, authorizationService);

        assertThatThrownBy(() -> handshake.start(new NetworkLoad(), peersCapability.getAddress()))
                .isInstanceOf(ConnectionException.class)
                .satisfies(e -> assertThat(((ConnectionException) e).getReason())
                        .isEqualTo(ConnectionException.Reason.ADDRESS_BANNED));
        verify(socket).close();
        verify(authorizationService, never()).isAuthorized(any(), any(), any(), any(), anyString());
    }

    @Test
    void theInitiatorRejectsAReplyWhichIsNotAResponse() throws IOException {
        Capability peersCapability = capability(5678);
        Socket socket = socketWhichReceives(new Ping(1));
        AuthorizationService authorizationService = authorizationServiceWhichCreatesTokens();
        ConnectionHandshake handshake = handshake(socket, mock(BanList.class), authorizationService);

        assertThatThrownBy(() -> handshake.start(new NetworkLoad(), peersCapability.getAddress()))
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("not type of Response");
        verify(socket).close();
        verify(authorizationService, never()).isAuthorized(any(), any(), any(), any(), anyString());
    }

    // The message of the type error contains the reply. A token of difficulty 0 made its string fail, so the initiator
    // reported an unspecified error instead of the type error.
    @Test
    void theInitiatorRejectsAReplyWhichIsNotAResponseAlsoWithATokenOfDifficultyZero() throws IOException {
        Capability peersCapability = capability(5678);
        AuthorizationToken tokenOfDifficultyZero = new HashCashV2Token(
                new ProofOfWork(new byte[20], 0, null, 0, new byte[72], 0), 0);
        Socket socket = socketWhichReceives(tokenOfDifficultyZero, new Ping(1));
        AuthorizationService authorizationService = authorizationServiceWhichCreatesTokens();
        ConnectionHandshake handshake = handshake(socket, mock(BanList.class), authorizationService);

        assertThatThrownBy(() -> handshake.start(new NetworkLoad(), peersCapability.getAddress()))
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("not type of Response");
        verify(socket).close();
    }

    private static ConnectionHandshake handshake(Socket socket,
                                                 BanList banList,
                                                 AuthorizationService authorizationService) throws IOException {
        KeyBundle keyBundle = mock(KeyBundle.class);
        when(keyBundle.getTorKeyPair()).thenReturn(mock(TorKeyPair.class));
        return new ConnectionHandshake(socket, banList, capability(1234), authorizationService, keyBundle,
                ParseMemoryBudget.forHandshakes());
    }

    private static AuthorizationService authorizationServiceWhichCreatesTokens() {
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.createToken(any(), any(), anyString(), anyInt(), anyCollection())).thenReturn(TOKEN);
        return authorizationService;
    }

    private static ConnectionHandshake.Request request(Capability capability) {
        return new ConnectionHandshake.Request(capability, Optional.empty(), new NetworkLoad(),
                System.currentTimeMillis());
    }

    // A socket whose input holds one envelope with the given message and whose output discards everything
    private static Socket socketWhichReceives(EnvelopePayloadMessage payload) throws IOException {
        return socketWhichReceives(TOKEN, payload);
    }

    private static Socket socketWhichReceives(AuthorizationToken token, EnvelopePayloadMessage payload)
            throws IOException {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        new NetworkEnvelope(token, payload).writeDelimitedTo(received);
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(received.toByteArray()));
        when(socket.getOutputStream()).thenReturn(OutputStream.nullOutputStream());
        return socket;
    }

    private static Capability capability(int port) {
        return new Capability(Capability.VERSION, Address.from("127.0.0.1", port),
                new ArrayList<>(List.of(TransportType.CLEAR)), new ArrayList<>(Feature.DEFAULT_FEATURES), "2.1.14");
    }
}
