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

import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class IdentityStore implements PersistableStore<IdentityStore> {
    @Getter
    private final Set<Identity> pool = new CopyOnWriteArraySet<>();
    @Getter
    private final Map<String, Identity> activeIdentityByDomainId = new ConcurrentHashMap<>();
    @Getter
    private final Set<Identity> retired = new CopyOnWriteArraySet<>();

    public IdentityStore() {
    }

    private IdentityStore(Map<String, Identity> activeIdentityByDomainId,
                          Set<Identity> pool,
                          Set<Identity> retired) {
        this.activeIdentityByDomainId.putAll(activeIdentityByDomainId);
        this.pool.addAll(pool);
        this.retired.addAll(retired);
    }

    @Override
    public IdentityStore getClone() {
        return new IdentityStore(activeIdentityByDomainId, pool, retired);
    }

    @Override
    public void applyPersisted(IdentityStore persisted) {
        activeIdentityByDomainId.clear();
        activeIdentityByDomainId.putAll(persisted.getActiveIdentityByDomainId());

        pool.clear();
        pool.addAll(persisted.getPool());

        retired.clear();
        retired.addAll(persisted.getRetired());
    }

    @Override
    public Message toProto() {
        log.error("Not impl yet");
        return null;
    }
}