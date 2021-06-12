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

package network.misq.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

class SymEncryption {
    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";
    static final String AES = "AES";
    static final int KEY_SIZE = 256;

    static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        return generateKey(KEY_SIZE);
    }

    static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    static SecretKey generateAESKey(byte[] encoded) {
        return new SecretKeySpec(encoded, 0, encoded.length, AES);
    }

    static byte[] generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(KeyGeneration.ECDH, "BC");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret(AES).getEncoded();
    }

    static IvParameterSpec generateIv() {
        return new IvParameterSpec(new SecureRandom().generateSeed(16));
    }

    static byte[] encrypt(byte[] message, SecretKey secretKey, IvParameterSpec iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(message);
    }

    static byte[] decrypt(byte[] encrypted, SecretKey secretKey, IvParameterSpec iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO, "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher.doFinal(encrypted);
    }
}
