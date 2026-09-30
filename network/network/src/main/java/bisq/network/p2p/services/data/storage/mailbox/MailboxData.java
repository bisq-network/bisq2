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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageData;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * Holds the ConfidentialMessage and the class name of the encrypted MailboxMessage.
 * <p>
 * The storage layer cannot decrypt the message. It uses the class name to select the store of the message type and
 * to look up the MetaData of that type in the StorableTypesRegistry. The signature does not cover the class name, so a
 * relay can change it to the name of another registered MailboxMessage type. That moves the data into the store of the
 * other type. It cannot make a node use MetaData values which are not registered for a MailboxMessage type, because the
 * mailbox store accepts only class names of MailboxMessage types.
 */
@EqualsAndHashCode
@Getter
public final class MailboxData implements StorageData, Canonical {
    private static final int VERSION = 2;
    // Upper bound for the time to live of mailbox data and mailbox remove requests in the network storage, independent
    // of the time to live which the type of the MailboxMessage declares.
    public static final long MAX_TTL = TimeUnit.DAYS.toMillis(15);

    private final ConfidentialMessage confidentialMessage;

    // Deprecated, replaced by className. Still sent, because nodes of earlier versions use it. This node sends its
    // registered MetaData of the class name (see fromProto) and never uses the field for a decision (see
    // getMetaData()).
    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;

    // Simple class name of the encrypted MailboxMessage. It selects the store.
    // Excluded from the hash, so it does not change signature V1, the storage key or the proof of work payload.
    // Signature V2 of AddMailboxRequest covers it (see SIGNATURE_V2_SCHEMA).
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final String className;

    public MailboxData(MetaData metaData, ConfidentialMessage confidentialMessage) {
        this(VERSION, metaData, confidentialMessage, metaData.getClassName());
    }

    private MailboxData(int version, MetaData metaData, ConfidentialMessage confidentialMessage, String className) {
        this.version = version;
        this.metaData = metaData;
        this.confidentialMessage = confidentialMessage;
        this.className = className;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.MailboxData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.MailboxData.Builder getBuilder(boolean serializeForHash) {
        //noinspection deprecation
        return bisq.network.protobuf.MailboxData.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setConfidentialMessage(confidentialMessage.toProto(serializeForHash).getConfidentialMessage())
                .setClassName(className);
    }

    public static MailboxData fromProto(bisq.network.protobuf.MailboxData proto) throws UnresolvableProtobufException {
        try {
            //noinspection deprecation
            MetaData receivedMetaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ?
                    receivedMetaData.getClassName() :
                    proto.getClassName();
            // Nodes of earlier versions use metaData. Send them the registered values, not those which a sender or relay
            // chose. In these versions serializeForHash() does not contain metaData, so the signature and the storage key
            // of the AddMailboxRequest and the proof of work payload do not change.
            MetaData metaData = isMetaDataExcludedForHash(proto.getVersion()) ?
                    StorableTypesRegistry.getMetaDataForEarlierVersions(className, receivedMetaData) :
                    receivedMetaData;
            return new MailboxData(
                    proto.getVersion(),
                    metaData,
                    ConfidentialMessage.fromProto(proto.getConfidentialMessage()),
                    className);
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // Contains metaData, version and className, which serializeForHash() omits (metaData only in versions 1 to 3), as
    // the default encoding contains all non-transient fields.
    public static final CanonicalSchema<MailboxData> DEFAULT_SCHEMA = CanonicalSchema.<MailboxData>newBuilder()
            // ConfidentialMessage.encodeCanonical writes the EnvelopePayloadMessage envelope, but MailboxData contains
            // the bare message. The value schema is therefore referenced directly, and a purpose-specific schema of
            // MailboxData must reference the value schema of ConfidentialMessage for the same purpose.
            .compose(1, MailboxData::getConfidentialMessage, ConfidentialMessage.DEFAULT_VALUE_SCHEMA)
            // The field metaData, not the MetaData which getMetaData() returns
            .canonical(2, mailboxData -> mailboxData.metaData)
            .sint32(3, MailboxData::getVersion)
            .string(4, MailboxData::getClassName)
            .build();

    // Part of the preimage of signature V2 of AddMailboxRequest. Contains version and className although they are
    // annotated with @ExcludeForHash: className selects the store, and signature V2 has no legacy bytes to reproduce.
    // Leaves out the deprecated metaData (field 2): no node uses the received value for a decision (see getMetaData()).
    public static final CanonicalSchema<MailboxData> SIGNATURE_V2_SCHEMA = CanonicalSchema.<MailboxData>newBuilder()
            .sint32(1, MailboxData::getVersion)
            .string(2, MailboxData::getClassName)
            .compose(3, MailboxData::getConfidentialMessage, ConfidentialMessage.SIGNATURE_VALUE_SCHEMA)
            .build();

    public static final CanonicalSchema<MailboxData> STORAGE_KEY_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<MailboxData> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<MailboxData> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 2) in versions
    // 1 to 3, version (field 3) and className (field 4).
    private static CanonicalSchema<MailboxData> createSchemaForHash() {
        return CanonicalSchema.<MailboxData>newBuilder()
                // ConfidentialMessage uses its default value schema for every purpose
                .compose(1, MailboxData::getConfidentialMessage, ConfidentialMessage.DEFAULT_VALUE_SCHEMA)
                // metaData is annotated with @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
                .canonical(2, mailboxData -> isMetaDataExcludedForHash(mailboxData.version) ?
                        null :
                        mailboxData.metaData)
                .build();
    }

    // Same versions as the @ExcludeForHash annotation of metaData
    private static boolean isMetaDataExcludedForHash(int version) {
        return version == 1 || version == 2 || version == 3;
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<MailboxData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_V2_SCHEMA;
            case STORAGE_KEY -> STORAGE_KEY_SCHEMA;
            case PROOF_OF_WORK -> PROOF_OF_WORK_SCHEMA;
            case TRADE_CONTRACT -> TRADE_CONTRACT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    // The MetaData of the type of the encrypted MailboxMessage, from the StorableTypesRegistry.
    // Throws if the type is not registered. StorageService does not accept such data.
    @Override
    public MetaData getMetaData() {
        return StorableTypesRegistry.getMetaData(className);
    }

    // The MetaData which this message carries in the deprecated field metaData for nodes of earlier versions. This node
    // does not use it for its own decisions.
    public MetaData getMetaDataForEarlierVersions() {
        return metaData;
    }

    // In version 0 serializeForHash() contains the field metaData. Then the signature and the storage key of the
    // AddMailboxRequest and the proof of work payload cover it, so a relay cannot change it.
    public boolean isMetaDataCoveredByHash() {
        return !isMetaDataExcludedForHash(version);
    }

    @Override
    public boolean isDataInvalid() {
        return confidentialMessage.isDataInvalid();
    }

    @Override
    public String toString() {
        return "MailboxData{" +
                "version=" + version +
                ", confidentialMessage=" + confidentialMessage +
                ", className=" + className +
                '}';
    }
}
