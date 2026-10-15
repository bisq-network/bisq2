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

import bisq.api.ApiConfig;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.PermissionSet;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.access.session.SessionService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void startupRunsTheHandlersForClientsTheStoreDropped() {
        // Profile and grant are already gone, so only what lives outside the store is left: a push
        // registration still owned by the dropped client would keep notifying it and refuse the
        // same device after it pairs again.
        PairingService pairingService = mock(PairingService.class);
        SessionService sessionService = mock(SessionService.class);
        ClientRevocationHandler pushHandler = mock(ClientRevocationHandler.class);
        when(pairingService.getClientProfiles()).thenReturn(List.of());
        when(pairingService.getClientIdsDroppedDuringLoad()).thenReturn(Set.of(CLIENT_ID));

        new ClientRevocationService(pairingService, sessionService, List.of(pushHandler))
                .completeInterruptedRevocations();

        verify(pushHandler).onClientRevoked(CLIENT_ID);
        verify(sessionService).removeSessionByClientId(CLIENT_ID);
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
    void accessIsWithdrawnBeforeAnythingThatCanFail() {
        // A subscription re-reading the grant while this runs must be refused, and a failure later
        // on must not leave the client with access.
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        doThrow(new RuntimeException("boom")).when(handler).onClientRevoked(CLIENT_ID);

        new ClientRevocationService(pairingService, mock(SessionService.class), List.of(handler))
                .revokeClient(CLIENT_ID);

        InOrder inOrder = inOrder(pairingService, handler);
        inOrder.verify(pairingService).revokePermissions(CLIENT_ID);
        inOrder.verify(handler).onClientRevoked(CLIENT_ID);
    }

    @Test
    void aFailedCleanupKeepsTheClientAddressableSoTheRetryReachesIt() {
        // The management ID resolves against the profile, so dropping it here would make the retry
        // this reports impossible and the failed cleanup would never run again.
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        doThrow(new RuntimeException("boom")).when(handler).onClientRevoked(CLIENT_ID);

        ClientRevocationResult result = new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of(handler)).revokeClient(CLIENT_ID);

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, result);
        verify(pairingService, never()).revokeClientProfile(CLIENT_ID);
    }

    @Test
    void aRetryAfterAFailedCleanupCompletesTheRevocation() {
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        doThrow(new RuntimeException("boom")).doNothing().when(handler).onClientRevoked(CLIENT_ID);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenReturn(true);
        ClientRevocationService service = new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of(handler));

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, service.revokeClient(CLIENT_ID));
        assertEquals(ClientRevocationResult.REVOKED, service.revokeClient(CLIENT_ID));

        verify(handler, times(2)).onClientRevoked(CLIENT_ID);
        verify(pairingService).revokeClientProfile(CLIENT_ID);
    }

    @Test
    void interruptedRevocationsAreFinishedForProfilesLeftWithoutPermissions() {
        // Push registrations survive a restart, so a cleanup that failed and was never retried
        // would otherwise keep feeding a revoked client for good.
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        ClientProfile interrupted = ClientProfile.fromSecret(CLIENT_ID, "secret", "Pixel 8");
        ClientProfile paired = ClientProfile.fromSecret("client-2", "other-secret", "iPhone");
        when(pairingService.getClientProfiles()).thenReturn(List.of(interrupted, paired));
        when(pairingService.hasPermissions(CLIENT_ID)).thenReturn(false);
        when(pairingService.hasPermissions("client-2")).thenReturn(true);

        new ClientRevocationService(pairingService, mock(SessionService.class), List.of(handler))
                .completeInterruptedRevocations();

        verify(handler).onClientRevoked(CLIENT_ID);
        verify(pairingService).revokeClientProfile(CLIENT_ID);
        verify(handler, never()).onClientRevoked("client-2");
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

    @Test
    void aFailedWriteOfThePermissionRemovalIsReportedAndKeepsTheClientForTheRetry() {
        // The grant is gone in memory but not on disk, so the next start would load it again: the
        // caller must not be told the client is revoked, and the profile must stay for the retry.
        PairingService pairingService = mock(PairingService.class);
        ClientRevocationHandler handler = mock(ClientRevocationHandler.class);
        SessionService sessionService = mock(SessionService.class);
        doThrow(new IllegalStateException("write failed")).when(pairingService).revokePermissions(CLIENT_ID);

        ClientRevocationResult result = new ClientRevocationService(pairingService,
                sessionService,
                List.of(handler)).revokeClient(CLIENT_ID);

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, result);
        verify(sessionService).removeSessionByClientId(CLIENT_ID);
        verify(handler).onClientRevoked(CLIENT_ID);
        verify(pairingService, never()).revokeClientProfile(CLIENT_ID);
    }

    @Test
    void aFailedWriteOfTheProfileRemovalIsReportedAsIncomplete() {
        PairingService pairingService = mock(PairingService.class);
        when(pairingService.revokeClientProfile(CLIENT_ID)).thenThrow(new IllegalStateException("write failed"));

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of()).revokeClient(CLIENT_ID));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aRevocationWhoseWritesFailIsIncompleteAndTheRetryFinishesIt() {
        // End to end on the real store: with every write failing, the disk keeps the profile and the
        // grant, so a client reported as revoked would get its access back on the next start.
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        ApiAccessStoreService store = new ApiAccessStoreService(persistenceService);
        PairingService pairingService = new PairingService(mock(ApiConfig.class),
                Path.of("unused"),
                store,
                new PermissionService(store));
        ClientRevocationService service = new ClientRevocationService(pairingService,
                mock(SessionService.class),
                List.of(mock(ClientRevocationHandler.class)));
        store.putClientProfileAndPermissions(CLIENT_ID,
                ClientProfile.fromSecret(CLIENT_ID, "secret", "Pixel 8"),
                PermissionSet.grantAll());
        when(persistence.persistAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("disk full")));

        assertEquals(ClientRevocationResult.CLEANUP_FAILED, service.revokeClient(CLIENT_ID));
        assertFalse(pairingService.hasPermissions(CLIENT_ID), "no access while this node runs");
        assertTrue(pairingService.findClientProfile(CLIENT_ID).isPresent(), "listed for the retry");

        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertEquals(ClientRevocationResult.REVOKED, service.revokeClient(CLIENT_ID));
        assertTrue(pairingService.findClientProfile(CLIENT_ID).isEmpty());
    }
}
