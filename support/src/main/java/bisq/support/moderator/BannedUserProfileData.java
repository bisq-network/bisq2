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

package bisq.support.moderator;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class BannedUserProfileData implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(15);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            BannedUserProfileData.class.getSimpleName());

    private final String profileId;
    private final boolean staticPublicKeysProvided;

    public BannedUserProfileData(String profileId, boolean staticPublicKeysProvided) {
        this.profileId = profileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
    }

    @Override
    public bisq.support.protobuf.BannedUserProfileData toProto() {
        return bisq.support.protobuf.BannedUserProfileData.newBuilder()
                .setProfileId(profileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static BannedUserProfileData fromProto(bisq.support.protobuf.BannedUserProfileData proto) {
        return new BannedUserProfileData(
                proto.getProfileId(),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.BannedUserProfileData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return AuthorizedPubKeys.KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "BannedUserProfileData{" +
                ",\r\n                    profileId=" + profileId +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}