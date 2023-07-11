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

import bisq.bonded_roles.BondedRoleType;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedAlertData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation {
    public final static int MAX_MESSAGE_LENGTH = 1000;
    public final static long TTL = TimeUnit.DAYS.toMillis(15);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedAlertData.class.getSimpleName());

    private final String id;
    private final long date;
    private final AlertType alertType;
    private final Optional<String> message;
    private final boolean haltTrading;
    private final boolean requireVersionForTrading;
    private final Optional<String> minVersion;
    private final Optional<String> bannedRoleProfileId;
    private final Optional<BondedRoleType> bannedBondedRoleType;

    public AuthorizedAlertData(String id,
                               long date,
                               AlertType alertType,
                               Optional<String> message,
                               boolean haltTrading,
                               boolean requireVersionForTrading,
                               Optional<String> minVersion,
                               Optional<String> bannedRoleProfileId,
                               Optional<BondedRoleType> bannedBondedRoleType) {
        this.id = id;
        this.date = date;
        this.alertType = alertType;
        this.message = message;
        this.haltTrading = haltTrading;
        this.requireVersionForTrading = requireVersionForTrading;
        this.minVersion = minVersion;
        this.bannedRoleProfileId = bannedRoleProfileId;
        this.bannedBondedRoleType = bannedBondedRoleType;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedAlertData toProto() {
        bisq.bonded_roles.protobuf.AuthorizedAlertData.Builder builder = bisq.bonded_roles.protobuf.AuthorizedAlertData.newBuilder()
                .setId(id)
                .setDate(date)
                .setAlertType(alertType.toProto())
                .setHaltTrading(haltTrading)
                .setRequireVersionForTrading(requireVersionForTrading);
        message.ifPresent(builder::setMessage);
        minVersion.ifPresent(builder::setMinVersion);
        bannedRoleProfileId.ifPresent(builder::setBannedRoleProfileId);
        bannedBondedRoleType.ifPresent(bannedBondedRoleType -> builder.setBannedBondedRoleType(bannedBondedRoleType.toProto()));

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
                proto.hasBannedRoleProfileId() ? Optional.of(proto.getBannedRoleProfileId()) : Optional.empty(),
                proto.hasBannedBondedRoleType() ? Optional.of(BondedRoleType.fromProto(proto.getBannedBondedRoleType())) : Optional.empty());
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
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return message.orElse("").length() > MAX_MESSAGE_LENGTH;
    }
}