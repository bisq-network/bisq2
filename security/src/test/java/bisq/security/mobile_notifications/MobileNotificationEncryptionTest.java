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

package bisq.security.mobile_notifications;

import bisq.common.encoding.Base64;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobileNotificationEncryptionTest {
    @Test
    void messageIsEncryptedWithAKeyOfSecp256k1AndOfSecp256r1() throws GeneralSecurityException {
        for (String curve : new String[]{"secp256k1", "secp256r1"}) {
            KeyPair keyPair = KeyGeneration.generateKeyPair(curve, KeyGeneration.ECDH);
            String publicKeyBase64 = Base64.encode(keyPair.getPublic().getEncoded());

            assertFalse(MobileNotificationEncryption.encrypt(publicKeyBase64, "message").isEmpty());
        }
    }

    // The device registration stores the key without a check. A key with explicit curve parameters and a large order
    // takes the key parser seconds to reject, so the size is checked before the parser runs.
    @Test
    void storedKeyAboveTheSizeLimitIsRejectedBeforeTheKeyParserRuns() {
        String oversizedKeyBase64 = Base64.encode(new byte[100_000]);

        GeneralSecurityException exception = assertThrows(GeneralSecurityException.class,
                () -> MobileNotificationEncryption.encrypt(oversizedKeyBase64, "message"));

        InvalidKeySpecException cause = assertInstanceOf(InvalidKeySpecException.class, exception.getCause());
        assertTrue(cause.getMessage().startsWith("EC public key not of the expected size"), cause.getMessage());
    }
}
