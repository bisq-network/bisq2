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

package bisq.network.p2p.node.authorization;

import bisq.common.encoding.Hex;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// See the section "Handshake requests of earlier versions" of docs/specifications/network/authorization-tokens.md.
@SuppressWarnings("deprecation")
class LegacyHandshakeTokenTest {
    // The envelope of a handshake request which the code of the release 2.1.13 (tag v2.1.13) created on 2026-10-02,
    // as ConnectionHandshake.start() of that release does. The requester has the address 127.0.0.1:2001 and the
    // features of the desktop.conf of 2.1.13. The responder has the address 127.0.0.1:2002. The token is the legacy
    // HASH_CASH token. Its proof of work contains the request serialized for the hash.
    private static final String REQUEST_ENVELOPE_OF_2_1_13 = "127e0801527a0a780a3e0a3c0a1f0a0e0a093132372e302e302e31" +
            "10a21f1205434c4541522a06322e312e31331a12099a9999999999b93f11000000000000f03f208a9fbbc69f68108e051a208819" +
            "2ad4e632a3f146e593a56be67b7e4d7aaa1c179d88f66c7c6aba122cd5f8217c14ae47e17a74402a0800000000000001471a450a" +
            "430a260a0e0a093132372e302e302e3110a21f1205434c4541521a0301030520022a06322e312e31331a12099a9999999999b93f" +
            "11000000000000f03f208a9fbbc69f68";
    private static final String RESPONDER_ADDRESS = "127.0.0.1:2002";
    // The difficulty which the releases 2.1.3 to 2.1.13 use for a handshake request: the target difficulty, the cost
    // factor of the request and the initial network load. As a double, the product is 327.68000000000006.
    private static final double DIFFICULTY_OF_EARLIER_VERSIONS = 65536 * 0.05 * 0.1;

    private final AuthorizationService authorizationService = new AuthorizationService(
            new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
            new HashCashProofOfWorkService(),
            new EquihashProofOfWorkService(),
            Set.of(Feature.AUTHORIZATION_HASH_CASH_V2));

    @Test
    void authorizesTheHandshakeRequestOfVersion2113() throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();
        bisq.network.protobuf.AuthorizationToken tokenProto = envelope.getAuthorizationToken();
        assertTrue(LegacyHandshakeToken.hasLegacyBody(tokenProto));

        LegacyHandshakeToken token = LegacyHandshakeToken.fromProto(tokenProto);
        ConnectionHandshake.Request request = request(envelope);

