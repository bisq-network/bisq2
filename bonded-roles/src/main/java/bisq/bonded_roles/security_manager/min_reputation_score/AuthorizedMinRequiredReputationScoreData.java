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

package bisq.bonded_roles.security_manager.min_reputation_score;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_100_DAYS;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
@Deprecated(since = "2.1.1")
public final class AuthorizedMinRequiredReputationScoreData implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final long date;
    private final long minRequiredReputationScore;
    private final String securityManagerProfileId;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedMinRequiredReputationScoreData(long date,
                                                    long minRequiredReputationScore,
                                                    String securityManagerProfileId,
                                                    boolean staticPublicKeysProvided) {
        this(VERSION,
                date,
                minRequiredReputationScore,
                securityManagerProfileId,
                staticPublicKeysProvided);
    }

    public AuthorizedMinRequiredReputationScoreData(int version,
                                                     long date,
                                                     long minRequiredReputationScore,
                                                     String securityManagerProfileId,
                                                     boolean staticPublicKeysProvided) {
        this.version = version;
        this.date = date;
        this.minRequiredReputationScore = minRequiredReputationScore;
        this.securityManagerProfileId = securityManagerProfileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateProfileId(securityManagerProfileId);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedMinRequiredReputationScoreData.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.AuthorizedMinRequiredReputationScoreData.newBuilder()
                .setDate(date)
                .setMinRequiredReputationScore(minRequiredReputationScore)
                .setSecurityManagerProfileId(securityManagerProfileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedMinRequiredReputationScoreData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedMinRequiredReputationScoreData fromProto(bisq.bonded_roles.protobuf.AuthorizedMinRequiredReputationScoreData proto) {
        return new AuthorizedMinRequiredReputationScoreData(
                proto.getVersion(),
                proto.getDate(),
                proto.getMinRequiredReputationScore(),
                proto.getSecurityManagerProfileId(),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedMinRequiredReputationScoreData.class));
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
        return false;
    }
}