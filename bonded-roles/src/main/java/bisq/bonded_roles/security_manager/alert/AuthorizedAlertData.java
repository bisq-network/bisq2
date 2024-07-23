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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
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

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedAlertData implements AuthorizedDistributedData {
    private static final int VERSION = 1;
    public final static int MAX_MESSAGE_LENGTH = 1000;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final String id;
    private final long date;
    private final AlertType alertType;
    private final Optional<String> headline;
    private final Optional<String> message;
    private final boolean haltTrading;
    private final boolean requireVersionForTrading;
    private final Optional<String> minVersion;
    private final Optional<AuthorizedBondedRole> bannedRole;
    private final String securityManagerProfileId;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedAlertData(String id,
                               long date,
                               AlertType alertType,
                               Optional<String> headline,
                               Optional<String> message,
                               boolean haltTrading,
                               boolean requireVersionForTrading,
                               Optional<String> minVersion,
                               Optional<AuthorizedBondedRole> bannedRole,
                               String securityManagerProfileId,
                               boolean staticPublicKeysProvided) {
        this(VERSION,
                id,
                date,
                alertType,
                headline,
                message,
                haltTrading,
                requireVersionForTrading,
                minVersion,
                bannedRole,
                securityManagerProfileId,
                staticPublicKeysProvided);
    }

    public AuthorizedAlertData(int version,
                                String id,
                                long date,
                                AlertType alertType,
                                Optional<String> headline,
                                Optional<String> message,
                                boolean haltTrading,
                                boolean requireVersionForTrading,
                                Optional<String> minVersion,
                                Optional<AuthorizedBondedRole> bannedRole,
                                String securityManagerProfileId,
                                boolean staticPublicKeysProvided) {
        this.version = version;
        this.id = id;
        this.date = date;
        this.alertType = alertType;
        this.headline = headline;
        this.message = message;
        this.haltTrading = haltTrading;
        this.requireVersionForTrading = requireVersionForTrading;
        this.minVersion = minVersion;
        this.bannedRole = bannedRole;
        this.securityManagerProfileId = securityManagerProfileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateText(message, MAX_MESSAGE_LENGTH);
        minVersion.ifPresent(NetworkDataValidation::validateVersion);
        NetworkDataValidation.validateProfileId(securityManagerProfileId);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedAlertData.Builder getBuilder(boolean serializeForHash) {
        bisq.bonded_roles.protobuf.AuthorizedAlertData.Builder builder = bisq.bonded_roles.protobuf.AuthorizedAlertData.newBuilder()
                .setId(id)
                .setDate(date)
                .setAlertType(alertType.toProtoEnum())
                .setHaltTrading(haltTrading)
                .setRequireVersionForTrading(requireVersionForTrading)
                .setSecurityManagerProfileId(securityManagerProfileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
        message.ifPresent(builder::setMessage);
        headline.ifPresent(headline -> {
            // We only set the headline if defaultHeadline is present (not AlertType.BAN) and
            // if headline is not same as defaultHeadline (for being backward compatible with old data which did not
            // contain the headline field). As nice side effect we save a few bytes if headline is default.
            getDefaultHeadline(alertType).ifPresent(defaultHeadline -> {
                if (!headline.equals(defaultHeadline)) {
                    builder.setHeadline(headline);
                }
            });
        });
        minVersion.ifPresent(builder::setMinVersion);
        bannedRole.ifPresent(authorizedBondedRole -> builder.setBannedRole(authorizedBondedRole.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedAlertData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedAlertData fromProto(bisq.bonded_roles.protobuf.AuthorizedAlertData proto) {
        AlertType alertType = AlertType.fromProto(proto.getAlertType());
        return new AuthorizedAlertData(
                proto.getVersion(),
                proto.getId(),
                proto.getDate(),
                alertType,
                proto.hasHeadline() ? Optional.of(proto.getHeadline()) : getDefaultHeadline(alertType),
                proto.hasMessage() ? Optional.of(proto.getMessage()) : Optional.empty(),
                proto.getHaltTrading(),
                proto.getRequireVersionForTrading(),
                proto.hasMinVersion() ? Optional.of(proto.getMinVersion()) : Optional.empty(),
                proto.hasBannedRole() ? Optional.of(AuthorizedBondedRole.fromProto(proto.getBannedRole())) : Optional.empty(),
                proto.getSecurityManagerProfileId(),
                proto.getStaticPublicKeysProvided()
        );
    }

    private static Optional<String> getDefaultHeadline(AlertType alertType) {
        if (alertType == AlertType.BAN) {
            return Optional.empty();
        } else {
            return Optional.of(Res.get("authorizedRole.securityManager.alertType." + alertType.name()));
        }
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
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.SECURITY_MANAGER_PUB_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return message.orElse("").length() > MAX_MESSAGE_LENGTH;
    }
}