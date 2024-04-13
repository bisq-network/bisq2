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

import java.util.concurrent.TimeUnit;

// We want to have fine-grained control over mailbox messages.
// As the data is encrypted we could not use it's metaData, and we would merge all mailbox proto into one storage file.
// To allow more fine-grained control we add the metaData to the MailboxData, though this is untrusted data. 
// For TTL we set an upper bound with MAX_TLL.

/**
 * Holds the ConfidentialMessage and metaData providing information about the message type.
 */
@EqualsAndHashCode
@ToString
@Getter
public final class MailboxData implements StorageData {
    public final static long MAX_TLL = TimeUnit.DAYS.toMillis(15);

    private final ConfidentialMessage confidentialMessage;
    private final MetaData metaData;

    public MailboxData(ConfidentialMessage confidentialMessage, MetaData metaData) {
        this.confidentialMessage = confidentialMessage;
        this.metaData = metaData;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.MailboxData toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public bisq.network.protobuf.MailboxData.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.network.protobuf.MailboxData.newBuilder()
                .setConfidentialMessage(confidentialMessage.toProto(ignoreAnnotation).getConfidentialMessage())
                .setMetaData(metaData.toProto(ignoreAnnotation));
    }

    public static MailboxData fromProto(bisq.network.protobuf.MailboxData proto) {
        return new MailboxData(ConfidentialMessage.fromProto(proto.getConfidentialMessage()),
                MetaData.fromProto(proto.getMetaData()));
    }

    public String getClassName() {
        return metaData.getClassName();
    }

    @Override
    public boolean isDataInvalid(byte[] ownerPubKeyHash) {
        return confidentialMessage.isDataInvalid(ownerPubKeyHash);
    }
}
