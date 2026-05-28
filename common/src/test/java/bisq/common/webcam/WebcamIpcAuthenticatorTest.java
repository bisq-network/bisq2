/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebcamIpcAuthenticatorTest {
    @Test
    void generatesUrlSafeSessionSecret() {
        String sessionSecret = WebcamIpcAuthenticator.generateSessionSecret();

        assertEquals(43, sessionSecret.length());
        assertTrue(sessionSecret.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void createsDeterministicHmac() {
        byte[] signedContent = "signed-content".getBytes(StandardCharsets.UTF_8);
        byte[] first = WebcamIpcAuthenticator.createHmac("secret", signedContent);
        byte[] second = WebcamIpcAuthenticator.createHmac("secret", signedContent);

        assertArrayEquals(first, second);
        assertEquals(WebcamIpcAuthenticator.HMAC_LENGTH, first.length);
    }

    @Test
    void changesHmacWhenContentChanges() {
        byte[] first = WebcamIpcAuthenticator.createHmac("secret", "signed-content".getBytes(StandardCharsets.UTF_8));
        byte[] second = WebcamIpcAuthenticator.createHmac("secret", "tampered-content".getBytes(StandardCharsets.UTF_8));

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void verifiesHmac() {
        byte[] signedContent = "signed-content".getBytes(StandardCharsets.UTF_8);
        byte[] hmac = WebcamIpcAuthenticator.createHmac("secret", signedContent);

        assertTrue(WebcamIpcAuthenticator.verifyHmac("secret", signedContent, hmac));
    }

    @Test
    void rejectsInvalidHmac() {
        byte[] hmac = WebcamIpcAuthenticator.createHmac("secret", "signed-content".getBytes(StandardCharsets.UTF_8));

        assertFalse(WebcamIpcAuthenticator.verifyHmac("secret", "tampered-content".getBytes(StandardCharsets.UTF_8), hmac));
    }
}
