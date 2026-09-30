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

import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ApiAccessStoreService serviceWith(Persistence persistence) {
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Stubbed so the RateLimitedPersistenceClient shutdown hook doesn't NPE on getStorePath().
        when(persistence.getStorePath()).thenReturn(Path.of("test-store"));
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
}
