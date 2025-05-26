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
import bisq.common.network.Address;
import bisq.common.network.DefaultClearNetLocalAddressFacade;
import bisq.common.network.TransportType;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.envelope.parser.DefaultProtoBufInputStream;
import bisq.network.p2p.node.envelope.parser.ProtoBufMessageLengthParser;
import bisq.network.p2p.node.envelope.parser.nio.NioProtoBufInputStream;
import bisq.network.p2p.node.envelope.parser.nio.ProtoBufMessageLengthWriter;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoBufMessageLengthTests {

    private final Path tmpDir = FileUtils.createTempDir();
    private final List<TransportType> supportedTransportTypes = new ArrayList<>(1);
    private final AuthorizationService authorizationService;

    public ProtoBufMessageLengthTests() throws IOException {
        supportedTransportTypes.add(TransportType.CLEAR);
        this.authorizationService = createAuthorizationService();
    }

    @Test
    void basicTest() {
        bisq.network.protobuf.NetworkEnvelope networkEnvelope = createValidRequest();
        byte[] envelopeInBytes = networkEnvelope.toByteArray();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ProtoBufMessageLengthWriter.writeToBuffer(envelopeInBytes.length, byteBuffer);
        byteBuffer.flip();

        byte[] serializedMessage = new byte[byteBuffer.remaining()];
        byteBuffer.get(serializedMessage);
        var byteArrayInputStream = new ByteArrayInputStream(serializedMessage);

        var protoBufStream = new DefaultProtoBufInputStream(byteArrayInputStream);
        var messageLengthParser = new ProtoBufMessageLengthParser(protoBufStream);

        long parsedLength = ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH;
        while (parsedLength == ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH) {
            parsedLength = messageLengthParser.parseMessageLength();
        }

        assertThat(parsedLength).isEqualTo(envelopeInBytes.length);
    }

    @Test
    void basicTestNio() {
        bisq.network.protobuf.NetworkEnvelope networkEnvelope = createValidRequest();
        byte[] envelopeInBytes = networkEnvelope.toByteArray();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ProtoBufMessageLengthWriter.writeToBuffer(envelopeInBytes.length, byteBuffer);
        byteBuffer.flip();

        var protoBufInputStream = new NioProtoBufInputStream(byteBuffer);
        var messageLengthParser = new ProtoBufMessageLengthParser(protoBufInputStream);

        long parsedLength = ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH;
        while (parsedLength == ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH) {
            parsedLength = messageLengthParser.parseMessageLength();
        }

        assertThat(parsedLength).isEqualTo(envelopeInBytes.length);
    }

    private AuthorizationService createAuthorizationService() {
        return new AuthorizationService(new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH));
    }

    private bisq.network.protobuf.NetworkEnvelope createValidRequest() {
        Capability peerCapability = createCapability(DefaultClearNetLocalAddressFacade.toLocalHostAddress(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Optional.empty(), new NetworkLoad(), 0);
        AuthorizationToken token = authorizationService.createToken(request,
                new NetworkLoad(),
                DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234).getFullAddress(),
                0, new ArrayList<>());
        return new NetworkEnvelope(token, request).completeProto();
    }

    private static Capability createCapability(Address address, List<TransportType> supportedTransportTypes) {
        return new Capability(Capability.VERSION, address, supportedTransportTypes, new ArrayList<>(), ApplicationVersion.getVersion().getVersionAsString());
    }
}
