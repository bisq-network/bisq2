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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
public class ScryptKeyDeriverTest {
    @Test
    void testDeriveAESKey() throws GeneralSecurityException {
        String password = "test_password";

        ScryptKeyDeriver scryptKeyDeriver1 = new ScryptKeyDeriver();
        AesSecretKey key1 = scryptKeyDeriver1.deriveKeyFromPassword(password);

        ScryptKeyDeriver scryptKeyDeriver2 = new ScryptKeyDeriver();
        AesSecretKey key2 = scryptKeyDeriver2.deriveKeyFromPassword(password);
        assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));

        // Only with same salt we get same key
        ScryptKeyDeriver scryptKeyDeriver3 = new ScryptKeyDeriver(scryptKeyDeriver1.getScryptParameters());
        AesSecretKey key3 = scryptKeyDeriver3.deriveKeyFromPassword(password);
        assertArrayEquals(key1.getEncoded(), key3.getEncoded());
    }
}
