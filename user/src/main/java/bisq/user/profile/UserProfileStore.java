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
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
@Getter
public final class UserProfileStore implements PersistableStore<UserProfileStore> {

    private final Map<String, Set<String>> nymsByNickName = new HashMap<>();

    public UserProfileStore() {
    }

    private UserProfileStore(Map<String, Set<String>> nymsByNickName) {
        this.nymsByNickName.putAll(nymsByNickName);
    }

    @Override
    public bisq.user.protobuf.UserProfileStore toProto() {
        return bisq.user.protobuf.UserProfileStore.newBuilder()
                .putAllNymListByNickName(nymsByNickName.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> bisq.user.protobuf.NymList.newBuilder()
                                        .addAllNyms(entry.getValue()).build())))
                .build();
    }

    public static UserProfileStore fromProto(bisq.user.protobuf.UserProfileStore proto) {
        return new UserProfileStore(
                proto.getNymListByNickNameMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> new HashSet<>(entry.getValue().getNymsList()))));
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
        return new UserProfileStore(nymsByNickName);
    }

    @Override
    public void applyPersisted(UserProfileStore persisted) {
        nymsByNickName.putAll(persisted.getNymsByNickName());
    }
}