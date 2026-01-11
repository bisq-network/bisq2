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

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionService<T extends PermissionMapping> {
    @Getter
    private final T permissionMapping;
    private final Map<UUID, Set<Permission>> permissionsByDeviceId = new ConcurrentHashMap<>();

    public PermissionService(T permissionMapping) {
        this.permissionMapping = permissionMapping;
    }

    public boolean hasPermission(Set<Permission> granted, Permission required) {
        return granted.contains(required);
    }

    public void setDevicePermissions(UUID deviceId, Set<Permission> permissions) {
        permissionsByDeviceId.put(deviceId, permissions);
    }

    public Optional<Set<Permission>> findPermissions(UUID deviceId) {
        return Optional.ofNullable(permissionsByDeviceId.get(deviceId))
                .map(Collections::unmodifiableSet);
    }
}
