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

package bisq.api.access.persistence;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the persist-after-promotion write-back: a grantAll promotion computed while loading
 * the store must reach disk on the same boot. Without it, a node that never pairs a new client
 * keeps the old explicit permission list on disk, and a later version with more permissions no
 * longer recognises the entry as a full standard grant — silently re-restricting the client
 * (the exact v1 -> v2 -> v3 rollout gap from the PR review).
 */
class ApiAccessStoreServiceTest {
    private static final String CLIENT_ID = "client-1";


    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ApiAccessStoreService serviceWith(Persistence persistence) {
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.deleteBackups()).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getBackups()).thenReturn(List.of());
        return new ApiAccessStoreService(persistenceService);
    }

    private static bisq.api.protobuf.ApiAccessStore legacyStoreWithFullStandardGrant(String clientId) {
        // Legacy-shaped entry: an explicit list equal to the full standard set, no grantAll flag.
        bisq.api.protobuf.PermissionSet legacyFullGrant = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(Permission.autoGrantable().stream().map(Permission::toProtoEnum).toList())
                .build();
        return bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putPermissionsByClientId(clientId, legacyFullGrant)
                .build();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedPromotesAndWritesGrantAllBackSoItSurvivesRestart() {
        Persistence persistence = mock(Persistence.class);
        // read() deserializes (and promotes) the legacy store, exactly as production does.
        when(persistence.read()).thenReturn(Optional.of(
                ApiAccessStore.fromProto(legacyStoreWithFullStandardGrant("legacy-client"))));
        ApiAccessStoreService service = serviceWith(persistence);

        // Drives the real chain: read() -> applyPersisted() -> onPersistedApplied() -> persist().
        service.readPersisted();

        // Capture what actually reaches disk and prove the promotion is in it — not just that
        // persistAsync was called.
        ArgumentCaptor<ApiAccessStore> captor = ArgumentCaptor.forClass(ApiAccessStore.class);
        verify(persistence).persistAsync(captor.capture());
        PermissionSet written = captor.getValue().getPermissionsByClientId().get("legacy-client");
        assertTrue(written.isGrantAll(), "the written store must carry the promoted grantAll entry");

        // Round-trip the written bytes to prove grantAll survives a subsequent restart's read.
        ApiAccessStore reloaded = ApiAccessStore.fromProto(captor.getValue().toProto(false));
        assertTrue(reloaded.getPermissionsByClientId().get("legacy-client").isGrantAll(),
                "grantAll must survive a restart round-trip of the written store");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedWritesHashedSecretsBackSoThePlaintextLeavesTheDisk() {
        Persistence persistence = mock(Persistence.class);
        bisq.api.protobuf.ApiAccessStore legacy = bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putClientProfileByIdMap(CLIENT_ID, bisq.api.protobuf.ClientProfile.newBuilder()
                        .setClientId(CLIENT_ID)
                        .setClientSecret("secret")
                        .setClientName("Pixel 8")
                        .build())
                .putPermissionsByClientId(CLIENT_ID, PermissionSet.grantAll().toProto(false))
                .setPermissionsSchemaVersion(1)
                .build();
        when(persistence.read()).thenReturn(Optional.of(ApiAccessStore.fromProto(legacy)));
        ApiAccessStoreService service = serviceWith(persistence);

        service.readPersisted();

        // Written, backups deleted, written again so a hashed backup exists: every write moves the
        // previous file into the backups, so the first write alone would leave the plaintext there.
        ArgumentCaptor<ApiAccessStore> captor = ArgumentCaptor.forClass(ApiAccessStore.class);
        InOrder inOrder = inOrder(persistence);
        inOrder.verify(persistence).persistAsync(captor.capture());
        inOrder.verify(persistence).deleteBackups();
        inOrder.verify(persistence).persistAsync(any());
        bisq.api.protobuf.ClientProfile written = captor.getValue().toProto(false)
                .getClientProfileByIdMapMap().get(CLIENT_ID);
        assertTrue(written.getClientSecret().isEmpty(), "the plaintext must not reach the disk again");
        assertTrue(ClientProfile.fromProto(written).matchesSecret("secret"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedKeepsBackupsWhenTheHashedWriteFails() {
        // The backups hold the plaintext, but they are also the only recovery copies until the
        // hashed store is on disk: a failed write must not sweep them.
        Persistence persistence = mock(Persistence.class);
        bisq.api.protobuf.ApiAccessStore legacy = bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putClientProfileByIdMap(CLIENT_ID, bisq.api.protobuf.ClientProfile.newBuilder()
                        .setClientId(CLIENT_ID)
                        .setClientSecret("secret")
                        .setClientName("Pixel 8")
                        .build())
                .putPermissionsByClientId(CLIENT_ID, PermissionSet.grantAll().toProto(false))
                .setPermissionsSchemaVersion(1)
                .build();
        when(persistence.read()).thenReturn(Optional.of(ApiAccessStore.fromProto(legacy)));
        ApiAccessStoreService service = serviceWith(persistence);
        when(persistence.persistAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("disk full")));

        service.readPersisted();

        verify(persistence, times(1)).persistAsync(any());
        verify(persistence, never()).deleteBackups();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedKeepsBackupsWhenNoPlaintextWasHashed() {
        // A grantAll promotion or a dropped dead entry rewrites the store, but the backups never
        // held a plaintext, so they stay as the corruption safety net they are.
        Persistence persistence = mock(Persistence.class);
        when(persistence.read()).thenReturn(Optional.of(
                ApiAccessStore.fromProto(legacyStoreWithFullStandardGrant("legacy-client"))));
        ApiAccessStoreService service = serviceWith(persistence);

        service.readPersisted();

        verify(persistence, times(1)).persistAsync(any());
        verify(persistence, never()).deleteBackups();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedExposesTheClientsTheLoadDropped() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.read()).thenReturn(Optional.of(ApiAccessStore.fromProto(bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putClientProfileByIdMap("dead-client", bisq.api.protobuf.ClientProfile.newBuilder()
                        .setClientId("dead-client")
                        .setClientName("Pixel 8")
                        .build())
                .putPermissionsByClientId("dead-client", PermissionSet.grantAll().toProto(false))
                .build())));
        ApiAccessStoreService service = serviceWith(persistence);

        service.readPersisted();

        assertEquals(Set.of("dead-client"), service.getClientIdsDroppedDuringLoad());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readPersistedDoesNotWriteBackWhenNothingWasPromoted() {
        Persistence persistence = mock(Persistence.class);
        // Already grantAll on disk — nothing promoted, nothing to write back.
        ApiAccessStore alreadyGrantAll = ApiAccessStore.fromProto(bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putPermissionsByClientId("client-1", PermissionSet.grantAll().toProto(false))
                .setPermissionsSchemaVersion(1)
                .build());
        when(persistence.read()).thenReturn(Optional.of(alreadyGrantAll));
        ApiAccessStoreService service = serviceWith(persistence);

        service.readPersisted();

        verify(persistence, never()).persistAsync(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void removingPermissionsEndsAccessWhileTheClientStaysAddressable() {
        // What the revocation ordering rests on: the grant answers whether a client still has
        // access, and the profile only keeps it addressable until the cleanup has succeeded.
        Persistence persistence = mock(Persistence.class);
        ApiAccessStoreService service = serviceWith(persistence);
        service.putClientProfileAndPermissions(CLIENT_ID,
                ClientProfile.fromSecret(CLIENT_ID, "secret", "Pixel 8"),
                PermissionSet.grantAll());

        service.removePermissions(CLIENT_ID);

        assertFalse(service.getPermissionsByClientId().containsKey(CLIENT_ID));
        assertTrue(service.getClientProfileByIdMap().containsKey(CLIENT_ID));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void removingTheProfileEndsTheRevocation() {
        Persistence persistence = mock(Persistence.class);
        ApiAccessStoreService service = serviceWith(persistence);
        service.putClientProfileAndPermissions(CLIENT_ID,
                ClientProfile.fromSecret(CLIENT_ID, "secret", "Pixel 8"),
                PermissionSet.grantAll());
        service.removePermissions(CLIENT_ID);

        assertTrue(service.removeClientProfile(CLIENT_ID));
        assertFalse(service.getClientProfileByIdMap().containsKey(CLIENT_ID));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bothWritesOfARevocationReachPersistence() {
        // A revocation writes twice in quick succession. Under a write rate limit the second is
        // dropped and lives in memory only, so a hard kill brings the client back.
        Persistence persistence = mock(Persistence.class);
        ApiAccessStoreService service = serviceWith(persistence);
        service.putClientProfileAndPermissions(CLIENT_ID,
                ClientProfile.fromSecret(CLIENT_ID, "secret", "Pixel 8"),
                PermissionSet.grantAll());

        service.removePermissions(CLIENT_ID);
        service.removeClientProfile(CLIENT_ID);

        verify(persistence, times(3)).persistAsync(any());
    }
}
