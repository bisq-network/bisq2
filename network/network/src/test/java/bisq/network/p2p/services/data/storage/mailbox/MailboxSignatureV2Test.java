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

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.EcPublicKeyVariants;
import bisq.network.p2p.services.data.storage.EcdsaSignatureVariants;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signature V2 of the mailbox requests covers every field of the request except itself and the deprecated metaData,
 * including the version and className of the nested MailboxData. Nodes of earlier versions know neither signatureV2
 * nor the version of the add request. Both are excluded from the hash, so the proof of work payload is the same for
 * nodes of all versions.
 * <p>
 * A node accepts requests without signature V2 only before SignedRequest.SIGNATURE_V1_CUTOFF_DATE. The tests of such
 * requests pass the time of the node, so they do not depend on the date at which they run.
 */
class MailboxSignatureV2Test {
    private static final String CLASS_NAME = AckMessage.class.getSimpleName();

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @Test
    void addMailboxRequestHasValidSignaturesAfterProtobufRoundTrip()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest request = createAddMailboxRequest();
        AddMailboxRequest received = AddMailboxRequest.fromProto(request.toValueProto(false));

        assertEquals(2, received.getVersion());
        assertTrue(received.isSignatureV2Present());
        assertFalse(received.isSignatureV2Invalid());
        assertFalse(received.isSignatureV1Invalid());
        assertFalse(received.isSignatureInvalid());
    }

    @Test
    void mailboxReceiverHashMustIdentifyTheReceiverUsedByTheStorageKey()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.MailboxSequentialData proto = createAddMailboxRequest()
                .getMailboxSequentialData().toProto(false);
        byte[] otherPublicKeyBytes = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();

        // Otherwise the receiver's removal finds the entry but fails the hash check, while a removal
        // signed by the key of the supplied hash looks up another storage key. Neither can remove it.
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> MailboxSequentialData.fromProto(proto.toBuilder()
                                .setReceiversPubKeyHash(ByteString.copyFrom(DigestUtil.hash(otherPublicKeyBytes)))
                                .build())).getCause());
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> MailboxSequentialData.fromProto(proto.toBuilder()
                                .setReceiversPubKeyBytes(ByteString.copyFrom(otherPublicKeyBytes))
                                .build())).getCause());
    }

    @Test
    void addMailboxRequestSenderHashMustBeTheHashOfTheSenderPublicKey()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest proto = createAddMailboxRequest().toValueProto(false);
        byte[] otherPublicKeyBytes = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();

        // Otherwise the signature names one key, and the sender check uses another one.
        UnresolvableProtobufException changedKey = assertThrows(UnresolvableProtobufException.class,
                () -> AddMailboxRequest.fromProto(proto.toBuilder()
                        .setSenderPublicKeyBytes(ByteString.copyFrom(otherPublicKeyBytes))
                        .build()));
        assertInstanceOf(IllegalArgumentException.class, changedKey.getCause());
        bisq.network.protobuf.AddMailboxRequest.Builder changedSenderHash = proto.toBuilder();
        changedSenderHash.getMailboxSequentialDataBuilder()
                .setSenderPublicKeyHash(ByteString.copyFrom(DigestUtil.hash(otherPublicKeyBytes)));
        UnresolvableProtobufException changedHash = assertThrows(UnresolvableProtobufException.class,
                () -> AddMailboxRequest.fromProto(changedSenderHash.build()));
        assertInstanceOf(IllegalArgumentException.class, changedHash.getCause());
    }

    @Test
    void addMailboxRequestWithChangedFieldIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest proto = createAddMailboxRequest().toValueProto(false);

        // Signature V1 does not cover the version and className of MailboxData. Signature V2 covers them.
        bisq.network.protobuf.AddMailboxRequest.Builder changedClassName = proto.toBuilder();
        // The className of the deprecated metaData changes as well, otherwise parsing rejects the request
        //noinspection deprecation
        changedClassName.getMailboxSequentialDataBuilder().getMailboxDataBuilder()
                .setClassName("OtherType")
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "OtherType").toProto(false));
        assertTrue(AddMailboxRequest.fromProto(changedClassName.build()).isSignatureInvalid());

        bisq.network.protobuf.AddMailboxRequest.Builder changedDataVersion = proto.toBuilder();
        // Another version which parses (MetaData.validateVersionForHashOfEarlierVersions)
        changedDataVersion.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setVersion(3);
        assertTrue(AddMailboxRequest.fromProto(changedDataVersion.build()).isSignatureInvalid());

        assertTrue(AddMailboxRequest.fromProto(proto.toBuilder().setVersion(3).build()).isSignatureInvalid());
    }

    // Version 2 is the first version with signature V2. Add requests of nodes up to 2.1.13 have version 0, remove
    // requests version 1, both without signature V2.
    @Test
    void mailboxRequestsWithSignatureV2AndVersion1AreRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest addProto = createAddMailboxRequest().toValueProto(false);
        assertThrows(UnresolvableProtobufException.class,
                () -> AddMailboxRequest.fromProto(addProto.toBuilder().setVersion(1).build()));

        bisq.network.protobuf.RemoveMailboxRequest removeProto = createRemoveMailboxRequest().toValueProto(false);
        assertThrows(UnresolvableProtobufException.class,
                () -> RemoveMailboxRequest.fromProto(removeProto.toBuilder().setVersion(1).build()));
    }

    @Test
    void addMailboxRequestWithoutSignatureV2IsVerifiedWithSignatureV1BeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest request = createAddMailboxRequest();

        AddMailboxRequest asFromEarlierVersion = AddMailboxRequest.fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().clearVersion().build());

        assertFalse(asFromEarlierVersion.isSignatureV2Present());
        assertFalse(asFromEarlierVersion.isRequiredSignatureV2Missing(BEFORE_CUTOFF));
        assertFalse(asFromEarlierVersion.isSignatureInvalid(BEFORE_CUTOFF));
        assertArrayEquals(request.serializeForHash(), asFromEarlierVersion.serializeForHash());
    }

    // Signature V1 is valid, but it does not cover all fields of the request. From the cutoff date on, a node does not
    // accept it alone.
    @Test
    void addMailboxRequestWithoutSignatureV2IsInvalidFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest asFromEarlierVersion = AddMailboxRequest.fromProto(
                createAddMailboxRequest().toValueProto(false).toBuilder().clearSignatureV2().clearVersion().build());

        assertFalse(asFromEarlierVersion.isSignatureV1Invalid());
        assertTrue(asFromEarlierVersion.isRequiredSignatureV2Missing(AT_CUTOFF));
        assertTrue(asFromEarlierVersion.isSignatureInvalid(AT_CUTOFF));
    }

    @Test
    void mailboxRequestsWithSignatureV2AreValidBeforeAndAfterTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest addRequest = createAddMailboxRequest();
        RemoveMailboxRequest removeRequest = createRemoveMailboxRequest();

        for (long now : new long[]{BEFORE_CUTOFF, AT_CUTOFF}) {
            assertFalse(addRequest.isRequiredSignatureV2Missing(now));
            assertFalse(addRequest.isSignatureInvalid(now));
            assertFalse(removeRequest.isRequiredSignatureV2Missing(now));
            assertFalse(removeRequest.isSignatureInvalid(now));
        }
    }

    @Test
    void removeMailboxRequestHasValidSignaturesAfterProtobufRoundTrip()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveMailboxRequest request = createRemoveMailboxRequest();
        RemoveMailboxRequest received = RemoveMailboxRequest.fromProto(request.toValueProto(false));

        assertTrue(received.isSignatureV2Present());
        assertFalse(received.isSignatureV2Invalid());
        assertFalse(received.isSignatureV1Invalid());
        assertFalse(received.isSignatureInvalid());
    }

    @Test
    void removeMailboxRequestWithChangedFieldIsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.RemoveMailboxRequest proto = createRemoveMailboxRequest().toValueProto(false);

        // Signature V1 covers only the hash. Signature V2 covers these fields as well.
        //noinspection deprecation
        assertTrue(RemoveMailboxRequest.fromProto(proto.toBuilder()
                        .setClassName("OtherType")
                        .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "OtherType").toProto(false))
                        .build())
                .isSignatureInvalid());
        assertTrue(RemoveMailboxRequest.fromProto(proto.toBuilder().setCreated(proto.getCreated() - 1).build())
                .isSignatureInvalid());
        assertTrue(RemoveMailboxRequest.fromProto(proto.toBuilder().setVersion(3).build())
                .isSignatureInvalid());
    }

    @Test
    void removeMailboxRequestWithoutSignatureV2IsVerifiedWithSignatureV1BeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveMailboxRequest request = createRemoveMailboxRequest();

        RemoveMailboxRequest withoutSignatureV2 = RemoveMailboxRequest.fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().build());

        assertFalse(withoutSignatureV2.isSignatureInvalid(BEFORE_CUTOFF));
        assertArrayEquals(request.serializeForHash(), withoutSignatureV2.serializeForHash());
    }

    // Signature V1 of a remove request does not cover the creation date. The time of the node decides, so an earlier
    // creation date, which a relay can set, does not make the request valid.
    @Test
    void removeMailboxRequestWithoutSignatureV2IsInvalidFromTheCutoffDateOnForAnyCreationDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveMailboxRequest request = createRemoveMailboxRequest();

        RemoveMailboxRequest withEarlierCreationDate = RemoveMailboxRequest.fromProto(
                request.toValueProto(false).toBuilder()
                        .clearSignatureV2()
                        .setCreated(request.getCreated() - 1000)
                        .build());

        assertFalse(withEarlierCreationDate.isSignatureV1Invalid());
        assertTrue(withEarlierCreationDate.isRequiredSignatureV2Missing(AT_CUTOFF));
        assertTrue(withEarlierCreationDate.isSignatureInvalid(AT_CUTOFF));
    }

    // Nodes of this version sign in low-S form. A relay can create the high-S form of a signature without the private
    // key. Signature V2 accepts only the low-S form, signature V1 accepts both (nodes of earlier versions create both).
    // Signature V1 of a remove request does not cover the public key. A relay could replace the key with another
    // encoding of the same key, and the request would get another storage key. Only the canonical encoding is valid.
    @Test
    void removeMailboxRequestWithOtherEncodingOfReceiverKeyIsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.RemoveMailboxRequest proto = createRemoveMailboxRequest().toValueProto(false).toBuilder()
                .clearSignatureV2()
                .setReceiverPublicKeyBytes(EcPublicKeyVariants.toCompressed(receiverKeyPair.getPublic()))
                .build();

        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> RemoveMailboxRequest.fromProto(proto)).getCause());
    }

    @Test
    void addMailboxRequestWithHighSSignatureV2IsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest proto = createAddMailboxRequest().toValueProto(false);

        assertTrue(AddMailboxRequest.fromProto(proto.toBuilder()
                        .setSignatureV2(EcdsaSignatureVariants.toOtherS(proto.getSignatureV2(), senderKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid());

        assertFalse(AddMailboxRequest.fromProto(proto.toBuilder()
                        .clearSignatureV2()
                        .clearVersion()
                        .setSignature(EcdsaSignatureVariants.toOtherS(proto.getSignature(), senderKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid(BEFORE_CUTOFF));
    }

    @Test
    void removeMailboxRequestWithHighSSignatureV2IsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.RemoveMailboxRequest proto = createRemoveMailboxRequest().toValueProto(false);

        assertTrue(RemoveMailboxRequest.fromProto(proto.toBuilder()
                        .setSignatureV2(EcdsaSignatureVariants.toOtherS(proto.getSignatureV2(), receiverKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid());

        assertFalse(RemoveMailboxRequest.fromProto(proto.toBuilder()
                        .clearSignatureV2()
                        .setSignature(EcdsaSignatureVariants.toOtherS(proto.getSignature(), receiverKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid(BEFORE_CUTOFF));
    }

    // The preimages of signature V2 are new messages. Their layout is written here independently with protobuf's
    // CodedOutputStream.
    @Test
    void addMailboxRequestSignatureV2PreimageHasFixedLayout()
            throws GeneralSecurityException, IOException, UnresolvableProtobufException {
        AddMailboxRequest request = createAddMailboxRequest();
        MailboxSequentialData sequentialData = request.getMailboxSequentialData();
        bisq.network.protobuf.AddMailboxRequest proto = request.toValueProto(false);

        // The sequential data has the field numbers of its protobuf message. The MailboxData in it has its own layout
        // (see mailboxDataSignatureEncodingHasFixedLayout).
        ByteArrayOutputStream sequentialDataLayout = new ByteArrayOutputStream();
        CodedOutputStream sequentialDataOutput = CodedOutputStream.newInstance(sequentialDataLayout);
        sequentialDataOutput.writeBytes(1,
                ByteString.copyFrom(getMailboxDataSignatureLayout(sequentialData.getMailboxData())));
        sequentialDataOutput.writeSInt32(2, sequentialData.getSequenceNumber());
        sequentialDataOutput.writeBytes(3, ByteString.copyFrom(sequentialData.getSenderPublicKeyHash()));
        sequentialDataOutput.writeSInt64(4, sequentialData.getCreated());
        sequentialDataOutput.writeBytes(5, ByteString.copyFrom(sequentialData.getReceiversPubKeyBytes()));
        sequentialDataOutput.writeBytes(6, ByteString.copyFrom(sequentialData.getReceiversPublicKeyHash()));
        sequentialDataOutput.flush();

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(expected);
        output.writeString(1, "bisq.AddMailboxRequest.signatureV2");
        output.writeSInt32(2, 2);
        output.writeBytes(3, ByteString.copyFrom(sequentialDataLayout.toByteArray()));
        output.writeBytes(4, proto.getSenderPublicKeyBytes());
        output.writeBytes(5, proto.getSignature());
        output.flush();

        assertArrayEquals(expected.toByteArray(), request.getPreImageV2());
    }

    @Test
    void removeMailboxRequestSignatureV2PreimageHasFixedLayout()
            throws GeneralSecurityException, IOException, UnresolvableProtobufException {
        RemoveMailboxRequest request = createRemoveMailboxRequest();
        bisq.network.protobuf.RemoveMailboxRequest proto = request.toValueProto(false);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(expected);
        output.writeString(1, "bisq.RemoveMailboxRequest.signatureV2");
        output.writeSInt32(2, 2);
        output.writeString(3, CLASS_NAME);
        output.writeSInt64(4, proto.getCreated());
        output.writeBytes(5, proto.getHash());
        output.writeBytes(6, proto.getReceiverPublicKeyBytes());
        output.writeBytes(7, proto.getSignature());
        output.flush();

        assertArrayEquals(expected.toByteArray(), request.getPreImageV2());
    }

    // The SIGNATURE encoding of MailboxData is part of the preimage of signature V2 of AddMailboxRequest. Like the
    // preimages, it has its own field numbers, not those of the protobuf message MailboxData. It has no metaData.
    @Test
    void mailboxDataSignatureEncodingHasFixedLayout() throws IOException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));

        assertArrayEquals(getMailboxDataSignatureLayout(mailboxData),
                mailboxData.encodeCanonical(EncodingPurpose.SIGNATURE));
    }

    private static byte[] getMailboxDataSignatureLayout(MailboxData mailboxData) throws IOException {
        ByteArrayOutputStream layout = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(layout);
        output.writeSInt32(1, mailboxData.getVersion());
        output.writeString(2, CLASS_NAME);
        output.writeBytes(3, mailboxData.getConfidentialMessage().toValueProto(false).toByteString());
        output.flush();
        return layout.toByteArray();
    }

    private AddMailboxRequest createAddMailboxRequest() throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        return AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
    }

    private RemoveMailboxRequest createRemoveMailboxRequest()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        return RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
    }
}
