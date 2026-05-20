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

package bisq.common.webcam;

import bisq.common.util.ByteArrayUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class WebcamIpcAuthenticator {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_NUM_BYTES = 32;
    static final int HMAC_LENGTH = 32;

    public static String generateSessionSecret() {
        byte[] bytes = ByteArrayUtils.getRandomBytes(SECRET_NUM_BYTES);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static byte[] createHmac(String sessionSecret, byte[] message) {
        checkArgument(sessionSecret != null && !sessionSecret.isBlank(), "sessionSecret must not be empty");
        checkNotNull(message, "message must not be null");
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            byte[] key = sessionSecret.getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create webcam IPC HMAC", e);
        }
    }

    public static boolean verifyHmac(String sessionSecret,
                                     byte[] message,
                                     byte[] hmac) {
        checkArgument(sessionSecret != null && !sessionSecret.isBlank(), "sessionSecret must not be empty");
        checkNotNull(message, "message must not be null");
        checkNotNull(hmac, "hmac must not be null");
        byte[] expectedHmac = createHmac(sessionSecret, message);
        return MessageDigest.isEqual(expectedHmac, hmac);
    }
}
