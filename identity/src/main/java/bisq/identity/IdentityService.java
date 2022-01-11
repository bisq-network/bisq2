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

package bisq.identity;


import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IdentityService implements PersistenceClient<HashMap<String, Identity>> {
    public final static String DEFAULT = "default";

    @Getter
    private final Persistence<HashMap<String, Identity>> persistence;
    private final Map<String, Identity> identityByDomainId = new ConcurrentHashMap<>();

    public IdentityService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, "db", "identities");
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void applyPersisted(HashMap<String, Identity> persisted) {
        synchronized (identityByDomainId) {
            identityByDomainId.putAll(persisted);
        }
    }

    @Override
    public HashMap<String, Identity> getClone() {
        synchronized (identityByDomainId) {
            return new HashMap<>(identityByDomainId);
        }
    }

    public void shutdown() {
    }

    public Identity getOrCreateDefaultIdentity() {
        return getOrCreateIdentity(DEFAULT);
    }

    public Identity getOrCreateIdentity(String domainId) {
        synchronized (identityByDomainId) {
            if (identityByDomainId.containsKey(domainId)) {
                return identityByDomainId.get(domainId);
            }
        }
        Identity identity;
        String keyId = UUID.randomUUID().toString().substring(0, 8);
        String nodeId = UUID.randomUUID().toString().substring(0, 8);
        identity = new Identity(domainId, nodeId, keyId);
        synchronized (identityByDomainId) {
            identityByDomainId.put(domainId, identity);
        }
        persist();
        return identity;
    }

    public Optional<Identity> findIdentityByNodeId(String nodeId) {
        synchronized (identityByDomainId) {
            return identityByDomainId.values().stream()
                    .filter(identity -> identity.nodeId().equals(nodeId))
                    .findAny();
        }
    }
}
