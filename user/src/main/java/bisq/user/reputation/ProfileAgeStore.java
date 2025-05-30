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

package bisq.user.reputation;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class ProfileAgeStore implements PersistableStore<ProfileAgeStore> {
    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private long lastRequested = 0;

    private ProfileAgeStore(long lastRequested) {
        this.lastRequested = lastRequested;
    }

    @Override
    public bisq.user.protobuf.ProfileAgeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.ProfileAgeStore.newBuilder()
                .clearProfileIds() // Not used anymore with v2.1.8
                .setLastRequested(lastRequested);
    }

    @Override
    public bisq.user.protobuf.ProfileAgeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ProfileAgeStore fromProto(bisq.user.protobuf.ProfileAgeStore proto) {
        return new ProfileAgeStore(proto.getLastRequested());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.ProfileAgeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ProfileAgeStore getClone() {
        return new ProfileAgeStore(lastRequested);
    }

    @Override
    public void applyPersisted(ProfileAgeStore persisted) {
        lastRequested = persisted.getLastRequested();
    }
}