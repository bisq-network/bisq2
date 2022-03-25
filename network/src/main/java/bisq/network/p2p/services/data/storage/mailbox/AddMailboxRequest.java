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

import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public class AddMailboxRequest extends AddAuthenticatedDataRequest implements MailboxRequest, AddDataRequest {

    public static AddMailboxRequest from(MailboxData mailboxData,
                                         KeyPair senderKeyPair,
                                         PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        PublicKey senderPublicKey = senderKeyPair.getPublic();
        byte[] senderPublicKeyHash = DigestUtil.hash(senderPublicKey.getEncoded());
        byte[] receiverPublicKeyHash = DigestUtil.hash(receiverPublicKey.getEncoded());
        MailboxSequentialData mailboxSequentialData = new MailboxSequentialData(mailboxData,
                senderPublicKeyHash,
                receiverPublicKeyHash,
                receiverPublicKey);
        byte[] serialized = mailboxSequentialData.serialize();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxRequest(mailboxSequentialData, signature, senderPublicKey);
    }

    public AddMailboxRequest(MailboxSequentialData mailboxData, byte[] signature, PublicKey senderPublicKey) {
        super(mailboxData, signature, senderPublicKey);
    }

    @Override
    public String toString() {
        return "AddMailboxDataRequest{} " + super.toString();
    }

    public MailboxSequentialData getMailboxSequentialData() {
        return (MailboxSequentialData) authenticatedSequentialData;
    }

}
