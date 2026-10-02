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

import bisq.common.network.I2PAddress;
import bisq.common.network.TransportType;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// See rules 2 and 6 of docs/specifications/network/message-framing.md.
class ConnectionHandshakeSizeLimitTest {
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
                mock(KeyBundle.class));

        assertThatThrownBy(() -> connectionHandshake.onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .hasCauseInstanceOf(InvalidProtocolBufferException.class);
        verify(socket).close();
    }

    @Test
    void largestValidHandshakeRequestIsBelowTheHandshakeLimit() {
        // The longest address is an I2P destination of 700 characters. A node creates the token of each handshake
        // request with HASH_CASH, whose proof of work contains a copy of the request. A response is smaller.
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
                new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH));
        AuthorizationToken token = authorizationService.createToken(request,
                NetworkLoad.INITIAL_NETWORK_LOAD,
                address.getFullAddress(),
                0,
                Feature.DEFAULT_FEATURES);

        int serializedSize = new NetworkEnvelope(token, request).completeProto().getSerializedSize();

        // 1771 bytes when the test was written.
        assertThat(serializedSize).isLessThan(MAX_HANDSHAKE_SERIALIZED_SIZE);
    }

    private static byte[] lengthPrefix(int length) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeUInt32NoTag(length);
        codedOutputStream.flush();
        return outputStream.toByteArray();
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
