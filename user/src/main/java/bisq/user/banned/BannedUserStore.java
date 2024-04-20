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

package bisq.user.banned;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class BannedUserStore implements PersistableStore<BannedUserStore> {
    private final ObservableSet<BannedUserProfileData> bannedUserProfileDataSet = new ObservableSet<>();

    public BannedUserStore() {
        this(new HashSet<>());
    }

    private BannedUserStore(Set<BannedUserProfileData> bannedUserProfileDataSet) {
        this.bannedUserProfileDataSet.addAll(bannedUserProfileDataSet);
    }

    @Override
    public bisq.user.protobuf.BannedUserStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.BannedUserStore.newBuilder()
                .addAllBannedUserProfileDataSet(bannedUserProfileDataSet.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.user.protobuf.BannedUserStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BannedUserStore fromProto(bisq.user.protobuf.BannedUserStore proto) {
        return new BannedUserStore(proto.getBannedUserProfileDataSetList().stream()
                .map(BannedUserProfileData::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.BannedUserStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BannedUserStore getClone() {
        return new BannedUserStore(new HashSet<>(bannedUserProfileDataSet));
    }

    @Override
    public void applyPersisted(BannedUserStore persisted) {
        bannedUserProfileDataSet.clear();
        bannedUserProfileDataSet.addAll(persisted.getBannedUserProfileDataSet());
    }

    ObservableSet<BannedUserProfileData> getBannedUserProfileDataSet() {
        return bannedUserProfileDataSet;
    }
}