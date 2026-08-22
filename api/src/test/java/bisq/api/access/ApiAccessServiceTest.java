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

package bisq.api.access;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiAccessServiceTest {
    @Test
    void revokeClientDelegatesToTheRevocationService() {
        // The facade is the only entry point, so this delegation is what every caller reaches.
        ClientRevocationService clientRevocationService = mock(ClientRevocationService.class);
        when(clientRevocationService.revokeClient("client-1")).thenReturn(true);

        assertTrue(new ApiAccessService(mock(PairingService.class),
                mock(SessionService.class),
                clientRevocationService).revokeClient("client-1"));

        verify(clientRevocationService).revokeClient("client-1");
    }

    @Test
    void getClientProfilesReturnsPairedClients() {
        PairingService pairingService = mock(PairingService.class);
        ClientProfile clientProfile = new ClientProfile("client-1", "secret", "Pixel 8");
        when(pairingService.getClientProfiles()).thenReturn(List.of(clientProfile));

        List<ClientProfile> clientProfiles =
                new ApiAccessService(pairingService,
                        mock(SessionService.class),
                        mock(ClientRevocationService.class)).getClientProfiles();

        assertEquals(List.of(clientProfile), clientProfiles);
    }
}
