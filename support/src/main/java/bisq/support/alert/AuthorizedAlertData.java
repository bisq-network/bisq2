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

package bisq.support.alert;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
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
public final class AuthorizedAlertData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation, StaticallyAuthorizedPublicKeyValidation {
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

    public AuthorizedAlertData(String id,
                               long date,
                               AlertType alertType,
                               Optional<String> message,
                               boolean haltTrading,
                               boolean requireVersionForTrading,
                               Optional<String> minVersion,
                               Optional<String> bannedRoleProfileId) {
        this.id = id;
        this.date = date;
        this.alertType = alertType;
        this.message = message;
        this.haltTrading = haltTrading;
        this.requireVersionForTrading = requireVersionForTrading;
        this.minVersion = minVersion;
        this.bannedRoleProfileId = bannedRoleProfileId;
    }

    @Override
    public bisq.support.protobuf.AuthorizedAlertData toProto() {
        bisq.support.protobuf.AuthorizedAlertData.Builder builder = bisq.support.protobuf.AuthorizedAlertData.newBuilder()
                .setId(id)
                .setDate(date)
                .setAlertType(alertType.toProto())
                .setHaltTrading(haltTrading)
                .setRequireVersionForTrading(requireVersionForTrading);
        message.ifPresent(builder::setMessage);
        minVersion.ifPresent(builder::setMinVersion);
        bannedRoleProfileId.ifPresent(builder::setBannedRoleProfileId);

        return builder
                .build();
    }

    public static AuthorizedAlertData fromProto(bisq.support.protobuf.AuthorizedAlertData proto) {
        return new AuthorizedAlertData(proto.getId(),
                proto.getDate(),
                AlertType.fromProto(proto.getAlertType()),
                proto.hasMessage() ? Optional.of(proto.getMessage()) : Optional.empty(),
                proto.getHaltTrading(),
                proto.getRequireVersionForTrading(),
                proto.hasMinVersion() ? Optional.of(proto.getMinVersion()) : Optional.empty(),
                proto.hasBannedRoleProfileId() ? Optional.of(proto.getBannedRoleProfileId()) : Optional.empty());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.AuthorizedAlertData.class));
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

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        return Set.of("3056301006072a8648ce3d020106052b8104000a034200049f256a94ec762254e6de1ce97649ce16d757a7a15b7e383a7163a56d89c354888d21ac73d7378feaac83371fca1207502ff708a13afdf81b4a7c4cd600a8d96e");
    }
}