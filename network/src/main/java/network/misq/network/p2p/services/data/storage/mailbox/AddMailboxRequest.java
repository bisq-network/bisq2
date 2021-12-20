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
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import network.misq.security.DigestUtil;
import network.misq.security.SignatureUtil;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public class AddMailboxRequest extends AddAuthenticatedDataRequest implements MailboxRequest {

    public static AddMailboxRequest from(MailboxDataStore store,
                                         MailboxPayload payload,
                                         KeyPair senderKeyPair,
                                         PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        PublicKey senderPublicKey = senderKeyPair.getPublic();
        byte[] hash = DigestUtil.hash(payload.serialize());
        int sequenceNumberFromMap = store.getSequenceNumber(hash);
        if (sequenceNumberFromMap == Integer.MAX_VALUE) {
            throw new IllegalStateException("Item was already removed in service map as sequenceNumber is marked with Integer.MAX_VALUE");
        }
        int newSequenceNumber = sequenceNumberFromMap + 1;
        byte[] hashOfSendersPublicKey = DigestUtil.hash(senderPublicKey.getEncoded());
        byte[] hashOfReceiversPublicKey = DigestUtil.hash(receiverPublicKey.getEncoded());
        MailboxData entry = new MailboxData(payload, newSequenceNumber, hashOfSendersPublicKey,
                hashOfReceiversPublicKey, receiverPublicKey);
        byte[] serialized = entry.serialize();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxRequest(entry, signature, senderPublicKey);
    }

    public AddMailboxRequest(MailboxData mailboxData, byte[] signature, PublicKey senderPublicKey) {
        super(mailboxData, signature, senderPublicKey);
    }

    @Override
    public String toString() {
        return "AddMailboxDataRequest{} " + super.toString();
    }

    public MailboxData getMailboxData() {
        return (MailboxData) authenticatedData;
    }


}
