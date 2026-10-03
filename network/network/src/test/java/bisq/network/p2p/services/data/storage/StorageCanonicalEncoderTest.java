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

package bisq.network.p2p.services.data.storage;

import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxSequentialData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The default canonical encoding of the storage classes contains all non-transient fields, with the envelopes that
 * serialize() writes. The STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT purposes leave out the fields annotated with
 * @ExcludeForHash, as serializeForHash() does. The SIGNATURE purpose does the same, except for the classes with
 * signature V2 (SignatureV2Test covers their preimages). Every class passes the encoding purpose on to its nested
 * objects. Data of higher-level modules, which the storage classes hold in a google.protobuf.Any, is encoded with the
 * retained bytes of the Any in the DEFAULT and SIGNATURE purposes, and with its protobuf serialization for hashes in
 * the STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT purposes. The external data stub therefore has a field that is
 * excluded from the hash: with it, the SIGNATURE encoding differs from serializeForHash(), and without it, it equals
 * serializeForHash() of classes without own excluded fields. DistributedDataAnyTest covers data with fields which this
 * node does not know.
 */
public class StorageCanonicalEncoderTest {
    private static final KeyPair KEY_PAIR = KeyGeneration.generateDefaultEcKeyPair();
    private static final byte[] PUBLIC_KEY_BYTES = KEY_PAIR.getPublic().getEncoded();
    private static final byte[] PUBLIC_KEY_HASH = DigestUtil.hash(PUBLIC_KEY_BYTES);
    private static final byte[] HASH = filledBytes(20, 0x11);
    private static final byte[] SIGNATURE = filledBytes(70, 0x22);
    private static final byte[] SIGNATURE_V2 = filledBytes(70, 0x66);
    private static final long CREATED = System.currentTimeMillis() - 60_000;
    private static final List<EncodingPurpose> HASH_PURPOSES = List.of(EncodingPurpose.SIGNATURE,
            EncodingPurpose.STORAGE_KEY,
            EncodingPurpose.PROOF_OF_WORK,
            EncodingPurpose.TRADE_CONTRACT);
    // The purposes which reproduce serializeForHash() also for the classes with signature V2 and for data of
    // higher-level modules
    private static final List<EncodingPurpose> HASH_SERIALIZATION_PURPOSES = List.of(EncodingPurpose.STORAGE_KEY,
            EncodingPurpose.PROOF_OF_WORK,
            EncodingPurpose.TRADE_CONTRACT);
    // The purposes of preimages in which the requests and the sequential data are nested. They have no STORAGE_KEY
    // encoding.
    private static final List<EncodingPurpose> NESTED_HASH_SERIALIZATION_PURPOSES = List.of(EncodingPurpose.PROOF_OF_WORK,
            EncodingPurpose.TRADE_CONTRACT);

    @Test
    public void metaDataMatchesProtobuf() {
        MetaData metaData = getMetaData();

        assertArrayEquals(metaData.serialize(), metaData.encodeCanonical());
        assertArrayEquals(metaData.serializeForHash(), metaData.encodeCanonical());
    }

