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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.EncodingPurpose;
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

/**
 * The default canonical encoding of the storage classes contains all non-transient fields, with the envelopes that
 * serialize() writes. Every class uses it for all encoding purposes and passes the purpose on to its nested objects.
 * Data of higher-level modules, which the storage classes hold in a google.protobuf.Any, is encoded with its protobuf
 * serialization for hashes. The external data stub therefore has a field that is excluded from the hash: with it, the
 * canonical bytes equal serializeForHash() of classes without own excluded fields, and without it they equal
 * serialize().
 */
public class StorageCanonicalEncoderTest {
    private static final KeyPair KEY_PAIR = KeyGeneration.generateDefaultEcKeyPair();
    private static final byte[] PUBLIC_KEY_BYTES = KEY_PAIR.getPublic().getEncoded();
    private static final byte[] HASH = filledBytes(20, 0x11);
    private static final byte[] OTHER_HASH = filledBytes(20, 0x55);
    private static final byte[] SIGNATURE = filledBytes(70, 0x22);
    private static final long CREATED = System.currentTimeMillis() - 60_000;

    @Test
    public void metaDataMatchesProtobuf() {
        MetaData metaData = getMetaData();

        assertArrayEquals(metaData.serialize(), metaData.encodeCanonical());
        assertArrayEquals(metaData.serializeForHash(), metaData.encodeCanonical());
    }

    @Test
    public void authenticatedDataEncodesExternalDataWithHashSerialization() {
        DefaultAuthenticatedData withExcludedField = new DefaultAuthenticatedData(new ExternalData("content", 7));
        assertArrayEquals(withExcludedField.serializeForHash(), withExcludedField.encodeCanonical());
        assertFalse(Arrays.equals(withExcludedField.serialize(), withExcludedField.encodeCanonical()));

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
    }

