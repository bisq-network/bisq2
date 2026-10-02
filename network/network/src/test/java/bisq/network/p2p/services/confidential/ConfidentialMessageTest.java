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

package bisq.network.p2p.services.confidential;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.security.ConfidentialData;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A confidential message without confidential data or without the key ID of the receiver cannot be created. The mailbox
 * store reads the sender from the confidential data.
 */
class ConfidentialMessageTest {
    private static final String RECEIVER_KEY_ID = "receiverKeyId";

    @Test
    void validMessageIsCreated() throws UnresolvableProtobufException {
        ConfidentialMessage message = ConfidentialMessage.fromProto(createProto());

        assertEquals(RECEIVER_KEY_ID, message.getReceiverKeyId());
        assertEquals(message, new ConfidentialMessage(message.getConfidentialData(), RECEIVER_KEY_ID));
    }

    @Test
    void messageWithoutConfidentialDataCannotBeCreated() {
        assertThrows(NullPointerException.class, () -> new ConfidentialMessage(null, RECEIVER_KEY_ID));
    }

    @Test
    void messageWithoutReceiverKeyIdCannotBeCreated() throws UnresolvableProtobufException {
        ConfidentialData confidentialData = ConfidentialMessage.fromProto(createProto()).getConfidentialData();

        assertThrows(NullPointerException.class, () -> new ConfidentialMessage(confidentialData, null));
    }

    // Protobuf returns no null values. A received message without confidential data contains the default values of
    // ConfidentialData, which ConfidentialData rejects.
    @Test
    void receivedMessageWithoutConfidentialDataIsRejected() {
        bisq.network.protobuf.ConfidentialMessage proto = createProto().toBuilder().clearConfidentialData().build();

        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> ConfidentialMessage.fromProto(proto)).getCause());
    }

    // The storage layer does not decrypt the message, so the content does not need to be valid cipher text
    private static bisq.network.protobuf.ConfidentialMessage createProto() {
        byte[] senderPublicKey = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        return bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(bisq.security.protobuf.ConfidentialData.newBuilder()
                        .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                        .setIv(ByteString.copyFrom(new byte[16]))
                        .setCipherText(ByteString.copyFrom(new byte[32]))
                        .setSignature(ByteString.copyFrom(new byte[70])))
                .setReceiverKeyId(RECEIVER_KEY_ID)
                .build();
    }
}