    @Test
    public void authenticatedDataEncodesExternalDataWithHashSerializationOnlyForStorageKey() {
        DefaultAuthenticatedData withExcludedField = new DefaultAuthenticatedData(new ExternalData("content", 7));
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical());
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical(EncodingPurpose.SIGNATURE));
        assertArrayEquals(withExcludedField.serializeForHash(),
                withExcludedField.encodeCanonical(EncodingPurpose.STORAGE_KEY));
        assertFalse(Arrays.equals(withExcludedField.serialize(), withExcludedField.serializeForHash()));

        DefaultAuthenticatedData withoutExcludedField = getDefaultAuthenticatedData();
        assertArrayEquals(withoutExcludedField.serialize(), withoutExcludedField.encodeCanonical());
    }

    @Test
    public void authorizedDataContainsSignatureUnlikeHashSerialization() {
        AuthorizedData withSignature = getAuthorizedData();
        assertArrayEquals(withSignature.serialize(), withSignature.encodeCanonical());
        // serializeForHash() omits the signature, so that republishing keeps the storage key
        assertFalse(Arrays.equals(withSignature.serializeForHash(), withSignature.encodeCanonical()));

        AuthorizedData withoutSignature = new AuthorizedData(new ExternalData("content", 0), KEY_PAIR.getPublic());
        assertArrayEquals(withoutSignature.serialize(), withoutSignature.encodeCanonical());
        assertArrayEquals(withoutSignature.serializeForHash(), withoutSignature.encodeCanonical());

        // SIGNATURE, PROOF_OF_WORK and TRADE_CONTRACT contain the signature: AuthorizedData is nested in their
        // preimages, where the hash serialization of the enclosing message contains it. Without a field excluded from
        // the hash in the external data, they equal the default encoding.
        assertArrayEquals(withSignature.encodeCanonical(), withSignature.encodeCanonical(EncodingPurpose.SIGNATURE));
        assertArrayEquals(withSignature.encodeCanonical(), withSignature.encodeCanonical(EncodingPurpose.PROOF_OF_WORK));
        assertArrayEquals(withSignature.encodeCanonical(), withSignature.encodeCanonical(EncodingPurpose.TRADE_CONTRACT));
        // STORAGE_KEY is the preimage of the data hash of the storage key, with AuthorizedData as root. Like
        // serializeForHash(), it omits the signature, so that republishing keeps the storage key.
        assertArrayEquals(withSignature.serializeForHash(), withSignature.encodeCanonical(EncodingPurpose.STORAGE_KEY));

        // The STORAGE_KEY purpose contains the external data in its hash serialization, the SIGNATURE purpose with its
        // exact bytes
        AuthorizedData withExcludedField = new AuthorizedData(new ExternalData("content", 7),
                Optional.of(SIGNATURE),
                KEY_PAIR.getPublic());
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical(EncodingPurpose.SIGNATURE));
        assertFalse(Arrays.equals(withExcludedField.encodeCanonical(),
                withExcludedField.encodeCanonical(EncodingPurpose.STORAGE_KEY)));
        // PROOF_OF_WORK and TRADE_CONTRACT equal the nested hash serialization toProto(true), not serializeForHash()
        for (EncodingPurpose encodingPurpose : NESTED_HASH_SERIALIZATION_PURPOSES) {
            assertArrayEquals(withExcludedField.toProto(true).toByteArray(),
                    withExcludedField.encodeCanonical(encodingPurpose),
                    encodingPurpose.name());
            assertFalse(Arrays.equals(withExcludedField.serializeForHash(),
                    withExcludedField.encodeCanonical(encodingPurpose)),
                    encodingPurpose.name());
        }
    }

    @Test
    public void authenticatedSequentialDataAndAddRequestMatchProtobufForEveryAuthenticatedDataType() {
        for (AuthenticatedData authenticatedData : List.of(getDefaultAuthenticatedData(), getAuthorizedData())) {
            AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authenticatedData,
                    3,
                    PUBLIC_KEY_HASH,
                    CREATED);
            assertArrayEquals(sequentialData.serialize(), sequentialData.encodeCanonical());

            AddAuthenticatedDataRequest request = getAddAuthenticatedDataRequest(sequentialData);
            assertArrayEquals(request.serialize(), request.encodeCanonical());
            // serializeForHash() omits signatureV2 and version
            assertFalse(Arrays.equals(request.serializeForHash(), request.encodeCanonical()));
        }
    }

    @Test
    public void removeAndRefreshAuthenticatedDataRequestsContainFieldsExcludedFromHash()
            throws UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest removeRequest = getRemoveAuthenticatedDataRequest();
        assertArrayEquals(removeRequest.serialize(), removeRequest.encodeCanonical());
        // serializeForHash() omits metaData, version, className and signatureV2
        assertFalse(Arrays.equals(removeRequest.serializeForHash(), removeRequest.encodeCanonical()));

        RefreshAuthenticatedDataRequest refreshRequest = getRefreshAuthenticatedDataRequest();
        assertArrayEquals(refreshRequest.serialize(), refreshRequest.encodeCanonical());
        assertFalse(Arrays.equals(refreshRequest.serializeForHash(), refreshRequest.encodeCanonical()));
    }

    @Test
    public void confidentialMessageMatchesProtobufWithAndWithoutEnvelope() throws UnresolvableProtobufException {
        ConfidentialMessage message = ConfidentialMessage.fromProto(getConfidentialMessageProto());

        assertArrayEquals(message.serialize(), message.encodeCanonical());
        assertArrayEquals(message.toValueProto(false).toByteArray(),
                CanonicalEncoder.DEFAULT.encode(message,
                        ConfidentialMessage.DEFAULT_VALUE_SCHEMA,
                        EncodingPurpose.DEFAULT));
    }

    @Test
    public void mailboxDataAndRequestsMatchProtobuf() throws UnresolvableProtobufException {
        MailboxData mailboxData = getMailboxData();
        assertArrayEquals(mailboxData.serialize(), mailboxData.encodeCanonical());
        // serializeForHash() omits metaData and version
        assertFalse(Arrays.equals(mailboxData.serializeForHash(), mailboxData.encodeCanonical()));

        MailboxSequentialData sequentialData = getMailboxSequentialData();
        assertArrayEquals(sequentialData.serialize(), sequentialData.encodeCanonical());

        AddMailboxRequest addRequest = getAddMailboxRequest();
        assertArrayEquals(addRequest.serialize(), addRequest.encodeCanonical());
        // serializeForHash() omits signatureV2 and version
        assertFalse(Arrays.equals(addRequest.serializeForHash(), addRequest.encodeCanonical()));

        RemoveMailboxRequest removeRequest = getRemoveMailboxRequest();
        assertArrayEquals(removeRequest.serialize(), removeRequest.encodeCanonical());
        assertFalse(Arrays.equals(removeRequest.serializeForHash(), removeRequest.encodeCanonical()));
    }

    @Test
    public void addAppendOnlyDataRequestEncodesExternalDataWithHashSerializationOnlyForProofOfWork() {
        AddAppendOnlyDataRequest withExcludedField = new AddAppendOnlyDataRequest(new ExternalData("content", 7));
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical());
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical(EncodingPurpose.SIGNATURE));
        assertArrayEquals(withExcludedField.serializeForHash(),
                withExcludedField.encodeCanonical(EncodingPurpose.PROOF_OF_WORK));
        assertFalse(Arrays.equals(withExcludedField.serialize(), withExcludedField.serializeForHash()));

        AddAppendOnlyDataRequest withoutExcludedField = new AddAppendOnlyDataRequest(new ExternalData("content", 0));
        assertArrayEquals(withoutExcludedField.serialize(), withoutExcludedField.encodeCanonical());
    }

    // AppendOnlyData supports canonical encoding: the bare message, without the Any in which the request carries it
    @Test
    public void appendOnlyDataHashPurposesMatchSerializeForHash() {
        ExternalData withExcludedField = new ExternalData("content", 7);
        assertArrayEquals(withExcludedField.serialize(), withExcludedField.encodeCanonical());
        assertHashSerializationPurposesMatchSerializeForHash(withExcludedField, "");
        assertFalse(Arrays.equals(withExcludedField.serialize(), withExcludedField.serializeForHash()));
    }

    // The hash purposes leave out the fields annotated with @ExcludeForHash, also in nested objects, so they give the
    // bytes of serializeForHash(). The versions cover the @ExcludeForHash(excludeOnlyInVersions = ...) annotations of
    // nested classes, and show that no version of the storage requests includes the deprecated metaData. AuthorizedData is an exception, see
    // authorizedDataContainsSignatureUnlikeHashSerialization(). Data of higher-level modules is the second exception:
    // the SIGNATURE purpose contains its exact bytes, with the fields excluded from the hash. The classes with
    // signature V2, and the classes which contain one of them, are the third exception: their SIGNATURE purpose is the
    // preimage of signature V2, so only their STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT purposes equal
    // serializeForHash().
    @Test
    public void hashPurposesMatchSerializeForHash() throws UnresolvableProtobufException {
        assertHashPurposesMatchSerializeForHash(getMetaData(), "");
        assertStorageKeyMatchesSerializeForHash(new DefaultAuthenticatedData(new ExternalData("content", 7)));
        for (AuthenticatedData authenticatedData : List.of(getDefaultAuthenticatedData(), getAuthorizedData())) {
            // The data hash of the storage key, which remove and refresh requests of all versions carry
            assertArrayEquals(authenticatedData.serializeForHash(),
                    authenticatedData.encodeCanonical(EncodingPurpose.STORAGE_KEY),
                    authenticatedData.getClass().getSimpleName());
            AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authenticatedData,
                    3,
                    PUBLIC_KEY_HASH,
                    CREATED);
            assertArrayEquals(sequentialData.serializeForHash(), sequentialData.encodeCanonical(EncodingPurpose.SIGNATURE));
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(sequentialData, "");
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(getAddAuthenticatedDataRequest(sequentialData), "");
        }
        assertHashPurposesMatchSerializeForHash(ConfidentialMessage.fromProto(getConfidentialMessageProto()), "");
        AddAppendOnlyDataRequest addAppendOnlyDataRequest = new AddAppendOnlyDataRequest(new ExternalData("content", 7));
        assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(addAppendOnlyDataRequest, "");
        assertArrayEquals(addAppendOnlyDataRequest.serialize(),
                addAppendOnlyDataRequest.encodeCanonical(EncodingPurpose.SIGNATURE));

        for (int version = 0; version <= 4; version++) {
            String description = "version " + version;
            MailboxData mailboxData = getMailboxData(version);
            MailboxSequentialData mailboxSequentialData = getMailboxSequentialData(mailboxData);
            assertHashSerializationPurposesMatchSerializeForHash(mailboxData, description);
            // The purpose reaches MailboxData
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(mailboxSequentialData, description);
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(getAddMailboxRequest(mailboxSequentialData), description);
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(getRemoveMailboxRequest(version), description);
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(getRemoveAuthenticatedDataRequest(version), description);
            // The refresh request has no preimage of its own. Its SIGNATURE purpose is the default encoding.
            RefreshAuthenticatedDataRequest refreshRequest = getRefreshAuthenticatedDataRequest(version);
            assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(refreshRequest, description);
            assertArrayEquals(refreshRequest.serialize(), refreshRequest.encodeCanonical(EncodingPurpose.SIGNATURE));
        }
    }

    // The proof of work of a message's authorization token binds the token to serializeForHash() of the message
    // (HashCashV2TokenService). The PROOF_OF_WORK purpose must reproduce these bytes for every
    // message with a canonical encoding: all DataRequest types and ConfidentialMessage. AuthorizedData is always nested
    // in these messages, so its signature is part of the payload. Data of higher-level modules is contained in its hash
    // serialization, also when it has a field excluded from the hash. TRADE_CONTRACT selects the same bytes for the
    // classes which a contract does not contain.
    @Test
    public void nestedHashPurposesMatchSerializeForHashOfEveryMessage() throws UnresolvableProtobufException {
        List<Proto> messages = new ArrayList<>();
        for (AuthenticatedData authenticatedData : List.of(getDefaultAuthenticatedData(),
                getAuthorizedData(),
                new DefaultAuthenticatedData(new ExternalData("content", 7)),
                new AuthorizedData(new ExternalData("content", 7), Optional.of(SIGNATURE), KEY_PAIR.getPublic()))) {
            messages.add(getAddAuthenticatedDataRequest(new AuthenticatedSequentialData(authenticatedData,
                    3,
                    PUBLIC_KEY_HASH,
                    CREATED)));
        }
        messages.add(new AddAppendOnlyDataRequest(new ExternalData("content", 0)));
        messages.add(new AddAppendOnlyDataRequest(new ExternalData("content", 7)));
        messages.add(ConfidentialMessage.fromProto(getConfidentialMessageProto()));
        for (int version = 0; version <= 4; version++) {
            messages.add(getAddMailboxRequest(getMailboxSequentialData(getMailboxData(version))));
            messages.add(getRemoveMailboxRequest(version));
            messages.add(getRemoveAuthenticatedDataRequest(version));
            messages.add(getRefreshAuthenticatedDataRequest(version));
        }

        for (Proto message : messages) {
            for (EncodingPurpose encodingPurpose : NESTED_HASH_SERIALIZATION_PURPOSES) {
                assertArrayEquals(message.serializeForHash(),
                        ((Canonical) message).encodeCanonical(encodingPurpose),
                        message.getClass().getSimpleName() + " " + encodingPurpose);
            }
        }
    }

    // ExternalData gives the same bytes for PROOF_OF_WORK and TRADE_CONTRACT, so the byte comparisons would not notice
    // if the request passed the wrong hash purpose on. DEFAULT and SIGNATURE use the kept Any and do not encode the
    // AppendOnlyData.
    @Test
    public void addAppendOnlyDataRequestPassesEncodingPurposeToAppendOnlyData() {
        PurposeRecordingAppendOnlyData appendOnlyData = new PurposeRecordingAppendOnlyData();
        AddAppendOnlyDataRequest request = new AddAppendOnlyDataRequest(appendOnlyData);
        for (EncodingPurpose encodingPurpose : List.of(EncodingPurpose.DEFAULT, EncodingPurpose.SIGNATURE)) {
            appendOnlyData.encodingPurposes.clear();

            request.encodeCanonical(encodingPurpose);

            assertEquals(List.of(), appendOnlyData.encodingPurposes, encodingPurpose.name());
        }
        for (EncodingPurpose encodingPurpose : NESTED_HASH_SERIALIZATION_PURPOSES) {
            appendOnlyData.encodingPurposes.clear();

            request.encodeCanonical(encodingPurpose);

            assertEquals(List.of(encodingPurpose), appendOnlyData.encodingPurposes);
        }
    }

    @Test
    public void addAuthenticatedDataRequestPassesEncodingPurposeToAuthenticatedData() {
        // A request has no STORAGE_KEY encoding, see assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey
        for (EncodingPurpose encodingPurpose : List.of(EncodingPurpose.DEFAULT,
                EncodingPurpose.SIGNATURE,
                EncodingPurpose.PROOF_OF_WORK,
                EncodingPurpose.TRADE_CONTRACT)) {
            PurposeRecordingAuthenticatedData authenticatedData = new PurposeRecordingAuthenticatedData();
            AddAuthenticatedDataRequest request = getAddAuthenticatedDataRequest(
                    new AuthenticatedSequentialData(authenticatedData, 1, PUBLIC_KEY_HASH, CREATED));

            request.encodeCanonical(encodingPurpose);

            assertEquals(List.of(encodingPurpose), authenticatedData.encodingPurposes);
        }
    }

    private static <T extends Proto & Canonical> void assertHashPurposesMatchSerializeForHash(T value,
                                                                                            String description) {
        for (EncodingPurpose encodingPurpose : HASH_PURPOSES) {
            assertArrayEquals(value.serializeForHash(), value.encodeCanonical(encodingPurpose),
                    value.getClass().getSimpleName() + " " + encodingPurpose + " " + description);
        }
    }

    private static <T extends Proto & Canonical> void assertHashSerializationPurposesMatchSerializeForHash(
            T value,
            String description) {
        for (EncodingPurpose encodingPurpose : HASH_SERIALIZATION_PURPOSES) {
            assertArrayEquals(value.serializeForHash(), value.encodeCanonical(encodingPurpose),
                    value.getClass().getSimpleName() + " " + encodingPurpose + " " + description);
        }
    }

    // Only the stored data has a STORAGE_KEY encoding (see StorageKeyV2). The requests and the sequential data which
    // contain it have none.
    private static <T extends Proto & Canonical> void assertNestedHashPurposesMatchSerializeForHashWithoutStorageKey(
            T value,
            String description) {
        for (EncodingPurpose encodingPurpose : NESTED_HASH_SERIALIZATION_PURPOSES) {
            assertArrayEquals(value.serializeForHash(), value.encodeCanonical(encodingPurpose),
                    value.getClass().getSimpleName() + " " + encodingPurpose + " " + description);
        }
        assertThrows(IllegalArgumentException.class, () -> value.encodeCanonical(EncodingPurpose.STORAGE_KEY),
                value.getClass().getSimpleName() + " " + description);
    }

    // For a holder of external data with a field excluded from the hash
    private static <T extends Proto & Canonical> void assertStorageKeyMatchesSerializeForHash(T value) {
        for (EncodingPurpose encodingPurpose : HASH_SERIALIZATION_PURPOSES) {
            assertArrayEquals(value.serializeForHash(), value.encodeCanonical(encodingPurpose),
                    value.getClass().getSimpleName() + " " + encodingPurpose);
        }
        assertArrayEquals(value.serialize(), value.encodeCanonical(EncodingPurpose.SIGNATURE),
                value.getClass().getSimpleName());
    }

    private static MetaData getMetaData() {
        return new MetaData(MetaData.TTL_10_DAYS, MetaData.LOW_PRIORITY, "ExternalData", MetaData.MAX_MAP_SIZE_5000);
    }

    private static DefaultAuthenticatedData getDefaultAuthenticatedData() {
        return new DefaultAuthenticatedData(new ExternalData("content", 0));
    }

    private static AuthorizedData getAuthorizedData() {
        return new AuthorizedData(new ExternalData("content", 0), Optional.of(SIGNATURE), KEY_PAIR.getPublic());
    }

    // With signature V2 and version 2, as nodes of this version create it
    private static AddAuthenticatedDataRequest getAddAuthenticatedDataRequest(AuthenticatedSequentialData sequentialData) {
        return new AddAuthenticatedDataRequest(sequentialData,
                SIGNATURE,
                PUBLIC_KEY_BYTES,
                KEY_PAIR.getPublic(),
                SIGNATURE_V2,
                2);
    }

    private static RemoveAuthenticatedDataRequest getRemoveAuthenticatedDataRequest()
            throws UnresolvableProtobufException {
        return getRemoveAuthenticatedDataRequest(1);
    }

    private static RemoveAuthenticatedDataRequest getRemoveAuthenticatedDataRequest(int version)
            throws UnresolvableProtobufException {
        return RemoveAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSequenceNumber(2)
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setCreated(CREATED)
                .setVersion(version)
                .setSignatureV2(ByteString.copyFrom(SIGNATURE_V2))
                .build());
    }

    private static RefreshAuthenticatedDataRequest getRefreshAuthenticatedDataRequest()
            throws UnresolvableProtobufException {
        return getRefreshAuthenticatedDataRequest(1);
    }

    private static RefreshAuthenticatedDataRequest getRefreshAuthenticatedDataRequest(int version)
            throws UnresolvableProtobufException {
        return RefreshAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSequenceNumber(2)
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setVersion(version)
                .setCreated(CREATED)
                .setSignatureV2(ByteString.copyFrom(SIGNATURE_V2))
                .build());
    }

    private static bisq.network.protobuf.ConfidentialMessage getConfidentialMessageProto() {
        return bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(bisq.security.protobuf.ConfidentialData.newBuilder()
                        .setSenderPublicKey(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                        .setIv(ByteString.copyFrom(filledBytes(16, 0x33)))
                        .setCipherText(ByteString.copyFrom(filledBytes(40, 0x44)))
                        .setSignature(ByteString.copyFrom(SIGNATURE)))
                .setReceiverKeyId("receiver-key-id")
                .build();
    }

    private static MailboxData getMailboxData() throws UnresolvableProtobufException {
        return getMailboxData(1);
    }

    private static MailboxData getMailboxData(int version) throws UnresolvableProtobufException {
        return MailboxData.fromProto(bisq.network.protobuf.MailboxData.newBuilder()
                .setConfidentialMessage(getConfidentialMessageProto())
                .setMetaData(getMetaData().toProto(false))
                .setVersion(version)
                .build());
    }

    private static MailboxSequentialData getMailboxSequentialData() throws UnresolvableProtobufException {
        return getMailboxSequentialData(getMailboxData());
    }

    private static MailboxSequentialData getMailboxSequentialData(MailboxData mailboxData) {
        return new MailboxSequentialData(mailboxData,
                PUBLIC_KEY_HASH,
                PUBLIC_KEY_HASH,
                KEY_PAIR.getPublic(),
                PUBLIC_KEY_BYTES,
                CREATED,
                1);
    }

    private static AddMailboxRequest getAddMailboxRequest() throws UnresolvableProtobufException {
        return getAddMailboxRequest(getMailboxSequentialData());
    }

    private static AddMailboxRequest getAddMailboxRequest(MailboxSequentialData mailboxSequentialData)
            throws UnresolvableProtobufException {
        return AddMailboxRequest.fromProto(bisq.network.protobuf.AddMailboxRequest.newBuilder()
                .setMailboxSequentialData(mailboxSequentialData.toProto(false))
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setSenderPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSignatureV2(ByteString.copyFrom(SIGNATURE_V2))
                .setVersion(2)
                .build());
    }

    private static RemoveMailboxRequest getRemoveMailboxRequest() throws UnresolvableProtobufException {
        return getRemoveMailboxRequest(1);
    }

    private static RemoveMailboxRequest getRemoveMailboxRequest(int version) throws UnresolvableProtobufException {
        return RemoveMailboxRequest.fromProto(bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setCreated(CREATED)
                .setVersion(version)
                .setSignatureV2(ByteString.copyFrom(SIGNATURE_V2))
                .build());
    }

    private static byte[] filledBytes(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    // Data of a higher-level module, held in an Any. It serializes as network.MetaData so that no resolver is needed.
    // priority is excluded from the hash, as the name matches the protobuf field.
    // Names the test key as its owner, so that requests which this key publishes are valid
    private static class ExternalData implements AuthorizedDistributedData, AppendOnlyData, OwnershipVerifiableData {
        private final String content;
        @ExcludeForHash
        private final int priority;
        private final transient MetaData metaData = new MetaData(MetaData.TTL_10_DAYS, "ExternalData");

        private ExternalData(String content, int priority) {
            this.content = content;
            this.priority = priority;
        }

        @Override
        public void verify() {
        }

        @Override
        public byte[] getOwnerPubKeyHash() {
            return PUBLIC_KEY_HASH;
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder()
                    .setClassName(content)
                    .setPriority(priority);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return metaData;
        }


        private static final CanonicalSchema<ExternalData> DEFAULT_SCHEMA = CanonicalSchema.<ExternalData>newBuilder()
                .sint32(2, data -> data.priority)
                .string(4, data -> data.content)
                .build();

        // Leaves out priority (field 2), which is annotated with @ExcludeForHash, as serializeForHash() does
        private static final CanonicalSchema<ExternalData> SCHEMA_FOR_HASH = CanonicalSchema.<ExternalData>newBuilder()
                .string(4, data -> data.content)
                .build();

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            CanonicalSchema<ExternalData> schema = switch (encodingPurpose) {
                case DEFAULT -> DEFAULT_SCHEMA;
                case SIGNATURE -> DEFAULT_SCHEMA;
                case STORAGE_KEY -> SCHEMA_FOR_HASH;
                case PROOF_OF_WORK -> SCHEMA_FOR_HASH;
                case TRADE_CONTRACT -> SCHEMA_FOR_HASH;
            };
            return canonicalEncoder.encode(this, schema, encodingPurpose);
        }
    }

    // AppendOnlyData which records the encoding purposes it receives
    private static final class PurposeRecordingAppendOnlyData extends ExternalData {
        private final List<EncodingPurpose> encodingPurposes = new ArrayList<>();

        private PurposeRecordingAppendOnlyData() {
            super("content", 0);
        }

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            encodingPurposes.add(encodingPurpose);
            return super.encodeCanonical(canonicalEncoder, encodingPurpose);
        }
    }

    // AuthenticatedData which records the encoding purposes it receives
    private static final class PurposeRecordingAuthenticatedData extends AuthenticatedData {
        private final List<EncodingPurpose> encodingPurposes = new ArrayList<>();

        private PurposeRecordingAuthenticatedData() {
            super(new ExternalData("content", 0));
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public bisq.network.protobuf.AuthenticatedData.Builder getBuilder(boolean serializeForHash) {
            return getAuthenticatedDataBuilder(serializeForHash).setDefaultAuthenticatedData(
                    bisq.network.protobuf.DefaultAuthenticatedData.newBuilder());
        }

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            encodingPurposes.add(encodingPurpose);
            return new byte[0];
        }
    }
}
