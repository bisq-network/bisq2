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

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class SymEncryptionUtilTest {
    @Test
    public void testSymEncryption() {
        try {
            byte[] message = "hello".getBytes();
            SecretKey sessionKey = SymEncryption.generateAESKey();
            IvParameterSpec ivSpec = SymEncryption.generateIv();
            byte[] encryptedMessage = SymEncryption.encrypt(message, sessionKey, ivSpec);
            byte[] iv = ivSpec.getIV();
            byte[] result = SymEncryption.decrypt(encryptedMessage, sessionKey, new IvParameterSpec(iv));
            assertArrayEquals(message, result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail();
        }
    }
}
