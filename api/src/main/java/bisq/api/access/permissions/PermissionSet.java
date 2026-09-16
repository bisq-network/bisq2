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

import bisq.common.proto.PersistableProto;
import bisq.common.proto.ProtobufUtils;
import lombok.Getter;

import java.util.Comparator;
import java.util.Set;

/**
 * Wrapper for easier proto handling of a map with a hashset as value.
 * <p>
 * A grant is either explicit (a fixed set of permissions) or {@code grantAll}. A grantAll
 * grant expands to the running version's AUTO-GRANTABLE permissions at read time (see
 * {@link Permission#autoGrantable()}), so clients paired before a node upgrade automatically
 * cover standard permissions added later — but never security-sensitive ones, which always
 * require an explicit per-device grant. Persisting the pairing-time expansion instead would
 * silently lock paired clients out of endpoints guarded by permissions added after pairing.
 */
public final class PermissionSet implements PersistableProto {
    // grantAll expands to the auto-grantable ("standard") permissions only — NEVER to sensitive
    // ones (Permission.autoGrantable = false), which always require an explicit per-device grant.
    // Today every permission is auto-grantable, so this equals the full enum; that changes the
    // day the first sensitive permission is declared, with no code change needed here.
    private static final Set<Permission> AUTO_GRANTABLE_PERMISSIONS = Permission.autoGrantable();

    private final Set<Permission> explicitPermissions;
    @Getter
    private final boolean grantAll;

    public PermissionSet(Set<Permission> permissions) {
        this(permissions, false);
    }

    private PermissionSet(Set<Permission> explicitPermissions, boolean grantAll) {
        this.explicitPermissions = Set.copyOf(explicitPermissions);
        this.grantAll = grantAll;
    }

    public static PermissionSet grantAll() {
        return new PermissionSet(Set.of(), true);
    }

    public Set<Permission> getPermissions() {
        return grantAll ? AUTO_GRANTABLE_PERMISSIONS : explicitPermissions;
    }

    @Override
    public bisq.api.protobuf.PermissionSet toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.api.protobuf.PermissionSet.Builder getBuilder(boolean serializeForHash) {
        // For grantAll we serialize the current expansion as well, so a node downgraded to a
        // version without the grantAll field still reads a full grant from the explicit list.
        // Sorted by id: set iteration order is unspecified, and an order-unstable repeated
        // field would break serializeForHash determinism.
        return bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(getPermissions().stream()
                        .sorted(Comparator.comparingInt(Permission::getId))
                        .map(Permission::toProtoEnum)
                        .toList())
                .setGrantAll(grantAll);
    }

    public static PermissionSet fromProto(bisq.api.protobuf.PermissionSet proto) {
        if (proto.getGrantAll()) {
            return grantAll();
        }
        return new PermissionSet(ProtobufUtils.fromProtoEnumSet(Permission.class, proto.getPermissionsList()));
    }
}
