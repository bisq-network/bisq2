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

package bisq.api.access.pairing;

import bisq.api.access.permissions.Permission;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
@EqualsAndHashCode
public final class PairingCode {
    public static final byte VERSION = 1;

    private final String id;
    private final Instant expiresAt;
    private final Set<Permission> grantedPermissions;

    public PairingCode(String id, Instant expiresAt, Set<Permission> grantedPermissions) {
        this.id = id;
        this.expiresAt = expiresAt;
        this.grantedPermissions = grantedPermissions;
    }
}
