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

package bisq.user.role;

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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedRoleRegistrationData implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(Long.MAX_VALUE);
    // The pubKeys which are authorized for publishing that data.
    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> authorizedPublicKeys = Set.of();

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedRoleRegistrationData.class.getSimpleName());

    private final UserProfile userProfile;
    private final RoleType roleType;
    private final String publicKeyAsHex;

    public AuthorizedRoleRegistrationData(UserProfile userProfile, RoleType roleType, String publicKeyAsHex) {
        this.userProfile = userProfile;
        this.roleType = roleType;
        this.publicKeyAsHex = publicKeyAsHex;
    }

    @Override
    public bisq.user.protobuf.AuthorizedRoleRegistrationData toProto() {
        return bisq.user.protobuf.AuthorizedRoleRegistrationData.newBuilder()
                .setUserProfile(userProfile.toProto())
                .setRoleType(roleType.toProto())
                .setPublicKeyAsHex(publicKeyAsHex)
                .build();
    }

    public static AuthorizedRoleRegistrationData fromProto(bisq.user.protobuf.AuthorizedRoleRegistrationData proto) {
        return new AuthorizedRoleRegistrationData(UserProfile.fromProto(proto.getUserProfile()),
                RoleType.fromProto(proto.getRoleType()),
                proto.getPublicKeyAsHex());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedRoleRegistrationData.class));
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
    public boolean isDataInvalid() {
        return false;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return authorizedPublicKeys;
        }
    }
}