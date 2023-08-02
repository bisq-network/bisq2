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

package bisq.bonded_roles.alert;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedAlertData implements AuthorizedDistributedData {
    public final static int MAX_MESSAGE_LENGTH = 1000;
    public final static long TTL = TimeUnit.DAYS.toMillis(30);

    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> AUTHORIZED_PUBLIC_KEYS = Set.of(
            // SecurityManager1
            "3056301006072a8648ce3d020106052b8104000a03420004b406936966b236bcfd26a85f53b952fbc8fc1c1c80b549de589c8c3bd1e0a114dc426afb6794747341f117ac9c452ad5ecbfcbb66801527ba1dbc7a33f776a40"
    );

    private final MetaData metaData = new MetaData(TTL, 100_000, AuthorizedAlertData.class.getSimpleName());
    private final String id;
    private final long date;
    private final AlertType alertType;
    private final Optional<String> message;
    private final boolean haltTrading;
    private final boolean requireVersionForTrading;
    private final Optional<String> minVersion;
    private final Optional<AuthorizedBondedRole> bannedRole;
    private final String securityManagerProfileId;
    private final boolean staticPublicKeysProvided;

    public AuthorizedAlertData(String id,
                               long date,
                               AlertType alertType,
                               Optional<String> message,
                               boolean haltTrading,
                               boolean requireVersionForTrading,
                               Optional<String> minVersion,
                               Optional<AuthorizedBondedRole> bannedRole,
                               String securityManagerProfileId,
                               boolean staticPublicKeysProvided) {
        this.id = id;
        this.date = date;
        this.alertType = alertType;
        this.message = message;
        this.haltTrading = haltTrading;
        this.requireVersionForTrading = requireVersionForTrading;
        this.minVersion = minVersion;
        this.bannedRole = bannedRole;
        this.securityManagerProfileId = securityManagerProfileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedAlertData toProto() {
        bisq.bonded_roles.protobuf.AuthorizedAlertData.Builder builder = bisq.bonded_roles.protobuf.AuthorizedAlertData.newBuilder()
                .setId(id)
                .setDate(date)
                .setAlertType(alertType.toProto())
                .setHaltTrading(haltTrading)
                .setRequireVersionForTrading(requireVersionForTrading)
                .setSecurityManagerProfileId(securityManagerProfileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided);
        message.ifPresent(builder::setMessage);
        minVersion.ifPresent(builder::setMinVersion);
        bannedRole.ifPresent(authorizedBondedRole -> builder.setBannedRole(authorizedBondedRole.toProto()));
        return builder.build();
    }

    public static AuthorizedAlertData fromProto(bisq.bonded_roles.protobuf.AuthorizedAlertData proto) {
        return new AuthorizedAlertData(proto.getId(),
                proto.getDate(),
                AlertType.fromProto(proto.getAlertType()),
                proto.hasMessage() ? Optional.of(proto.getMessage()) : Optional.empty(),
                proto.getHaltTrading(),
                proto.getRequireVersionForTrading(),
                proto.hasMinVersion() ? Optional.of(proto.getMinVersion()) : Optional.empty(),
                proto.hasBannedRole() ? Optional.of(AuthorizedBondedRole.fromProto(proto.getBannedRole())) : Optional.empty(),
                proto.getSecurityManagerProfileId(),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedAlertData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return AUTHORIZED_PUBLIC_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return message.orElse("").length() > MAX_MESSAGE_LENGTH;
    }
}