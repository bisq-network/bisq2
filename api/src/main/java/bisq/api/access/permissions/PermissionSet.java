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

import java.util.Set;

// Just a wrapper for easier proto handling of a map with a hashset as value
public final class PermissionSet implements PersistableProto {
    @Getter
    private final Set<Permission> permissions;

    public PermissionSet(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public bisq.api.protobuf.PermissionSet toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.api.protobuf.PermissionSet.Builder getBuilder(boolean serializeForHash) {
        return bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(permissions.stream().map(Permission::toProtoEnum).toList());
    }

    public static PermissionSet fromProto(bisq.api.protobuf.PermissionSet proto) {
        return new PermissionSet(ProtobufUtils.fromProtoEnumSet(Permission.class, proto.getPermissionsList()));
    }
}
