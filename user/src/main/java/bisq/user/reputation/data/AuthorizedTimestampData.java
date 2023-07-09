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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedTimestampData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation {
    public final static long TTL = TimeUnit.DAYS.toMillis(15);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedTimestampData.class.getSimpleName());

    private final String profileId;
    private final long date;

    public AuthorizedTimestampData(String profileId, long date) {
        this.profileId = profileId;
        this.date = date;
    }

    @Override
    public bisq.user.protobuf.AuthorizedTimestampData toProto() {
        return bisq.user.protobuf.AuthorizedTimestampData.newBuilder()
                .setProfileId(profileId)
                .setDate(date)
                .build();
    }

    public static AuthorizedTimestampData fromProto(bisq.user.protobuf.AuthorizedTimestampData proto) {
        return new AuthorizedTimestampData(
                proto.getProfileId(),
                proto.getDate());
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
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public String toString() {
        return "AuthorizedTimestampData{" +
                ",\r\n     profileId=" + profileId +
                ",\r\n     date=" + new Date(date) +
                "\r\n}";
    }
}