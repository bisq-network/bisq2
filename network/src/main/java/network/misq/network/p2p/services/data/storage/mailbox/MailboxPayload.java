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

package network.misq.network.p2p.services.data.storage.mailbox;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.network.p2p.services.confidential.ConfidentialMessage;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import network.misq.security.ConfidentialData;
import network.misq.security.HybridEncryption;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

// We want to have fine grained control over mailbox messages.
// As the data is encrypted we could not use it's TTL and we would merge all mailbox message into one storage file.
// By wrapping the sealed data into that NetworkData we can add the fileName and ttl from the unencrypted NetworkData.
@EqualsAndHashCode

public class MailboxPayload implements AuthenticatedPayload {
    public static MailboxPayload createMailboxPayload(MailboxMessage mailboxMessage,
                                                      KeyPair senderKeyPair,
                                                      PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(mailboxMessage.serialize(), receiverPublicKey, senderKeyPair);
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(confidentialData, "DEFAULT");
        return new MailboxPayload(confidentialMessage, mailboxMessage.getMetaData());
    }

    @Getter
    private final ConfidentialMessage confidentialMessage;
    private final MetaData metaData;

    public MailboxPayload(ConfidentialMessage confidentialMessage, MetaData metaData) {
        this.confidentialMessage = confidentialMessage;
        this.metaData = metaData;
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    @Override
    public String toString() {
        return "MailboxPayload{" +
                "\r\n     confidentialMessage=" + confidentialMessage +
                ",\r\n     metaData='" + metaData + '\'' +
                "\r\n}";
    }
}
