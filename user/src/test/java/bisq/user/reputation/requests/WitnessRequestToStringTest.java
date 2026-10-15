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

package bisq.user.reputation.requests;

import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The oracle logs these requests. The log must not link the Bisq 2 profile to the Bisq 1 account.
class WitnessRequestToStringTest {
    private static final String PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String HASH_AS_HEX = "2222222222222222222222222222222222222222";
    private static final String PUB_KEY_BASE64 = Base64.getEncoder()
            .encodeToString(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded());
    private static final String SIGNATURE_BASE64 = Base64.getEncoder().encodeToString(new byte[70]);

    @Test
    void accountAgeRequestNamesOnlyTheProfile() {
        String text = new AuthorizeAccountAgeRequest(PROFILE_ID, HASH_AS_HEX, new byte[]{1}, PUB_KEY_BASE64,
                SIGNATURE_BASE64).toString();

        assertWithoutBisq1AccountData(text);
    }

    @Test
    void signedWitnessRequestNamesOnlyTheProfile() {
        String text = new AuthorizeSignedWitnessRequest(PROFILE_ID, HASH_AS_HEX, new byte[]{1}, PUB_KEY_BASE64,
                SIGNATURE_BASE64).toString();

        assertWithoutBisq1AccountData(text);
    }

    private static void assertWithoutBisq1AccountData(String text) {
        assertTrue(text.contains(PROFILE_ID), text);
        assertFalse(text.contains(HASH_AS_HEX), text);
        assertFalse(text.contains(PUB_KEY_BASE64), text);
        assertFalse(text.contains(SIGNATURE_BASE64), text);
    }
}
