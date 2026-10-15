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
import bisq.common.network.I2PAddress;
import bisq.common.network.TransportType;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.envelope.ParseMemoryBudget;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.network.p2p.message.NetworkEnvelope.MAX_HANDSHAKE_SERIALIZED_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// See rules 2 and 6 of docs/specifications/network/message-framing.md.
class ConnectionHandshakeSizeLimitTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void requestAndResponseUseTheHandshakeBudget(boolean outbound) throws Exception {
        ParseMemoryBudget budget = spy(ParseMemoryBudget.forHandshakes());
        Capability myCapability = capability(1234);
        Capability peersCapability = capability(5678);
        NetworkLoad networkLoad = new NetworkLoad();
        AuthorizationToken token = new HashCashV2Token(
                new ProofOfWork(new byte[20], 0, null, 0, new byte[72], 0), 0);
        EnvelopePayloadMessage payload = outbound
                ? new ConnectionHandshake.Response(peersCapability, networkLoad)
                : new ConnectionHandshake.Request(peersCapability, Optional.empty(), networkLoad,
                        System.currentTimeMillis());
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        new NetworkEnvelope(token, payload).writeDelimitedTo(received);
        ByteArrayInputStream input = new ByteArrayInputStream(received.toByteArray()) {
            @Override
            public synchronized int read(byte[] bytes, int offset, int length) {
                // A slow peer must not reserve parse memory while its bytes are still arriving.
                assertThat(budget.getAvailableBytes()).isEqualTo(budget.getBudget());
                return super.read(bytes, offset, Math.min(length, 1));
            }
        };
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(input);
        when(socket.getOutputStream()).thenReturn(OutputStream.nullOutputStream());
        KeyBundle keyBundle = mock(KeyBundle.class);
        when(keyBundle.getTorKeyPair()).thenReturn(mock(TorKeyPair.class));
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.createToken(any(), any(), anyString(), anyInt(), anyCollection())).thenReturn(token);
        when(authorizationService.isAuthorized(any(), any(), any(), any(), anyString())).thenAnswer(invocation -> {
            // Authorization and response handling must run after the parse releases its permits.
            assertThat(budget.getAvailableBytes()).isEqualTo(budget.getBudget());
            return true;
        });
        ConnectionHandshake handshake = new ConnectionHandshake(socket, mock(BanList.class), myCapability,
                authorizationService, keyBundle, budget);

        ConnectionHandshake.Result result = outbound
                ? handshake.start(networkLoad, peersCapability.getAddress())
                : handshake.onSocket(networkLoad);

        assertThat(result.getPeersCapability()).isEqualTo(peersCapability);
        assertThat(input.available()).isZero();
        verify(budget).parse(any(byte[].class), any());
        assertThat(budget.getAvailableBytes()).isEqualTo(budget.getBudget());
    }

    @Test
    void closesTheSocketIfTheFirstMessageExceedsTheHandshakeLimit() throws IOException {
        // Above the limit of the handshake, but far below the limit of an established connection.
        byte[] lengthPrefix = lengthPrefix(MAX_HANDSHAKE_SERIALIZED_SIZE + 1);
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(inputWhichFailsAfter(lengthPrefix));
        when(socket.getOutputStream()).thenReturn(OutputStream.nullOutputStream());
        ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket,
                mock(BanList.class),
                mock(Capability.class),
                mock(AuthorizationService.class),
                mock(KeyBundle.class),
                ParseMemoryBudget.forHandshakes());

        assertThatThrownBy(() -> connectionHandshake.onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .hasCauseInstanceOf(InvalidProtocolBufferException.class);
        verify(socket).close();
    }

    @Test
    void largestValidHandshakeRequestIsBelowTheHandshakeLimit() {
        // The longest address is an I2P destination of 700 characters. A node creates the token of each handshake
        // request with HASH_CASH_V2, whose proof of work contains the hash of the request.
        I2PAddress address = new I2PAddress("A".repeat(700), 65_535);
        Capability capability = new Capability(Capability.VERSION,
                address,
                new ArrayList<>(List.of(TransportType.values())),
                new ArrayList<>(List.of(Feature.values())),
                "999.999.999");
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(capability,
                Optional.of(new byte[64]),
                new NetworkLoad(1, 1),
                Long.MAX_VALUE);
        //noinspection deprecation
        AuthorizationService authorizationService = new AuthorizationService(
                new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH_V2));
        AuthorizationToken token = authorizationService.createToken(request,
                NetworkLoad.INITIAL_NETWORK_LOAD,
                address.getFullAddress(),
                0,
                Feature.DEFAULT_FEATURES);

        int serializedSize = new NetworkEnvelope(token, request).completeProto().getSerializedSize();

        assertThat(serializedSize).isLessThan(MAX_HANDSHAKE_SERIALIZED_SIZE);
    }

    private static byte[] lengthPrefix(int length) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(length);
        codedOutputStream.flush();
        return outputStream.toByteArray();
    }

    private static Capability capability(int port) {
        return new Capability(Capability.VERSION, Address.from("127.0.0.1", port),
                new ArrayList<>(List.of(TransportType.CLEAR)), new ArrayList<>(Feature.DEFAULT_FEATURES), "2.1.14");
    }

    // Returns the given bytes. A read of a further byte fails the test.
    private static InputStream inputWhichFailsAfter(byte[] bytes) {
        return new InputStream() {
            private int position;

            @Override
            public int read() {
                if (position == bytes.length) {
                    throw new AssertionError("The handshake read more than " + bytes.length + " bytes");
                }
                return Byte.toUnsignedInt(bytes[position++]);
            }
        };
    }
}
