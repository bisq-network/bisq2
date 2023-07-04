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

package bisq.user.role;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public final class RoleRegistrationServiceStore implements PersistableStore<RoleRegistrationServiceStore> {
    private final ObservableSet<AuthorizedRoleRegistrationData> myRoleRegistrations = new ObservableSet<>();

    RoleRegistrationServiceStore() {
    }

    private RoleRegistrationServiceStore(Set<AuthorizedRoleRegistrationData> myRoleRegistrations) {
        this.myRoleRegistrations.setAll(myRoleRegistrations);
    }

    @Override
    public bisq.user.protobuf.RoleRegistrationServiceStore toProto() {
        return bisq.user.protobuf.RoleRegistrationServiceStore.newBuilder()
                .addAllMyRoleRegistrations(myRoleRegistrations.stream()
                        .map(AuthorizedRoleRegistrationData::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static RoleRegistrationServiceStore fromProto(bisq.user.protobuf.RoleRegistrationServiceStore proto) {
        return new RoleRegistrationServiceStore(proto.getMyRoleRegistrationsList().stream()
                .map(AuthorizedRoleRegistrationData::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.RoleRegistrationServiceStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public RoleRegistrationServiceStore getClone() {
        return new RoleRegistrationServiceStore(myRoleRegistrations);
    }

    @Override
    public void applyPersisted(RoleRegistrationServiceStore persisted) {
        myRoleRegistrations.setAll(persisted.getMyRoleRegistrations());
    }
}