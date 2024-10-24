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

package bisq.tor;

import bisq.common.network.Address;
import bisq.common.network.AddressOwnershipProof;
import bisq.common.network.AddressOwnershipProofGenerator;
import bisq.security.TorSignatureUtil;
import bisq.security.keys.TorKeyPair;
import org.bouncycastle.crypto.CryptoException;

import java.nio.ByteBuffer;

public class TorAddressOwnershipProofGenerator implements AddressOwnershipProofGenerator {
    private final TorKeyPair torKeyPair;

    public TorAddressOwnershipProofGenerator(TorKeyPair torKeyPair) {
        this.torKeyPair = torKeyPair;
    }

    @Override
    public AddressOwnershipProof generate(Address myAddress, Address peerAddress) {
        long signatureDate = System.currentTimeMillis();
        if (myAddress.isTorAddress()) {
            String message = buildMessageForSigning(myAddress, peerAddress, signatureDate);
            try {
                byte[] signature = TorSignatureUtil.sign(torKeyPair.getPrivateKey(), message.getBytes());
                var proof = ByteBuffer.wrap(signature);
                return new AddressOwnershipProof(signatureDate, proof);
            } catch (CryptoException e) {
                throw new RuntimeException(e);
            }
        }
        return new AddressOwnershipProof(signatureDate);
    }

    private static String buildMessageForSigning(Address signersAddress, Address verifiersAddress, long date) {
        return signersAddress.getFullAddress() + "|" + verifiersAddress.getFullAddress() + "@" + date;
    }
}
