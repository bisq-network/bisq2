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

package bisq.user.reputation.data;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedTimestampData implements AuthorizedDistributedData {
    public static final long TTL = TTL_30_DAYS;
    private final MetaData metaData = new MetaData(TTL, getClass().getSimpleName());
    private final String profileId;
    private final long date;
    private final boolean staticPublicKeysProvided;

    public AuthorizedTimestampData(String profileId, long date, boolean staticPublicKeysProvided) {
        this.profileId = profileId;
        this.date = date;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateDate(date);
    }

    @Override
    public bisq.user.protobuf.AuthorizedTimestampData toProto() {
        return bisq.user.protobuf.AuthorizedTimestampData.newBuilder()
                .setProfileId(profileId)
                .setDate(date)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static AuthorizedTimestampData fromProto(bisq.user.protobuf.AuthorizedTimestampData proto) {
        return new AuthorizedTimestampData(
                proto.getProfileId(),
                proto.getDate(),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedTimestampData.class));
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
        return "AuthorizedTimestampData{" +
                ",\r\n                    profileId=" + profileId +
                ",\r\n                    date=" + new Date(date) +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}