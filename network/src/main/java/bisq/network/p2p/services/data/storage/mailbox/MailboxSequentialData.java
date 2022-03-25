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

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.security.PublicKey;

@Getter
@EqualsAndHashCode(callSuper = true)
public class MailboxSequentialData extends AuthenticatedSequentialData {
    private final byte[] receiversPubKeyBytes;
    private final byte[] receiversPublicKeyHash;
    transient final private PublicKey receiversPubKey;

    public MailboxSequentialData(MailboxData data,
                                 byte[] hashOfSenderPublicKey,
                                 byte[] receiversPublicKeyHash,
                                 PublicKey receiversPubKey) {
        this(data,
                hashOfSenderPublicKey,
                receiversPublicKeyHash,
                receiversPubKey,
                System.currentTimeMillis());
    }

    public MailboxSequentialData(MailboxData data,
                                 byte[] hashOfSenderPublicKey,
                                 byte[] receiversPublicKeyHash,
                                 PublicKey receiversPubKey,
                                 long created) {
        // We set sequenceNumber to 1 as there will be only one AddMailBoxRequest
        super(data, 1, hashOfSenderPublicKey, created); 

        receiversPubKeyBytes = receiversPubKey.getEncoded();
        this.receiversPublicKeyHash = receiversPublicKeyHash;
        this.receiversPubKey = receiversPubKey;
    }

    @Override
    public String toString() {
        return "MailboxSequentialData{" +
                "\r\n     receiversPubKeyBytes=" + Hex.encode(receiversPubKeyBytes) +
                ",\r\n     receiversPublicKeyHash=" + Hex.encode(receiversPublicKeyHash) +
                "\r\n} " + super.toString();
    }

    public MailboxData getMailboxData() {
        return (MailboxData) authenticatedData;
    }
}
