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
import network.misq.common.encoding.Hex;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedData;

import java.security.PublicKey;

@Getter
@EqualsAndHashCode(callSuper = true)
public class MailboxData extends AuthenticatedData {
    private final byte[] receiversPubKeyBytes;
    private final byte[] hashOfReceiversPublicKey;
    transient final private PublicKey receiversPubKey;

    public MailboxData(MailboxPayload data,
                       int sequenceNumber,
                       byte[] hashOfSenderPublicKey,
                       byte[] hashOfReceiversPublicKey,
                       PublicKey receiversPubKey) {
        this(data,
                sequenceNumber,
                hashOfSenderPublicKey,
                hashOfReceiversPublicKey,
                receiversPubKey,
                System.currentTimeMillis());
    }

    public MailboxData(MailboxPayload data,
                       int sequenceNumber,
                       byte[] hashOfSenderPublicKey,
                       byte[] hashOfReceiversPublicKey,
                       PublicKey receiversPubKey,
                       long created) {
        super(data, sequenceNumber, hashOfSenderPublicKey, created);

        receiversPubKeyBytes = receiversPubKey.getEncoded();
        this.hashOfReceiversPublicKey = hashOfReceiversPublicKey;
        this.receiversPubKey = receiversPubKey;
    }

    @Override
    public String toString() {
        return "MailboxData{" +
                "\r\n     receiversPubKeyBytes=" + Hex.encode(receiversPubKeyBytes) +
                ",\r\n     hashOfReceiversPublicKey=" + Hex.encode(hashOfReceiversPublicKey) +
                "\r\n} " + super.toString();
    }

    public MailboxPayload getMailboxPayload() {
        return (MailboxPayload) payload;
    }
}
