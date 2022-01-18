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

import bisq.persistence.Persistable;
import lombok.Getter;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

public class IdentityModel implements Persistable<IdentityModel> {
    @Getter
    private final Queue<Identity> pool = new ConcurrentLinkedQueue<>();
    @Getter
    private final Map<String, Identity> activeIdentityByDomainId = new ConcurrentHashMap<>();
    @Getter
    private final Set<Identity> retired = new CopyOnWriteArraySet<>();
    @Getter
    private final Map<String, String> userNameByDomainId = new ConcurrentHashMap<>();

    public IdentityModel() {
    }

    private IdentityModel(Map<String, Identity> activeIdentityByDomainId,
                          Map<String, String> userNameByDomainId,
                          Queue<Identity> pool,
                          Set<Identity> retired) {
        this.activeIdentityByDomainId.putAll(activeIdentityByDomainId);
        this.userNameByDomainId.putAll(userNameByDomainId);
        this.pool.addAll(pool);
        this.retired.addAll(retired);
    }

    @Override
    public IdentityModel getClone() {
        return new IdentityModel(activeIdentityByDomainId, userNameByDomainId, pool, retired);
    }

    @Override
    public void applyPersisted(IdentityModel persisted) {
        activeIdentityByDomainId.clear();
        activeIdentityByDomainId.putAll(persisted.getActiveIdentityByDomainId());

        userNameByDomainId.clear();
        userNameByDomainId.putAll(persisted.getUserNameByDomainId());

        pool.clear();
        pool.addAll(persisted.getPool());

        retired.clear();
        retired.addAll(persisted.getRetired());
    }
}