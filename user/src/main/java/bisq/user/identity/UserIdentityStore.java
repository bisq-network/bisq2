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

package bisq.user.identity;

import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
@Getter
public final class UserIdentityStore implements PersistableStore<UserIdentityStore> {
    private final Observable<UserIdentity> selectedUserProfile = new Observable<>();
    private final ObservableSet<UserIdentity> userIdentities;

    public UserIdentityStore() {
        userIdentities = new ObservableSet<>();
    }

    private UserIdentityStore(UserIdentity selectedUserIdentity,
                              Set<UserIdentity> userIdentities) {
        this.selectedUserProfile.set(selectedUserIdentity);
        this.userIdentities = new ObservableSet<>(userIdentities);
    }

    @Override
    public bisq.user.protobuf.UserIdentityStore toProto() {
        return bisq.user.protobuf.UserIdentityStore.newBuilder()
                .setSelectedUserIdentity(selectedUserProfile.get().toProto())
                .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()))
                .build();
    }

    public static UserIdentityStore fromProto(bisq.user.protobuf.UserIdentityStore proto) {
        return new UserIdentityStore(UserIdentity.fromProto(proto.getSelectedUserIdentity()),
                proto.getUserIdentitiesList().stream()
                        .map(UserIdentity::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.UserIdentityStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public UserIdentityStore getClone() {
        return new UserIdentityStore(selectedUserProfile.get(), userIdentities);
    }

    @Override
    public void applyPersisted(UserIdentityStore persisted) {
        selectedUserProfile.set(persisted.getSelectedUserProfile().get());
        userIdentities.addAll(persisted.getUserIdentities());
    }
}