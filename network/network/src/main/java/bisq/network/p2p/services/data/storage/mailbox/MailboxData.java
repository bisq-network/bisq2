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
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageData;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

// We want to have fine-grained control over mailbox messages.
// As the data is encrypted we could not use it's metaData, and we would merge all mailbox proto into one storage file.
// To allow more fine-grained control we add the metaData to the MailboxData, though this is untrusted data. 
// For TTL we set an upper bound with MAX_TLL.

/**
 * Holds the ConfidentialMessage and metaData providing information about the message type.
 */
@EqualsAndHashCode
@Getter
public final class MailboxData implements StorageData, Canonical {
    private static final int VERSION = 1;
    public final static long MAX_TLL = TimeUnit.DAYS.toMillis(15);

    private final ConfidentialMessage confidentialMessage;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;

    public MailboxData(MetaData metaData, ConfidentialMessage confidentialMessage) {
        this(VERSION, metaData, confidentialMessage);
    }

    private MailboxData(int version, MetaData metaData, ConfidentialMessage confidentialMessage) {
        this.version = version;
        this.metaData = metaData;
        this.confidentialMessage = confidentialMessage;

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
        return bisq.network.protobuf.MailboxData.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setConfidentialMessage(confidentialMessage.toProto(serializeForHash).getConfidentialMessage());
    }

    public static MailboxData fromProto(bisq.network.protobuf.MailboxData proto) {
        return new MailboxData(
                proto.getVersion(),
                MetaData.fromProto(proto.getMetaData()),
                ConfidentialMessage.fromProto(proto.getConfidentialMessage()));
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // Contains metaData and version, which serializeForHash() omits (metaData only in versions 1 to 3), as the
    // default encoding contains all non-transient fields.
    public static final CanonicalSchema<MailboxData> DEFAULT_SCHEMA = CanonicalSchema.<MailboxData>newBuilder()
            // ConfidentialMessage.encodeCanonical writes the EnvelopePayloadMessage envelope, but MailboxData contains
            // the bare message. The value schema is therefore referenced directly, and a purpose-specific schema of
            // MailboxData must reference the value schema of ConfidentialMessage for the same purpose.
            .compose(1, MailboxData::getConfidentialMessage, ConfidentialMessage.DEFAULT_VALUE_SCHEMA)
            .canonical(2, MailboxData::getMetaData)
            .sint32(3, MailboxData::getVersion)
            .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<MailboxData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    public String getClassName() {
        return metaData.getClassName();
    }

    @Override
    public boolean isDataInvalid(byte[] ownerPubKeyHash) {
        return confidentialMessage.isDataInvalid(ownerPubKeyHash);
    }

    @Override
    public String toString() {
        return "MailboxData{" +
                "metaData=" + metaData +
                ", version=" + version +
                ", confidentialMessage=" + confidentialMessage +
                '}';
    }
}
