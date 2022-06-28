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

import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

// We want to have fine-grained control over mailbox messages.
// As the data is encrypted we could not use it's TTL, and we would merge all mailbox proto into one storage file.
// By wrapping the sealed data into that NetworkData we can add the fileName and ttl from the unencrypted NetworkData.

/**
 * Holds the ConfidentialMessage and metaData providing information about the message type.
 */
@EqualsAndHashCode
@ToString
public final class MailboxData implements StorageData {
    @Getter
    protected final ConfidentialMessage confidentialMessage;
    private final MetaData metaData;

    public MailboxData(ConfidentialMessage confidentialMessage, MetaData metaData) {
        this.confidentialMessage = confidentialMessage;
        this.metaData = metaData;
    }

    public bisq.network.protobuf.MailboxData toProto() {
        return bisq.network.protobuf.MailboxData.newBuilder()
                .setConfidentialMessage(confidentialMessage.toProto().getConfidentialMessage())
                .setMetaData(metaData.toProto())
                .build();
    }

    public static MailboxData fromProto(bisq.network.protobuf.MailboxData proto) {
        return new MailboxData(ConfidentialMessage.fromProto(proto.getConfidentialMessage()),
                MetaData.fromProto(proto.getMetaData()));
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}
