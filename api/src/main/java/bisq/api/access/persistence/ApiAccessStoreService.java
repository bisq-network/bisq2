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
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ApiAccessStoreService extends RateLimitedPersistenceClient<ApiAccessStore> {
    @Getter(onMethod_ = {@Override})
    private final ApiAccessStore persistableStore = new ApiAccessStore();
    @Getter(onMethod_ = {@Override})
    private final Persistence<ApiAccessStore> persistence;

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
     * Write grantAll promotions back to disk on the boot that computed them. Without this, a
     * node that never pairs a new client keeps the old explicit permission list on disk, and a
     * later version with additional permissions no longer recognises it as a full standard
     * grant — the client would silently fall back to a restricted set (see
     * {@code ApiAccessStore#promoteIfFullStandardGrant}).
     */
    @Override
    public void onPersistedApplied(ApiAccessStore persisted) {
        if (persisted.hadPromotedEntriesDuringLoad()) {
            log.info("Persisting grantAll promotions computed while loading the store");
            persist();
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

    public void putPermissions(String clientId, PermissionSet permissionSet) {
        persistableStore.getPermissionsByClientId().put(clientId, permissionSet);
        persist();
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
