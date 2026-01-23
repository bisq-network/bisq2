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

package bisq.api.access.persistence;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
final class ApiAccessStore implements PersistableStore<ApiAccessStore> {
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, ClientProfile> clientProfileByIdMap = new ConcurrentHashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, Set<Permission>> permissionsByClientId = new ConcurrentHashMap<>();

    ApiAccessStore() {
        this(new HashMap<>(), new HashMap<>());
    }

    private ApiAccessStore(Map<String, ClientProfile> clientProfileByIdMap,
                           Map<String, Set<Permission>> permissionsByClientId) {
        this.clientProfileByIdMap.putAll(clientProfileByIdMap);
        this.permissionsByClientId.putAll(permissionsByClientId);
    }

    @Override
    public bisq.api.protobuf.ApiAccessStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putAllClientProfileByIdMap(clientProfileByIdMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))))
                .putAllPermissionsByClientId(permissionsByClientId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> new PermissionSet(e.getValue()).toProto(serializeForHash))));
    }

    @Override
    public bisq.api.protobuf.ApiAccessStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ApiAccessStore fromProto(bisq.api.protobuf.ApiAccessStore proto) {
        Map<String, ClientProfile> clientProfileByIdMap = proto.getClientProfileByIdMapMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ClientProfile.fromProto(e.getValue())));
        Map<String, Set<Permission>> permissionsByClientId = proto.getPermissionsByClientIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> PermissionSet.fromProto(e.getValue()).getPermissions()));
        return new ApiAccessStore(clientProfileByIdMap,
                permissionsByClientId);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.api.protobuf.ApiAccessStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ApiAccessStore getClone() {
        return new ApiAccessStore(Map.copyOf(clientProfileByIdMap),
                Map.copyOf(permissionsByClientId)
        );
    }

    @Override
    public void applyPersisted(ApiAccessStore persisted) {
        clientProfileByIdMap.putAll(persisted.getClientProfileByIdMap());
        permissionsByClientId.putAll(persisted.getPermissionsByClientId());
    }
}