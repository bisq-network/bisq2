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

package bisq.api.access.identity;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientProfileTest {
    private static final String CLIENT_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    private static final String CLIENT_SECRET = "secret";
    /** SHA-256 of the UTF-8 bytes of "secret"; pinned so the stored shape cannot drift silently. */
    private static final String CLIENT_SECRET_HASH =
            "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b";

    @Test
    void storesTheSha256OfTheSecretAsPresented() {
        ClientProfile clientProfile = ClientProfile.fromSecret(CLIENT_ID, CLIENT_SECRET, "Pixel 8");

        assertArrayEquals(HexFormat.of().parseHex(CLIENT_SECRET_HASH), clientProfile.getClientSecretHash());
    }

    @Test
    void matchesOnlyTheSecretItWasBuiltFrom() {
        ClientProfile clientProfile = ClientProfile.fromSecret(CLIENT_ID, CLIENT_SECRET, "Pixel 8");

        assertTrue(clientProfile.matchesSecret(CLIENT_SECRET));
        assertFalse(clientProfile.matchesSecret("Secret"));
        assertFalse(clientProfile.matchesSecret(""));
        // Presenting the stored hash itself is not the secret.
        assertFalse(clientProfile.matchesSecret(CLIENT_SECRET_HASH));
    }

    @Test
    void protoRoundTripCarriesTheHashAndNeverThePlaintext() {
        ClientProfile clientProfile = ClientProfile.fromSecret(CLIENT_ID, CLIENT_SECRET, "Pixel 8");

        bisq.api.protobuf.ClientProfile proto = clientProfile.toProto(false);

        assertTrue(proto.getClientSecret().isEmpty());
        assertFalse(ClientProfile.hasLegacyPlaintextSecret(proto));
        assertEquals(clientProfile, ClientProfile.fromProto(proto));
    }

    @Test
    void legacyPlaintextIsHashedOnRead() {
        bisq.api.protobuf.ClientProfile legacy = bisq.api.protobuf.ClientProfile.newBuilder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .setClientName("Pixel 8")
                .build();

        assertTrue(ClientProfile.hasLegacyPlaintextSecret(legacy));
        assertEquals(ClientProfile.fromSecret(CLIENT_ID, CLIENT_SECRET, "Pixel 8"), ClientProfile.fromProto(legacy));
    }
}
