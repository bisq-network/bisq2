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

package bisq.api.access.permissions;

import bisq.api.access.persistence.ApiAccessStoreService;
import lombok.Getter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class PermissionService<T extends PermissionMapping> {
    private final ApiAccessStoreService apiAccessStoreService;
    @Getter
    private final T permissionMapping;

    public PermissionService(ApiAccessStoreService apiAccessStoreService, T permissionMapping) {
        this.apiAccessStoreService = apiAccessStoreService;
        this.permissionMapping = permissionMapping;
    }

    public boolean hasPermission(Set<Permission> granted, Permission required) {
        return granted.contains(required);
    }

    public void putPermissions(String clientId, Set<Permission> permissions) {
        apiAccessStoreService.putPermissions(clientId, permissions);
    }

    public Optional<Set<Permission>> findPermissions(String clientId) {
        return Optional.ofNullable(apiAccessStoreService.getPermissionsByClientId().get(clientId))
                .map(Collections::unmodifiableSet);
    }
}
