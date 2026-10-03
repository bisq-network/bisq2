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

package bisq.network.p2p.message;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.keys.KeyGeneration;
import bisq.security.pow.ProofOfWork;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A NetworkEnvelope cannot carry a message which is valid only inside a ConfidentialMessage: a message of another
 * module, for example a chat or trade message, or an AckMessage. The receiver checks the sender of such a message only
 * when it decrypts the ConfidentialMessage (see docs/specifications/network/confidential-messages.md).
 */
class NetworkEnvelopePayloadTest {
    private static final AuthorizationToken TOKEN =
            new HashCashV2Token(new ProofOfWork(new byte[20], 0, null, 0, new byte[72], 0), 0);

    @BeforeAll
    static void registerResolver() {
        TestExternalNetworkMessage.registerResolver();
    }

    @Test
    void envelopeCannotCarryMessageOfAnotherModule() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkEnvelope(TOKEN, new TestExternalNetworkMessage("text")));
    }

    @Test
    void envelopeCannotCarryAckMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkEnvelope(TOKEN, new AckMessage("ackRequestingMessageId")));
    }

    // The node creates the envelope of a received message with fromProto, which fails for such a payload
    @Test
    void receivedEnvelopeWithSuchPayloadFailsAtParsing() {
        bisq.network.protobuf.NetworkEnvelope validEnvelope = new NetworkEnvelope(TOKEN, new Ping(1)).toProto(false);
        bisq.network.protobuf.NetworkEnvelope envelopeWithAckMessage = validEnvelope.toBuilder()
                .setNetworkMessage(new AckMessage("ackRequestingMessageId").toProto(false))
                .build();
        bisq.network.protobuf.NetworkEnvelope envelopeWithMessageOfAnotherModule = validEnvelope.toBuilder()
                .setNetworkMessage(new TestExternalNetworkMessage("text").toProto(false))
                .build();

        assertInstanceOf(IllegalArgumentException.class, assertThrows(UnresolvableProtobufException.class,
                () -> NetworkEnvelope.fromProto(envelopeWithAckMessage)).getCause());
        assertInstanceOf(IllegalArgumentException.class, assertThrows(UnresolvableProtobufException.class,
                () -> NetworkEnvelope.fromProto(envelopeWithMessageOfAnotherModule)).getCause());
    }

    // The message comes from another node, so the exception names only its type (rule 7 of validation-at-parsing.md)
    @Test
    void exceptionNamesTheTypeAndNotTheContent() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new NetworkEnvelope(TOKEN, new TestExternalNetworkMessage("private chat text")));

        assertTrue(exception.getMessage().contains(TestExternalNetworkMessage.class.getSimpleName()));
        assertFalse(exception.getMessage().contains("private chat text"));
    }

    @Test
    void envelopeCarriesConfidentialMessage() throws UnresolvableProtobufException {
        ConfidentialMessage confidentialMessage = createConfidentialMessage();

        assertDoesNotThrow(() -> new NetworkEnvelope(TOKEN, confidentialMessage));
    }

    // The envelope does not decrypt the message, so the content does not need to be valid cipher text
    private static ConfidentialMessage createConfidentialMessage() throws UnresolvableProtobufException {
        byte[] senderPublicKey = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        return ConfidentialMessage.fromProto(bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(bisq.security.protobuf.ConfidentialData.newBuilder()
                        .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                        .setIv(ByteString.copyFrom(new byte[16]))
                        .setCipherText(ByteString.copyFrom(new byte[32]))
                        .setSignature(ByteString.copyFrom(new byte[70])))
                .setReceiverKeyId("receiverKeyId")
                .build());
    }
}
