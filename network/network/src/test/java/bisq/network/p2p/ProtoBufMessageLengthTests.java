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
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoBufMessageLengthTests {

    private final Path tmpDir = FileUtils.createTempDir();
    private final List<Transport.Type> supportedTransportTypes = new ArrayList<>(1);
    private final AuthorizationService authorizationService;

    public ProtoBufMessageLengthTests() throws IOException {
        supportedTransportTypes.add(Transport.Type.CLEAR);
        this.authorizationService = createAuthorizationService();
    }

    @Test
    void basicTest() {
        bisq.network.protobuf.NetworkEnvelope networkEnvelope = createValidRequest();
        byte[] envelopeInBytes = networkEnvelope.toByteArray();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ProtoBufMessageLengthWriter.writeToBuffer(envelopeInBytes.length, byteBuffer);
        byteBuffer.flip();

        ProtoBufMessageLengthParser messageLengthParser = new ProtoBufMessageLengthParser(byteBuffer);
        long parsedLength = ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH;
        while (byteBuffer.hasRemaining()) {
            parsedLength = messageLengthParser.parseMessageLength();
        }

        assertThat(parsedLength).isEqualTo(envelopeInBytes.length);
    }

    private AuthorizationService createAuthorizationService() {
        String baseDir = tmpDir.toAbsolutePath().toString();
        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService);
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }

    private bisq.network.protobuf.NetworkEnvelope createValidRequest() {
        Capability peerCapability = new Capability(Address.localHost(2345), supportedTransportTypes);
        ConnectionHandshake.Request request = new ConnectionHandshake.Request(peerCapability, Load.INITIAL_LOAD);
        AuthorizationToken token = authorizationService.createToken(request,
                Load.INITIAL_LOAD,
                Address.localHost(1234).getFullAddress(),
                0);
        return new NetworkEnvelope(NetworkEnvelope.VERSION, token, request).toProto();
    }
}
