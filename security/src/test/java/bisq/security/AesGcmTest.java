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
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class AesGcmTest {
    @Test
    void encryptAndDecryptTest() throws GeneralSecurityException {
        KeyPair aliceKeyPair = KeyGeneration.generateKeyPair();
        KeyPair bobKeyPair = KeyGeneration.generateKeyPair();

        // Alice sends to Bob
        SecretKey sharedAesKey = AesGcm.generateSharedAesSecretKey(aliceKeyPair.getPrivate(), bobKeyPair.getPublic());
        byte[] iv = AesGcm.generateIv().getIV();
        String message = "hello, world!";
        byte[] cipherText = AesGcm.encrypt(sharedAesKey, iv, message.getBytes());

        byte[] plainText = AesGcm.decrypt(sharedAesKey, iv, cipherText);
        String decryptedText = new String(plainText);

        assertThat(decryptedText).isEqualTo(message);
    }

    @Test
    void encryptAndDecryptWithPasswordTest() throws GeneralSecurityException {
        String password = "test_password";
        ScryptKeyDeriver scryptKeyDeriver1 = new ScryptKeyDeriver();
        ScryptParameters scryptParameters = scryptKeyDeriver1.getScryptParameters();
        AesSecretKey key1 = scryptKeyDeriver1.deriveKeyFromPassword(password);
        byte[] plainText = "test_data".getBytes();
        byte[] iv = AesGcm.generateIv().getIV();
        byte[] cipherText = AesGcm.encrypt(key1, iv, plainText);
        EncryptedData encryptedData = new EncryptedData(iv, cipherText);

        ScryptKeyDeriver scryptKeyDeriver2 = new ScryptKeyDeriver(scryptParameters);
        AesSecretKey key2 = scryptKeyDeriver2.deriveKeyFromPassword(password);
        byte[] decryptedData = AesGcm.decrypt(key2, encryptedData.getIv(), encryptedData.getCipherText());
        assertArrayEquals(plainText, decryptedData);
    }
}