        assertEquals(DIFFICULTY_OF_EARLIER_VERSIONS, token.getProofOfWork().getDifficulty());
        assertEquals("2.1.13", request.getCapability().getApplicationVersion());
        assertTrue(authorizationService.isLegacyHandshakeRequestAuthorized(request, token, RESPONDER_ADDRESS));
    }

    @Test
    void noEnvelopeCanHoldTheLegacyToken() throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();

        assertThrows(UnresolvableProtobufException.class,
                () -> AuthorizationToken.fromProto(envelope.getAuthorizationToken()));
        assertThrows(UnresolvableProtobufException.class, () -> NetworkEnvelope.fromProto(envelope));
    }

    @Test
    void rejectsATokenWhichWasCreatedForAnotherResponder() throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();
        LegacyHandshakeToken token = LegacyHandshakeToken.fromProto(envelope.getAuthorizationToken());

        assertFalse(authorizationService.isLegacyHandshakeRequestAuthorized(request(envelope), token,
                "127.0.0.1:2003"));
    }

    @Test
    void rejectsATokenWhichWasCreatedForAnotherRequest() throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();
        LegacyHandshakeToken token = LegacyHandshakeToken.fromProto(envelope.getAuthorizationToken());
        ConnectionHandshake.Request request = request(envelope);
        ConnectionHandshake.Request otherRequest = new ConnectionHandshake.Request(request.getCapability(),
                request.getAddressOwnershipProof(),
                request.getNetworkLoad(),
                request.getSignatureDate() + 1);

        assertFalse(authorizationService.isLegacyHandshakeRequestAuthorized(otherRequest, token, RESPONDER_ADDRESS));
    }

    @Test
    void rejectsAProofOfWorkWithoutTheDeclaredWork() throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();
        bisq.network.protobuf.AuthorizationToken tokenProto = envelope.getAuthorizationToken();
        // The solution of the token of 2.1.13 is the counter 327. The minting tried the counters from 1 on, so the
        // hash of the counter 1 has too few leading zero bits.
        bisq.network.protobuf.AuthorizationToken changed = withProofOfWork(tokenProto,
                tokenProto.getHashCashToken().getProofOfWork().toBuilder()
                        .setSolution(ByteString.copyFrom(new byte[]{0, 0, 0, 0, 0, 0, 0, 1})));

        assertFalse(authorizationService.isLegacyHandshakeRequestAuthorized(request(envelope),
                LegacyHandshakeToken.fromProto(changed), RESPONDER_ADDRESS));
    }

    // The earlier versions tolerated a difficulty which is up to 50 000 below the expected one. The expected
    // difficulty of a handshake request is about 327.68, so they accepted a proof of work of any difficulty.
    @ParameterizedTest
    @ValueSource(doubles = {0, 128, 327.68, Double.NaN, -1})
    void rejectsADifficultyBelowTheOneOfEarlierVersions(double difficulty) throws Exception {
        bisq.network.protobuf.NetworkEnvelope envelope = requestEnvelopeOf2113();
        bisq.network.protobuf.AuthorizationToken tokenProto = envelope.getAuthorizationToken();
        // The proof of work of 2.1.13 has more work than each of these difficulties needs
        bisq.network.protobuf.AuthorizationToken changed = withProofOfWork(tokenProto,
                tokenProto.getHashCashToken().getProofOfWork().toBuilder().setDifficulty(difficulty));

        assertFalse(authorizationService.isLegacyHandshakeRequestAuthorized(request(envelope),
                LegacyHandshakeToken.fromProto(changed), RESPONDER_ADDRESS));
    }

    @ParameterizedTest
    @EnumSource(value = bisq.network.protobuf.AuthorizationTokenType.class,
            names = {"AUTHORIZATIONTOKENTYPE_HASH_CASH"}, mode = EnumSource.Mode.EXCLUDE)
    void rejectsALegacyBodyWithAnotherDeclaredType(bisq.network.protobuf.AuthorizationTokenType type) throws Exception {
        var builder = requestEnvelopeOf2113().getAuthorizationToken().toBuilder();
        if (type == bisq.network.protobuf.AuthorizationTokenType.UNRECOGNIZED) {
            builder.setAuthorizationTokenTypeValue(999);
        } else {
            builder.setAuthorizationTokenType(type);
        }

        assertThrows(UnresolvableProtobufException.class, () -> LegacyHandshakeToken.fromProto(builder.build()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, Integer.MAX_VALUE})
    void rejectsAMessageCounterOtherThanZero(int messageCounter) throws Exception {
        bisq.network.protobuf.AuthorizationToken tokenProto = requestEnvelopeOf2113().getAuthorizationToken();
        bisq.network.protobuf.AuthorizationToken changed = tokenProto.toBuilder()
                .setHashCashToken(tokenProto.getHashCashToken().toBuilder().setMessageCounter(messageCounter))
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> LegacyHandshakeToken.fromProto(changed));
    }

    @Test
    void rejectsATokenOfASupportedMethod() throws Exception {
        ConnectionHandshake.Request request = request(requestEnvelopeOf2113());
        AuthorizationToken token = authorizationService.createToken(request, NetworkLoad.INITIAL_NETWORK_LOAD,
                RESPONDER_ADDRESS, 0, Feature.DEFAULT_FEATURES);
        bisq.network.protobuf.AuthorizationToken tokenProto = token.toProto(false);

        assertFalse(LegacyHandshakeToken.hasLegacyBody(tokenProto));
        assertThrows(UnresolvableProtobufException.class, () -> LegacyHandshakeToken.fromProto(tokenProto));
    }

    private static bisq.network.protobuf.NetworkEnvelope requestEnvelopeOf2113() throws Exception {
        return bisq.network.protobuf.NetworkEnvelope.parseFrom(Hex.decode(REQUEST_ENVELOPE_OF_2_1_13));
    }

    private static ConnectionHandshake.Request request(bisq.network.protobuf.NetworkEnvelope envelope)
            throws Exception {
        EnvelopePayloadMessage message = EnvelopePayloadMessage.fromProto(envelope.getNetworkMessage());
        return assertInstanceOf(ConnectionHandshake.Request.class, message);
    }

    private static bisq.network.protobuf.AuthorizationToken withProofOfWork(
            bisq.network.protobuf.AuthorizationToken tokenProto,
            bisq.security.protobuf.ProofOfWork.Builder proofOfWork) {
        return tokenProto.toBuilder()
                .setHashCashToken(tokenProto.getHashCashToken().toBuilder().setProofOfWork(proofOfWork))
                .build();
    }
}
