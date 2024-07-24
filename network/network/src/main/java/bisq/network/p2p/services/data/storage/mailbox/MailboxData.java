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
public final class MailboxData implements StorageData {
    private static final int VERSION = 1;
    public final static long MAX_TLL = TimeUnit.DAYS.toMillis(15);

    public static MailboxData cloneWithVersion0(MailboxData mailboxData) {
        return new MailboxData(0, mailboxData.getMetaData(), mailboxData.getConfidentialMessage());
    }

    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final ConfidentialMessage confidentialMessage;

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
        return resolveProto(serializeForHash);
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
