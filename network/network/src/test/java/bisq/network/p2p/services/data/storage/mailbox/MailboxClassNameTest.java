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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MailboxData and RemoveMailboxRequest carry the class name of the MailboxMessage in the field className. Nodes of
 * earlier versions do not know the field. They drop it when they parse, relay or persist a message, and they
 * compute signature V1 preimages, storage keys and proof of work payloads without it. Therefore className must not
 * change these bytes, and a message without className must still be accepted. Signature V2 covers className; a
 * receiver takes it from metaData if the field is missing.
 */
class MailboxClassNameTest {
    private static final String CLASS_NAME = AckMessage.class.getSimpleName();
    private static final MetaData META_DATA = AckMessage.META_DATA;

    private static final List<String> INVALID_CLASS_NAMES = List.of(
            "Name\nforged log line",
            "A".repeat(51),
            "lowerCaseName");

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @Test
    void mailboxDataSendsClassNameAndMetaData() throws UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair));

        bisq.network.protobuf.MailboxData proto = mailboxData.toProto(false);

        assertEquals(CLASS_NAME, mailboxData.getClassName());
        assertEquals(CLASS_NAME, proto.getClassName());
        //noinspection deprecation
        assertEquals(META_DATA.toProto(false), proto.getMetaData());
        assertEquals("", mailboxData.toProto(true).getClassName());
    }

    @Test
    void mailboxDataWithoutClassNameUsesClassNameOfMetaData() throws UnresolvableProtobufException {
        bisq.network.protobuf.MailboxData proto = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair))
                .toProto(false).toBuilder()
                .clearClassName()
                .build();

        assertEquals(CLASS_NAME, MailboxData.fromProto(proto).getClassName());
    }

    @Test
    void mailboxDataUsesClassNameFieldIfPresent() throws UnresolvableProtobufException {
        bisq.network.protobuf.MailboxData proto = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair))
                .toProto(false).toBuilder()
                .setClassName("OtherMailboxMessage")
                .build();

        assertEquals("OtherMailboxMessage", MailboxData.fromProto(proto).getClassName());
    }

    @Test
    void addMailboxRequestWithoutClassNameHasSameSignatureStorageKeyAndProofOfWorkPayload()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair));
        AddMailboxRequest request = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        bisq.network.protobuf.AddMailboxRequest.Builder builder = request.toValueProto(false).toBuilder();
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().clearClassName();

        AddMailboxRequest requestWithoutClassName = AddMailboxRequest.fromProto(builder.build());

        assertFalse(requestWithoutClassName.isSignatureInvalid());
        assertEquals(request.getStorageKeyV1(), requestWithoutClassName.getStorageKeyV1());
        assertEquals(request.getStorageKey(), requestWithoutClassName.getStorageKey());
        assertArrayEquals(request.serializeForHash(), requestWithoutClassName.serializeForHash());
        assertEquals(CLASS_NAME, requestWithoutClassName.getMailboxSequentialData().getMailboxData().getClassName());
    }

    @Test
    void removeMailboxRequestSendsClassNameAndMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair));
        RemoveMailboxRequest request = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);

        bisq.network.protobuf.RemoveMailboxRequest proto = request.toValueProto(false);

        assertEquals(CLASS_NAME, request.getClassName());
        assertEquals(CLASS_NAME, proto.getClassName());
        //noinspection deprecation
        assertEquals(CLASS_NAME, proto.getMetaData().getClassName());
        assertEquals("", request.toValueProto(true).getClassName());
    }

    @Test
    void removeMailboxRequestWithoutClassNameHasSameProofOfWorkPayload()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair));
        RemoveMailboxRequest request = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);

        RemoveMailboxRequest requestWithoutClassName = RemoveMailboxRequest.fromProto(
                request.toValueProto(false).toBuilder().clearClassName().build());

        assertEquals(CLASS_NAME, requestWithoutClassName.getClassName());
        assertFalse(requestWithoutClassName.isSignatureInvalid());
        assertArrayEquals(request.serializeForHash(), requestWithoutClassName.serializeForHash());
    }

    // className is a field which the sender sets. It must have the format of the class name of MetaData, so a value
    // of any length or with line breaks fails at parsing (see docs/specifications/network/storage-metadata.md).
    @Test
    void mailboxDataWithInvalidClassNameIsRejectedAtParsing() throws UnresolvableProtobufException {
        bisq.network.protobuf.MailboxData proto = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair))
                .toProto(false);

        for (String className : INVALID_CLASS_NAMES) {
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(UnresolvableProtobufException.class,
                            () -> MailboxData.fromProto(proto.toBuilder().setClassName(className).build())).getCause());
        }
    }

    @Test
    void removeMailboxRequestWithInvalidClassNameIsRejectedAtParsing()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(META_DATA, createConfidentialMessage(senderKeyPair));
        bisq.network.protobuf.RemoveMailboxRequest proto = RemoveMailboxRequest.from(mailboxData, receiverKeyPair)
                .toValueProto(false);

        for (String className : INVALID_CLASS_NAMES) {
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(UnresolvableProtobufException.class,
                            () -> RemoveMailboxRequest.fromProto(proto.toBuilder().setClassName(className).build()))
                            .getCause());
        }
    }
}
