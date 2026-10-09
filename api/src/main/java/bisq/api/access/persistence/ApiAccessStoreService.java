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
import bisq.api.access.permissions.PermissionSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.persistence.backup.BackupFileInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ApiAccessStoreService implements PersistenceClient<ApiAccessStore> {
    @Getter(onMethod_ = {@Override})
    private final ApiAccessStore persistableStore = new ApiAccessStore();
    @Getter(onMethod_ = {@Override})
    private final Persistence<ApiAccessStore> persistence;
    /** See {@link ApiAccessStore#getClientIdsDroppedDuringLoad()}. Empty before the store is read. */
    @Getter
    private volatile Set<String> clientIdsDroppedDuringLoad = Set.of();

    public ApiAccessStoreService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    public Map<String, ClientProfile> getClientProfileByIdMap() {
        return Map.copyOf(persistableStore.getClientProfileByIdMap());
    }

    public Map<String, PermissionSet> getPermissionsByClientId() {
        return Map.copyOf(persistableStore.getPermissionsByClientId());
    }

    /**
     * Write what the load rewrote back to disk on the same boot. For grantAll promotions: without
     * this, a node that never pairs a new client keeps the old explicit permission list on disk,
     * and a later version with additional permissions no longer recognises it as a full standard
     * grant — the client would silently fall back to a restricted set (see
     * {@code ApiAccessStore#promoteIfFullStandardGrant}). For hashed secrets: the plaintext would
     * otherwise stay on disk until the next pairing or revocation, and its backups have to go too;
     * see {@link #persistWithoutPlaintextSecrets()}.
     * <p>
     * Relies on the transport starting only after all stores are read: the load replaces the live
     * maps without the monitor the write paths take, so a pairing landing during it could be lost.
     */
    @Override
    public void onPersistedApplied(ApiAccessStore persisted) {
        clientIdsDroppedDuringLoad = persisted.getClientIdsDroppedDuringLoad();
        if (!persisted.needsWriteBackAfterLoad()) {
            return;
        }
        log.info("Persisting store rewrites computed while loading (promotions: {}, plaintext secrets: {}, dropped clients: {})",
                persisted.hadPromotedEntriesDuringLoad(),
                persisted.hadPlaintextSecretsDuringLoad(),
                persisted.getClientIdsDroppedDuringLoad().size());
        if (persisted.hadPlaintextSecretsDuringLoad()) {
            persistWithoutPlaintextSecrets();
        } else {
            persist();
        }
    }

    /**
     * Hashing the secrets is only complete once the backups are gone too: every write moves the
     * previous store file into the backups, which are kept for up to a year, so the plaintext would
     * outlive the migration there. The order is load-bearing: the backups are deleted only once
     * the hashed store has been written, they are the recovery copies until then, and one more
     * write then seeds a hashed backup. A failed write keeps them and the plaintext is found again
     * next boot. Awaited, because the transport starts after this and a pairing written in between
     * would be backed up and deleted with the rest.
     */
    private void persistWithoutPlaintextSecrets() {
        persist()
                .thenCompose(this::deletePlaintextBackupsIfWritten)
                .exceptionally(throwable -> {
                    log.error("Rewriting the store without plaintext client secrets did not complete", throwable);
                    return false;
                })
                .join();
    }

    private CompletableFuture<Boolean> deletePlaintextBackupsIfWritten(boolean written) {
        if (!written) {
            // The backups are the only recovery copies while the store on disk is not yet
            // rewritten, so they are kept; the plaintext is retried next boot.
            log.error("Writing the store without plaintext client secrets failed, keeping the backups");
            return CompletableFuture.completedFuture(false);
        }
        log.info("Deleting backups of the store, they still hold plaintext client secrets");
        return persistence.deleteBackups()
                .thenCompose(nil -> {
                    logBackupsThatSurvivedDeletion();
                    // Seeds a backup of the hashed store.
                    return persist();
                });
    }

    /**
     * Loud rather than silent: the store no longer holds a plaintext after this boot, so nothing
     * would try the deletion again.
     */
    private void logBackupsThatSurvivedDeletion() {
        List<Path> remaining = persistence.getBackups().stream().map(BackupFileInfo::getPath).toList();
        if (!remaining.isEmpty()) {
            log.error("Backups still holding plaintext client secrets could not be deleted, " +
                    "remove them manually: {}", remaining);
        }
    }

    /**
     * Stores a client's profile and its permissions as one step, persisted once.
     * <p>
     * Written under the same monitor as {@link #removeClientProfile(String)} because the two are
     * otherwise interleavable: a revocation landing between separate writes removes a profile and a
     * grant that does not exist yet, and the grant is then written afterwards. That orphan grant is
     * not inert — the authorization filter reads permissions, not profiles, so with session
     * handling off (as every shipped config runs) it is by itself enough to authorize the client
     * that was just revoked.
     */
    public void putClientProfileAndPermissions(String clientId,
                                               ClientProfile clientProfile,
                                               PermissionSet permissionSet) {
        synchronized (persistableStore) {
            persistableStore.getClientProfileByIdMap().put(clientId, clientProfile);
            persistableStore.getPermissionsByClientId().put(clientId, permissionSet);
            persist();
        }
    }

    /**
     * Removes only the permissions, leaving the profile. Used to end a client's access at the start
     * of a revocation, before the steps that can fail.
     */
    public void removePermissions(String clientId) {
        synchronized (persistableStore) {
            if (persistableStore.getPermissionsByClientId().remove(clientId) != null) {
                persist();
            }
        }
    }

    /**
     * Removes the client profile and associated permissions for the given client ID.
     * Both removals are applied atomically under a lock and persisted in a single
     * {@link #persist()} call.
     *
     * @param clientId The client ID to remove
     * @return {@code true} if a profile was present and removed; {@code false} if the client was not found
     */
    public boolean removeClientProfile(String clientId) {
        synchronized (persistableStore) {
            boolean removed = persistableStore.getClientProfileByIdMap().remove(clientId) != null;
            persistableStore.getPermissionsByClientId().remove(clientId);
            persist();
            return removed;
        }
    }
}
