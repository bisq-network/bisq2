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
import bisq.common.proto.PersistableProto;
import bisq.common.proto.ProtobufUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Getter
@EqualsAndHashCode
public final class PairingCode implements PersistableProto {
    public static final byte VERSION = 1;

    private final String id;
    private final Instant expiresAt;
    private final Set<Permission> grantedPermissions;

    public PairingCode(String id, Instant expiresAt, Set<Permission> grantedPermissions) {
        this.id = Objects.requireNonNull(id, "id");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.grantedPermissions = Set.copyOf(Objects.requireNonNull(grantedPermissions, "grantedPermissions"));
    }

    @Override
    public bisq.api.protobuf.PairingCode toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.api.protobuf.PairingCode.Builder getBuilder(boolean serializeForHash) {
        return bisq.api.protobuf.PairingCode.newBuilder()
                .setId(id)
                .setExpiresAt(expiresAt.toEpochMilli())
                .addAllGrantedPermissions(grantedPermissions.stream().map(Permission::toProtoEnum).toList());
    }

    public static PairingCode fromProto(bisq.api.protobuf.PairingCode proto) {
        return new PairingCode(proto.getId(),
                Instant.ofEpochMilli(proto.getExpiresAt()),
                ProtobufUtils.fromProtoEnumSet(Permission.class, proto.getGrantedPermissionsList()));
    }
}
