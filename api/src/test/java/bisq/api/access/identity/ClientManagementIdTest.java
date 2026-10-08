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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientManagementIdTest {
    private static final String CLIENT_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    private static final String CLIENT_SECRET = "0123456789012345678901234567890123456789012";

    private static ClientProfile clientProfile(String clientId, String clientSecret) {
        return new ClientProfile(clientId, clientSecret, "Pixel 8");
    }

    @Test
    void theSameClientAlwaysGetsTheSameId() {
        // Clients hold on to it between calls, so it has to survive restarts and re-reads.
        ClientProfile clientProfile = clientProfile(CLIENT_ID, CLIENT_SECRET);

        assertEquals(ClientManagementId.of(clientProfile),
                ClientManagementId.of(clientProfile(CLIENT_ID, CLIENT_SECRET)));
    }

    @Test
    void theIdRevealsNeitherTheClientIdNorTheSecret() {
        // The whole point: it names a client and carries no credential value.
        String managementId = ClientManagementId.of(clientProfile(CLIENT_ID, CLIENT_SECRET));

        assertFalse(managementId.contains(CLIENT_ID));
        assertFalse(managementId.contains(CLIENT_SECRET));
    }

    @Test
    void differentClientsGetDifferentIds() {
        assertNotEquals(ClientManagementId.of(clientProfile(CLIENT_ID, CLIENT_SECRET)),
                ClientManagementId.of(clientProfile("f81d4fae-7dec-11d0-a765-00a0c91e6bf6", CLIENT_SECRET)));
    }

    @Test
    void theIdIsBoundToTheSecretSoItCannotBeDerivedFromTheClientIdAlone() {
        // Keyed by the client's own secret, so a client ID alone does not yield the handle.
        assertNotEquals(ClientManagementId.of(clientProfile(CLIENT_ID, CLIENT_SECRET)),
                ClientManagementId.of(clientProfile(CLIENT_ID, "9876543210987654321098765432109876543210987")));
    }

    @Test
    void matchesAcceptsOnlyTheClientsOwnId() {
        ClientProfile clientProfile = clientProfile(CLIENT_ID, CLIENT_SECRET);

        assertTrue(ClientManagementId.matches(clientProfile, ClientManagementId.of(clientProfile)));
        assertFalse(ClientManagementId.matches(clientProfile, "not-a-management-id"));
        assertFalse(ClientManagementId.matches(clientProfile, ""));
    }
}
