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

package bisq.security;

import bisq.security.keys.KeyGeneration;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Using Elliptic Curve Integrated Encryption Scheme for hybrid encryption.
 * <p>
 * Follows roughly the schemes described here:
 * <a href="https://cryptobook.nakov.com/asymmetric-key-ciphers/ecies-public-key-encryption">...</a>
 * <a href="https://www.nominet.uk/how-elliptic-curve-cryptography-encryption-works/">...</a>
 */
@Slf4j
public class HybridEncryption {

    public static ConfidentialData encryptAndSign(byte[] message, PublicKey receiverPublicKey, KeyPair senderKeyPair)
            throws GeneralSecurityException {
        // Create shared secret with our private key and receivers public key
        SecretKey sharedAesSecretKey = AesGcm.generateSharedAesSecretKey(senderKeyPair.getPrivate(), receiverPublicKey);

        byte[] iv = AesGcm.generateIv().getIV();
        byte[] cipherText = AesGcm.encrypt(sharedAesSecretKey, iv, message);
        byte[] signature = SignatureUtil.sign(cipherText, senderKeyPair.getPrivate());

        byte[] senderPublicKeyAsBytes = senderKeyPair.getPublic().getEncoded();
        return new ConfidentialData(senderPublicKeyAsBytes, iv, cipherText, signature);
    }

    public static byte[] decryptAndVerify(ConfidentialData confidentialData, KeyPair receiversKeyPair) throws GeneralSecurityException {
        byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();
        byte[] iv = confidentialData.getIv();
        byte[] cipherText = confidentialData.getCipherText();
        byte[] signature = confidentialData.getSignature();

        PublicKey senderPublicKey = KeyGeneration.generatePublic(encodedSenderPublicKey);
        checkArgument(SignatureUtil.verify(cipherText, signature, senderPublicKey), "Invalid signature");

        // Create shared secret with our private key and senders public key
        SecretKey sharedAesSecretKey = AesGcm.generateSharedAesSecretKey(receiversKeyPair.getPrivate(), senderPublicKey);
        return AesGcm.decrypt(sharedAesSecretKey, iv, cipherText);
    }
}
