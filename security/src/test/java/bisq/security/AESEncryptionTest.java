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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Slf4j
public class AESEncryptionTest {
    @Test
    void testEncryptDecrypt() throws GeneralSecurityException {
        String password = "test_password";
        ScryptKeyDeriver scryptKeyDeriver1 = new ScryptKeyDeriver();
        ScryptParameters scryptParameters = scryptKeyDeriver1.getScryptParameters();
        AESSecretKey key1 = scryptKeyDeriver1.deriveKeyFromPassword(password);
        byte[] data = "test_data".getBytes();
        EncryptedData encryptedData = AESEncryption.encrypt(data, key1);

        ScryptKeyDeriver scryptKeyDeriver2 = new ScryptKeyDeriver(scryptParameters);
        AESSecretKey key2 = scryptKeyDeriver2.deriveKeyFromPassword(password);
        byte[] decryptedData = AESEncryption.decrypt(encryptedData, key2);
        assertArrayEquals(data, decryptedData);
    }
}
