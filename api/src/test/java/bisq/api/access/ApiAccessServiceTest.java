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
import bisq.api.access.session.InvalidSessionRequestException;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void aSessionIsRefusedWhileARevocationWaitsForItsCleanup() {
        // The profile outlives its permissions until the cleanup succeeds. A session issued in that
        // window is a credential for an access that is already gone.
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        when(pairingService.findClientProfile("client-1"))
                .thenReturn(Optional.of(new ClientProfile("client-1", "secret", "Pixel 8")));
        when(pairingService.hasPermissions("client-1")).thenReturn(false);

        ApiAccessService apiAccessService = new ApiAccessService(pairingService,
                sessionService,
                mock(ClientRevocationService.class));

        assertThrows(InvalidSessionRequestException.class,
                () -> apiAccessService.requestSession("client-1", "secret"));
        verify(sessionService, never()).createSession(anyString());
    }

    @Test
    void aPairedClientStillGetsASession() throws InvalidSessionRequestException {
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        when(pairingService.findClientProfile("client-1"))
                .thenReturn(Optional.of(new ClientProfile("client-1", "secret", "Pixel 8")));
        when(pairingService.hasPermissions("client-1")).thenReturn(true);
        when(sessionService.createSession("client-1")).thenReturn(new SessionToken(60, "client-1"));

        new ApiAccessService(pairingService, sessionService, mock(ClientRevocationService.class))
                .requestSession("client-1", "secret");

        verify(sessionService).createSession("client-1");
    }

    @Test
    void aSessionCreatedWhileTheClientIsBeingRevokedIsWithdrawn() {
        // The revocation lands between the check and the creation, so it sweeps sessions before
        // this one exists. Deterministic rather than threaded: the grant is present for the check
        // and gone for the re-read, which is exactly that interleaving.
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        when(pairingService.findClientProfile("client-1"))
                .thenReturn(Optional.of(new ClientProfile("client-1", "secret", "Pixel 8")));
        when(pairingService.hasPermissions("client-1")).thenReturn(true, false);
        when(sessionService.createSession("client-1")).thenReturn(new SessionToken(60, "client-1"));

        ApiAccessService apiAccessService = new ApiAccessService(pairingService,
                sessionService,
                mock(ClientRevocationService.class));

        assertThrows(InvalidSessionRequestException.class,
                () -> apiAccessService.requestSession("client-1", "secret"));
        verify(sessionService).remove(anyString());
    }
}
