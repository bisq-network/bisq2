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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
public final class UserIdentityStore implements PersistableStore<UserIdentityStore> {
    @Nullable
    private UserIdentity selectedUserIdentity;
    private final Set<UserIdentity> userIdentities;

    public UserIdentityStore() {
        userIdentities = new HashSet<>();
    }

    private UserIdentityStore(@Nullable UserIdentity selectedUserIdentity,
                              Set<UserIdentity> userIdentities) {
        this.userIdentities = new HashSet<>(userIdentities);
        setSelectedUserIdentity(selectedUserIdentity);
    }

    @Nullable
    public UserIdentity getSelectedUserIdentity() {
        return selectedUserIdentity;
    }

    public void setSelectedUserIdentity(@Nullable UserIdentity selectedUserIdentity) {
        this.selectedUserIdentity = userIdentities.stream()
                .filter(userIdentity -> userIdentity.equals(selectedUserIdentity))
                .findAny()
                .orElse(null);
    }

    Set<UserIdentity> getUserIdentities() {
        return userIdentities;
    }

    @Override
    public bisq.user.protobuf.UserIdentityStore toProto() {
        bisq.user.protobuf.UserIdentityStore.Builder builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()));
        Optional.ofNullable(selectedUserIdentity).ifPresent(selectedUserIdentity
                -> builder.setSelectedUserIdentity(selectedUserIdentity.toProto()));
        return builder.build();
    }

    public static UserIdentityStore fromProto(bisq.user.protobuf.UserIdentityStore proto) {
        return new UserIdentityStore(proto.hasSelectedUserIdentity() ?
                UserIdentity.fromProto(proto.getSelectedUserIdentity()) :
                null,
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
        return new UserIdentityStore(selectedUserIdentity, userIdentities);
    }

    @Override
    public void applyPersisted(UserIdentityStore persisted) {
        userIdentities.addAll(persisted.getUserIdentities());
        setSelectedUserIdentity(persisted.getSelectedUserIdentity());
    }
}