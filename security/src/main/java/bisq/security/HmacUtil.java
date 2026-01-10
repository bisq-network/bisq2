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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Slf4j
public class HmacUtil {
    public static final String HMAC = "HmacSHA256";

    public static boolean verifyHmac(byte[] input, SecretKey secretKey, byte[] hmac) throws GeneralSecurityException {
        byte[] hmacTest = createHmac(input, secretKey);
        return Arrays.equals(hmacTest, hmac);
    }

    public static byte[] createHmac(byte[] input, SecretKey secretKey) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(secretKey);
        return mac.doFinal(input);
    }

    public static SecretKeySpec createHmacKeySpec(byte[] key) {
        if (key.length < 32) {
            throw new IllegalArgumentException("HMAC key must be at least 32 bytes");
        }
        return new SecretKeySpec(key, HmacUtil.HMAC);
    }
}
