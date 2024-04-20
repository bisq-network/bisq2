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
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
@Getter
public final class UserProfileStore implements PersistableStore<UserProfileStore> {
    private final Map<String, Set<String>> nymsByNickName = new ConcurrentHashMap<>();
    private final ObservableSet<String> ignoredUserProfileIds = new ObservableSet<>();
    private final ObservableHashMap<String, UserProfile> userProfileById = new ObservableHashMap<>();
    private final Object lock = new Object();

    public UserProfileStore() {
    }

    private UserProfileStore(Map<String, Set<String>> nymsByNickName,
                             Set<String> ignoredUserProfileIds,
                             Map<String, UserProfile> userProfileById) {
        this.nymsByNickName.putAll(nymsByNickName);
        this.ignoredUserProfileIds.clear();
        this.ignoredUserProfileIds.addAll(ignoredUserProfileIds);
        this.userProfileById.putAll(userProfileById);
    }

    @Override
    public bisq.user.protobuf.UserProfileStore.Builder getBuilder(boolean serializeForHash) {
        bisq.user.protobuf.UserProfileStore.Builder protoBuilder;
        synchronized (lock) {
            protoBuilder = bisq.user.protobuf.UserProfileStore.newBuilder()
                    .putAllNymListByNickName(nymsByNickName.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> bisq.user.protobuf.NymList.newBuilder()
                                            .addAllNyms(entry.getValue()).build())))
                    .addAllIgnoredUserProfileIds(ignoredUserProfileIds)
                    .putAllUserProfileById(userProfileById.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> entry.getValue().toProto(serializeForHash))));
        }
        return protoBuilder;
    }

    @Override
    public bisq.user.protobuf.UserProfileStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static UserProfileStore fromProto(bisq.user.protobuf.UserProfileStore proto) {
        Map<String, Set<String>> nymsByNickName = proto.getNymListByNickNameMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new HashSet<>(entry.getValue().getNymsList())));
        Set<String> ignoredUserProfileIds = new HashSet<>(proto.getIgnoredUserProfileIdsList());
        Map<String, UserProfile> userProfileById = proto.getUserProfileByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> UserProfile.fromProto(entry.getValue())));
        return new UserProfileStore(nymsByNickName, ignoredUserProfileIds, userProfileById);
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
            userProfileStore = new UserProfileStore(new HashMap<>(nymsByNickName), new HashSet<>(ignoredUserProfileIds), new HashMap<>(userProfileById));
        }
        return userProfileStore;
    }

    @Override
    public void applyPersisted(UserProfileStore persisted) {
        synchronized (lock) {
            nymsByNickName.putAll(persisted.getNymsByNickName());
            ignoredUserProfileIds.clear();
            ignoredUserProfileIds.addAll(persisted.getIgnoredUserProfileIds());
            userProfileById.putAll(persisted.getUserProfileById());
        }
    }
}