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

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.profile.UserProfile;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_100_DAYS;

@Slf4j
@EqualsAndHashCode
@Getter
public final class BannedUserProfileData implements AuthorizedDistributedData {
    @EqualsAndHashCode.Exclude
    private final MetaData metaData = new MetaData(TTL_100_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final UserProfile userProfile;
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public BannedUserProfileData(UserProfile userProfile, boolean staticPublicKeysProvided) {
        this.userProfile = userProfile;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.user.protobuf.BannedUserProfileData.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.BannedUserProfileData.newBuilder()
                .setUserProfile(userProfile.toProto(serializeForHash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided);
    }

    @Override
    public bisq.user.protobuf.BannedUserProfileData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BannedUserProfileData fromProto(bisq.user.protobuf.BannedUserProfileData proto) {
        return new BannedUserProfileData(
                UserProfile.fromProto(proto.getUserProfile()),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.BannedUserProfileData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.MODERATOR_PUB_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "BannedUserProfileData{" +
                ",\r\n                    userProfile=" + userProfile +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}