    @Test
    public void authenticatedSequentialDataAndAddRequestMatchProtobufForEveryAuthenticatedDataType() {
        for (AuthenticatedData authenticatedData : List.of(getDefaultAuthenticatedData(), getAuthorizedData())) {
            AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authenticatedData,
                    3,
                    HASH,
                    CREATED);
            assertArrayEquals(sequentialData.serialize(), sequentialData.encodeCanonical());

            AddAuthenticatedDataRequest request = new AddAuthenticatedDataRequest(sequentialData,
                    SIGNATURE,
                    PUBLIC_KEY_BYTES,
                    KEY_PAIR.getPublic());
            assertArrayEquals(request.serialize(), request.encodeCanonical());
        }
    }

    @Test
    public void removeAndRefreshAuthenticatedDataRequestsContainFieldsExcludedFromHash() {
        RemoveAuthenticatedDataRequest removeRequest = getRemoveAuthenticatedDataRequest();
        assertArrayEquals(removeRequest.serialize(), removeRequest.encodeCanonical());
        // serializeForHash() omits metaData and version
        assertFalse(Arrays.equals(removeRequest.serializeForHash(), removeRequest.encodeCanonical()));

        RefreshAuthenticatedDataRequest refreshRequest = getRefreshAuthenticatedDataRequest();
        assertArrayEquals(refreshRequest.serialize(), refreshRequest.encodeCanonical());
        assertFalse(Arrays.equals(refreshRequest.serializeForHash(), refreshRequest.encodeCanonical()));
    }

    @Test
    public void confidentialMessageMatchesProtobufWithAndWithoutEnvelope() {
        ConfidentialMessage message = ConfidentialMessage.fromProto(getConfidentialMessageProto());

        assertArrayEquals(message.serialize(), message.encodeCanonical());
        assertArrayEquals(message.toValueProto(false).toByteArray(),
                CanonicalEncoder.DEFAULT.encode(message,
                        ConfidentialMessage.DEFAULT_VALUE_SCHEMA,
                        EncodingPurpose.DEFAULT));
    }

    @Test
    public void mailboxDataAndRequestsMatchProtobuf() {
        MailboxData mailboxData = getMailboxData();
        assertArrayEquals(mailboxData.serialize(), mailboxData.encodeCanonical());
        // serializeForHash() omits metaData and version
        assertFalse(Arrays.equals(mailboxData.serializeForHash(), mailboxData.encodeCanonical()));

        MailboxSequentialData sequentialData = getMailboxSequentialData();
        assertArrayEquals(sequentialData.serialize(), sequentialData.encodeCanonical());

        AddMailboxRequest addRequest = getAddMailboxRequest();
        assertArrayEquals(addRequest.serialize(), addRequest.encodeCanonical());

        RemoveMailboxRequest removeRequest = getRemoveMailboxRequest();
        assertArrayEquals(removeRequest.serialize(), removeRequest.encodeCanonical());
        assertFalse(Arrays.equals(removeRequest.serializeForHash(), removeRequest.encodeCanonical()));
    }

    @Test
    public void addAppendOnlyDataRequestEncodesExternalDataWithHashSerialization() {
        AddAppendOnlyDataRequest withExcludedField = new AddAppendOnlyDataRequest(new ExternalData("content", 7));
        assertArrayEquals(withExcludedField.serializeForHash(), withExcludedField.encodeCanonical());
        assertFalse(Arrays.equals(withExcludedField.serialize(), withExcludedField.encodeCanonical()));

        AddAppendOnlyDataRequest withoutExcludedField = new AddAppendOnlyDataRequest(new ExternalData("content", 0));
        assertArrayEquals(withoutExcludedField.serialize(), withoutExcludedField.encodeCanonical());
    }

    @Test
    public void everyPurposeUsesTheDefaultEncoding() {
        List<Canonical> canonicals = List.of(getMetaData(),
                getDefaultAuthenticatedData(),
                getAuthorizedData(),
                new AuthenticatedSequentialData(getAuthorizedData(), 1, HASH, CREATED),
                new AddAuthenticatedDataRequest(new AuthenticatedSequentialData(getAuthorizedData(), 1, HASH, CREATED),
                        SIGNATURE,
                        PUBLIC_KEY_BYTES,
                        KEY_PAIR.getPublic()),
                getRemoveAuthenticatedDataRequest(),
                getRefreshAuthenticatedDataRequest(),
                ConfidentialMessage.fromProto(getConfidentialMessageProto()),
                getMailboxData(),
                getMailboxSequentialData(),
                getAddMailboxRequest(),
                getRemoveMailboxRequest(),
                new AddAppendOnlyDataRequest(new ExternalData("content", 7)));

        for (Canonical canonical : canonicals) {
            byte[] defaultBytes = canonical.encodeCanonical();
            for (EncodingPurpose encodingPurpose : EncodingPurpose.values()) {
                assertArrayEquals(defaultBytes, canonical.encodeCanonical(encodingPurpose),
                        canonical.getClass().getSimpleName() + " " + encodingPurpose);
            }
        }
    }

    @Test
    public void addAuthenticatedDataRequestPassesEncodingPurposeToAuthenticatedData() {
        for (EncodingPurpose encodingPurpose : EncodingPurpose.values()) {
            PurposeRecordingAuthenticatedData authenticatedData = new PurposeRecordingAuthenticatedData();
            AddAuthenticatedDataRequest request = new AddAuthenticatedDataRequest(
                    new AuthenticatedSequentialData(authenticatedData, 1, HASH, CREATED),
                    SIGNATURE,
                    PUBLIC_KEY_BYTES,
                    KEY_PAIR.getPublic());

            request.encodeCanonical(encodingPurpose);

            assertEquals(List.of(encodingPurpose), authenticatedData.encodingPurposes);
        }
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

    private static RemoveAuthenticatedDataRequest getRemoveAuthenticatedDataRequest() {
        return RemoveAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSequenceNumber(2)
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setCreated(CREATED)
                .setVersion(1)
                .build());
    }

    private static RefreshAuthenticatedDataRequest getRefreshAuthenticatedDataRequest() {
        return RefreshAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSequenceNumber(2)
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setVersion(1)
                .setCreated(CREATED)
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

    private static MailboxData getMailboxData() {
        return MailboxData.fromProto(bisq.network.protobuf.MailboxData.newBuilder()
                .setConfidentialMessage(getConfidentialMessageProto())
                .setMetaData(getMetaData().toProto(false))
                .setVersion(1)
                .build());
    }

    private static MailboxSequentialData getMailboxSequentialData() {
        return new MailboxSequentialData(getMailboxData(),
                HASH,
                OTHER_HASH,
                KEY_PAIR.getPublic(),
                PUBLIC_KEY_BYTES,
                CREATED,
                1);
    }

    private static AddMailboxRequest getAddMailboxRequest() {
        return AddMailboxRequest.fromProto(bisq.network.protobuf.AddMailboxRequest.newBuilder()
                .setMailboxSequentialData(getMailboxSequentialData().toProto(false))
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setSenderPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .build());
    }

    private static RemoveMailboxRequest getRemoveMailboxRequest() {
        return RemoveMailboxRequest.fromProto(bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setMetaData(getMetaData().toProto(false))
                .setHash(ByteString.copyFrom(HASH))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES))
                .setSignature(ByteString.copyFrom(SIGNATURE))
                .setCreated(CREATED)
                .setVersion(1)
                .build());
    }

    private static byte[] filledBytes(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    // Data of a higher-level module, held in an Any. It serializes as network.MetaData so that no resolver is needed.
    // priority is excluded from the hash, as the name matches the protobuf field.
    private static final class ExternalData implements AuthorizedDistributedData, AppendOnlyData {
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

        @Override
        public boolean isDataInvalid(byte[] pubKeyHash) {
            return false;
        }

        @Override
        public double getCostFactor() {
            return 0.5;
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

        @Override
        public boolean isDataInvalid(byte[] ownerPubKeyHash) {
            return false;
        }
    }
}
