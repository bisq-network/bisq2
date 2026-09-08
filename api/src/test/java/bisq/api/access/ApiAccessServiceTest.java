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

import bisq.api.access.identity.ClientManagementId;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiAccessServiceTest {
    @Test
    void revokeClientDelegatesToTheRevocationService() {
        // The facade is the only entry point, so this delegation is what every caller reaches.
        ClientRevocationService clientRevocationService = mock(ClientRevocationService.class);
        when(clientRevocationService.revokeClient("client-1")).thenReturn(ClientRevocationResult.REVOKED);

        assertEquals(ClientRevocationResult.REVOKED, new ApiAccessService(mock(PairingService.class),
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

    @Test
    void revokeByManagementIdResolvesTheClientItNames() {
        PairingService pairingService = mock(PairingService.class);
        ClientProfile clientProfile = new ClientProfile("client-1", "secret", "Pixel 8");
        ClientProfile otherProfile = new ClientProfile("client-2", "other-secret", "iPhone");
        ClientRevocationService clientRevocationService = mock(ClientRevocationService.class);
        when(pairingService.getClientProfiles()).thenReturn(List.of(otherProfile, clientProfile));
        when(clientRevocationService.revokeClient("client-1")).thenReturn(ClientRevocationResult.REVOKED);

        ClientRevocationResult result = new ApiAccessService(pairingService,
                mock(SessionService.class),
                clientRevocationService).revokeClientByManagementId(ClientManagementId.of(clientProfile));

        assertEquals(ClientRevocationResult.REVOKED, result);
        verify(clientRevocationService).revokeClient("client-1");
    }

    @Test
    void revokeByManagementIdReportsNotFoundForAnUnknownHandle() {
        // A handle nothing resolves to must not fall through to any client, and a client ID is not
        // a handle: passing one resolves to nothing.
        PairingService pairingService = mock(PairingService.class);
        ClientProfile clientProfile = new ClientProfile("client-1", "secret", "Pixel 8");
        ClientRevocationService clientRevocationService = mock(ClientRevocationService.class);
        when(pairingService.getClientProfiles()).thenReturn(List.of(clientProfile));

        ApiAccessService apiAccessService = new ApiAccessService(pairingService,
                mock(SessionService.class),
                clientRevocationService);

        assertEquals(ClientRevocationResult.NOT_FOUND, apiAccessService.revokeClientByManagementId("unknown"));
        assertEquals(ClientRevocationResult.NOT_FOUND, apiAccessService.revokeClientByManagementId("client-1"));
        verify(clientRevocationService, never()).revokeClient(anyString());
    }
}
