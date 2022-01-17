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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityModel implements Persistable<IdentityModel> {
    @Getter
    private final Map<String, Identity> identityByDomainId = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> userNameByDomainId = new HashMap<>();

    public IdentityModel() {
    }

    private IdentityModel(Map<String, Identity> identityByDomainId, Map<String, String> userNameByDomainId) {
        this.identityByDomainId.putAll(identityByDomainId);
        this.userNameByDomainId.putAll(userNameByDomainId);
    }

    @Override
    public IdentityModel getClone() {
        return new IdentityModel(identityByDomainId, userNameByDomainId);
    }

    @Override
    public void applyPersisted(IdentityModel persisted) {
        identityByDomainId.clear();
        identityByDomainId.putAll(persisted.getIdentityByDomainId());

        userNameByDomainId.clear();
        userNameByDomainId.putAll(persisted.getUserNameByDomainId());
    }
}