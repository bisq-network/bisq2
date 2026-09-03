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

import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientRevocationServiceTest {
    private static final String CLIENT_ID = "client-1";

    @Test
    void revokeRunsAllRevocationHandlers() {
        // WebSocket auth happens at the handshake only and push registrations are keyed by device,
        // so removing the profile and the session leaves a revoked client connected and notified.
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        ClientRevocationHandler disconnectHandler = mock(ClientRevocationHandler.class);
        ClientRevocationHandler pushHandler = mock(ClientRevocationHandler.class);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenReturn(true);

        assertEquals(ClientRevocationResult.REVOKED, new ClientRevocationService(pairingService,
                sessionService,
                List.of(disconnectHandler, pushHandler)).revokeClient(CLIENT_ID));

        verify(sessionService).removeSessionByClientId(CLIENT_ID);
        verify(disconnectHandler).onClientRevoked(CLIENT_ID);
        verify(pushHandler).onClientRevoked(CLIENT_ID);
    }

    @Test
    void revokeCleansUpEvenWhenNoProfileWasFound() {
        // The profile may have been removed already while a session or connection is still alive.
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenReturn(false);

        assertEquals(ClientRevocationResult.NOT_FOUND,
                new ClientRevocationService(pairingService, sessionService, List.of(handler))
                        .revokeClient(CLIENT_ID));

        verify(sessionService).removeSessionByClientId(CLIENT_ID);
        verify(handler).onClientRevoked(CLIENT_ID);
    }

    @Test
    void aFailingHandlerDoesNotStopTheRemainingOnesAndIsReported() {
        // A revoked client must never stay half connected because one collaborator threw, and the
        // caller must not be told the client was revoked while it can still be connected or
        // receive push notifications.
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler failingHandler = mock(ClientRevocationHandler.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        doThrow(new RuntimeException("boom")).when(failingHandler).onClientRevoked(CLIENT_ID);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenReturn(true);

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of(failingHandler, handler)).revokeClient(CLIENT_ID));

        verify(handler).onClientRevoked(CLIENT_ID);
    }

    @Test
    void aFailingHandlerIsReportedEvenWhenNoProfileExisted() {
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler failingHandler = mock(ClientRevocationHandler.class);
        doThrow(new RuntimeException("boom")).when(failingHandler).onClientRevoked(CLIENT_ID);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenReturn(false);

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of(failingHandler)).revokeClient(CLIENT_ID));
    }
}
