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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2TokenService;
import bisq.network.p2p.node.envelope.ParseMemoryBudget;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.network.p2p.message.NetworkEnvelope.MAX_HANDSHAKE_SERIALIZED_SIZE;
import static bisq.network.p2p.node.ConnectionException.Reason.AUTHORIZATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// See the section "Handshake requests of earlier versions" of docs/specifications/network/authorization-tokens.md.
// LegacyHandshakeTokenTest checks the handshake request which the release 2.1.13 created. The date check of the
// handshake rejects those fixed bytes, so the tests of this class create the legacy token as the earlier versions do.
@SuppressWarnings("deprecation")
class ConnectionHandshakeLegacyTokenTest {
    // The difficulty which the releases 2.1.3 to 2.1.13 use for a handshake request: the target difficulty, the cost
    // factor of the request and the initial network load. As a double, the product is 327.68000000000006.
    private static final double DIFFICULTY_OF_EARLIER_VERSIONS = 65536 * 0.05 * 0.1;
    // The features of the desktop.conf of the releases 2.1.3 to 2.1.13
    private static final List<Feature> FEATURES_OF_EARLIER_VERSIONS = List.of(Feature.INVENTORY_HASH_SET,
            Feature.AUTHORIZATION_HASH_CASH,
            Feature.AUTHORIZATION_HASH_CASH_V2);
    private static final List<Feature> MY_FEATURES = List.of(Feature.INVENTORY_HASH_SET,
            Feature.INVENTORY_HASH_SET_V2,
            Feature.AUTHORIZATION_HASH_CASH_V2);

    private final Capability myCapability = capability(2002, MY_FEATURES, "2.1.14");
    private final Capability peersCapability = capability(2001, FEATURES_OF_EARLIER_VERSIONS, "2.1.13");
    private final String myAddress = myCapability.getAddress().getFullAddress();
    private final AuthorizationService authorizationService = new AuthorizationService(
            new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
            new HashCashProofOfWorkService(),
            new EquihashProofOfWorkService(),
            Set.copyOf(MY_FEATURES));
    private final ByteArrayOutputStream sent = new ByteArrayOutputStream();

    @Test
    void responderAcceptsARequestWithTheLegacyTokenAndAnswersWithV2() throws Exception {
        ConnectionHandshake.Request request = request(peersCapability);
        bisq.network.protobuf.NetworkEnvelope requestEnvelope = legacyEnvelope(request, myAddress,
                DIFFICULTY_OF_EARLIER_VERSIONS);
        Socket socket = socketWhichReceives(requestEnvelope);

        ConnectionHandshake.Result result = handshake(socket).onSocket(new NetworkLoad());

        assertThat(result.getPeersCapability()).isEqualTo(peersCapability);
        assertThat(result.getPeersCapability().getFeatures())
                .containsExactlyInAnyOrderElementsOf(FEATURES_OF_EARLIER_VERSIONS);
        assertThat(result.getConnectionMetrics().getNumMessagesReceived()).isEqualTo(1);
        assertThat(result.getConnectionMetrics().getReceivedBytes()).isEqualTo(requestEnvelope.getSerializedSize());
        // The earlier versions support HASH_CASH_V2 and advertise it in the request
        NetworkEnvelope responseEnvelope = sentEnvelope();
        assertThat(responseEnvelope.getAuthorizationToken()).isInstanceOf(HashCashV2Token.class);
        assertThat(responseEnvelope.getEnvelopePayloadMessage()).isInstanceOf(ConnectionHandshake.Response.class);
        assertThat(authorizationService.isAuthorized(responseEnvelope.getEnvelopePayloadMessage(),
                responseEnvelope.getAuthorizationToken(),
                request.getNetworkLoad(),
                "connectionId",
                peersCapability.getAddress().getFullAddress())).isTrue();
    }

    @Test
    void responderRejectsALegacyTokenWithTooLowDifficulty() throws Exception {
        ConnectionHandshake.Request request = request(peersCapability);
        Socket socket = socketWhichReceives(legacyEnvelope(request, myAddress, HashCashV2TokenService.MIN_DIFFICULTY));

        assertThatThrownBy(() -> handshake(socket).onSocket(new NetworkLoad()))
                .isInstanceOfSatisfying(ConnectionException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(AUTHORIZATION_FAILED));
        assertThat(sent.size()).isZero();
        verify(socket).close();
    }

    @Test
    void responderRejectsALegacyTokenOfAnotherResponder() throws Exception {
        ConnectionHandshake.Request request = request(peersCapability);
        Socket socket = socketWhichReceives(legacyEnvelope(request, "127.0.0.1:2003", DIFFICULTY_OF_EARLIER_VERSIONS));

        assertThatThrownBy(() -> handshake(socket).onSocket(new NetworkLoad()))
                .isInstanceOfSatisfying(ConnectionException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(AUTHORIZATION_FAILED));
        assertThat(sent.size()).isZero();
        verify(socket).close();
    }

