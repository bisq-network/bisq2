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

package bisq.user.profile;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Persists my user profiles and the selected user profile.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public final class UserProfileStore implements PersistableStore<UserProfileStore> {
    // We do not prune the ignoredUserProfileIds when a user profile is expired/removed because
    // in case the user profile gets added again (become active again) we want to have it
    // remembered to be ignored.
    @Getter(AccessLevel.PACKAGE)
    private final ObservableSet<String> ignoredUserProfileIds = new ObservableSet<>();
    private final Object lock = new Object();

    private UserProfileStore(Set<String> ignoredUserProfileIds) {
        this.ignoredUserProfileIds.setAll(ignoredUserProfileIds);
    }

    @Override
    public bisq.user.protobuf.UserProfileStore.Builder getBuilder(boolean serializeForHash) {
        bisq.user.protobuf.UserProfileStore.Builder protoBuilder;
        synchronized (lock) {
            protoBuilder = bisq.user.protobuf.UserProfileStore.newBuilder()
                    .addAllIgnoredUserProfileIds(ignoredUserProfileIds)
                    .clearNymListByNickName() // We kept the protobuf field for backward compatibility, but we clear the field.
                    .clearUserProfileById(); // We kept the protobuf field for backward compatibility, but we clear the field.
        }
        return protoBuilder;
    }

    @Override
    public bisq.user.protobuf.UserProfileStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static UserProfileStore fromProto(bisq.user.protobuf.UserProfileStore proto) {
        Set<String> ignoredUserProfileIds = new HashSet<>(proto.getIgnoredUserProfileIdsList());
        return new UserProfileStore(ignoredUserProfileIds);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.UserProfileStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public UserProfileStore getClone() {
        UserProfileStore userProfileStore;
        synchronized (lock) {
            userProfileStore = new UserProfileStore(new HashSet<>(ignoredUserProfileIds));
        }
        return userProfileStore;
    }

    @Override
    public void applyPersisted(UserProfileStore persisted) {
        synchronized (lock) {
            ignoredUserProfileIds.setAll(persisted.getIgnoredUserProfileIds());
        }
    }

    public void addIgnoredUserProfileIds(String id) {
        ignoredUserProfileIds.add(id);
    }

    public void removeIgnoredUserProfileIds(String id) {
        ignoredUserProfileIds.remove(id);
    }
}