    @Test
    void responderRejectsALegacyTokenForAMessageWhichIsNoRequest() throws Exception {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(peersCapability, new NetworkLoad());
        Socket socket = socketWhichReceives(legacyEnvelope(response, myAddress, DIFFICULTY_OF_EARLIER_VERSIONS));

        assertThatThrownBy(() -> handshake(socket).onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("not type of Request");
        assertThat(sent.size()).isZero();
        verify(socket).close();
    }

    // The releases before 2.1.3 support only the legacy method. A node of this version cannot create the token of
    // the response for them.
    @Test
    void responderCannotAnswerAPeerWhichSupportsOnlyTheLegacyMethod() throws Exception {
        Capability capabilityOf212 = capability(2001,
                List.of(Feature.INVENTORY_HASH_SET, Feature.AUTHORIZATION_HASH_CASH), "2.1.2");
        ConnectionHandshake.Request request = request(capabilityOf212);
        Socket socket = socketWhichReceives(legacyEnvelope(request, myAddress, DIFFICULTY_OF_EARLIER_VERSIONS));

        assertThatThrownBy(() -> handshake(socket).onSocket(new NetworkLoad()))
                .isInstanceOf(ConnectionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThat(sent.size()).isZero();
        verify(socket).close();
    }

    @Test
    void initiatorRejectsAResponseWithALegacyToken() throws Exception {
        ConnectionHandshake.Response response = new ConnectionHandshake.Response(peersCapability, new NetworkLoad());
        Socket socket = socketWhichReceives(legacyEnvelope(response, myAddress, DIFFICULTY_OF_EARLIER_VERSIONS));

        assertThatThrownBy(() -> handshake(socket).start(new NetworkLoad(), peersCapability.getAddress()))
                .isInstanceOf(ConnectionException.class)
                .hasCauseInstanceOf(UnresolvableProtobufException.class);
        verify(socket).close();
    }

    // The proof of work of the legacy token contains the request, so the envelope has about twice the size of the
    // request. See rule 6 of docs/specifications/network/message-framing.md.
    @Test
    void largestRequestWithTheLegacyTokenIsBelowTheHandshakeLimit() {
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

        int serializedSize = legacyEnvelope(request, address.getFullAddress(), DIFFICULTY_OF_EARLIER_VERSIONS)
                .getSerializedSize();

        assertThat(serializedSize).isLessThan(1800);
        assertThat(serializedSize).isLessThan(MAX_HANDSHAKE_SERIALIZED_SIZE);
    }

    private ConnectionHandshake handshake(Socket socket) throws IOException {
        KeyBundle keyBundle = mock(KeyBundle.class);
        when(keyBundle.getTorKeyPair()).thenReturn(mock(TorKeyPair.class));
        return new ConnectionHandshake(socket, mock(BanList.class), myCapability, authorizationService, keyBundle,
                ParseMemoryBudget.forHandshakes());
    }

    private Socket socketWhichReceives(bisq.network.protobuf.NetworkEnvelope envelope) throws IOException {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        envelope.writeDelimitedTo(received);
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(received.toByteArray()));
        when(socket.getOutputStream()).thenReturn(sent);
        return socket;
    }

    private NetworkEnvelope sentEnvelope() throws Exception {
        return NetworkEnvelope.fromProto(bisq.network.protobuf.NetworkEnvelope.parseDelimitedFrom(
                new ByteArrayInputStream(sent.toByteArray())));
    }

    private static ConnectionHandshake.Request request(Capability capability) {
        return new ConnectionHandshake.Request(capability, Optional.empty(), new NetworkLoad(),
                System.currentTimeMillis());
    }

    // The envelope as the releases up to 2.1.13 create it for their handshake request: the payload of the proof of
    // work is the message serialized for the hash, the challenge is derived from the address of the responder and
    // the message counter 0.
    private static bisq.network.protobuf.NetworkEnvelope legacyEnvelope(EnvelopePayloadMessage message,
                                                                        String responderAddress,
                                                                        double difficulty) {
        ProofOfWork proofOfWork = new HashCashProofOfWorkService().mint(message.serializeForHash(),
                HashCashV2TokenService.getChallenge(responderAddress, 0),
                difficulty);
        return bisq.network.protobuf.NetworkEnvelope.newBuilder()
                .setVersion(NetworkEnvelope.networkVersion)
                .setAuthorizationToken(bisq.network.protobuf.AuthorizationToken.newBuilder()
                        .setAuthorizationTokenType(
                                bisq.network.protobuf.AuthorizationTokenType.AUTHORIZATIONTOKENTYPE_HASH_CASH)
                        .setHashCashToken(bisq.network.protobuf.HashCashToken.newBuilder()
                                .setProofOfWork(proofOfWork.toProto(false))
                                .setMessageCounter(0)))
                .setNetworkMessage(message.toProto(false))
                .build();
    }

    private static Capability capability(int port, List<Feature> features, String applicationVersion) {
        return new Capability(Capability.VERSION, Address.from("127.0.0.1", port),
                new ArrayList<>(List.of(TransportType.CLEAR)), new ArrayList<>(features), applicationVersion);
    }
}
