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

import bisq.bonded_roles.oracle.OracleNodeAuthorizedData;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StoragePriority;
import bisq.user.reputation.WitnessReputationProtocol;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@Getter
public final class AuthorizedAccountAgeData implements OracleNodeAuthorizedData {
    public static final int LEGACY_VERSION = 1;
    public static final int VERSION = 2;
    public static final long TTL = TTL_30_DAYS;

    public static final MetaData META_DATA = new MetaData(TTL, StoragePriority.HIGHEST, AuthorizedAccountAgeData.class.getSimpleName());
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    private final int version;
    private final String profileId;
    private final long dateBucket;
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    private final byte[] witnessNullifier;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final boolean staticPublicKeysProvided;

    public AuthorizedAccountAgeData(String profileId,
                                    long dateBucket,
                                    byte[] witnessNullifier,
                                    boolean staticPublicKeysProvided) {
        this(VERSION,
                profileId,
                dateBucket,
                witnessNullifier,
                staticPublicKeysProvided);
    }

    private AuthorizedAccountAgeData(int version,
                                     String profileId,
                                     long dateBucket,
                                     byte[] witnessNullifier,
                                     boolean staticPublicKeysProvided) {
        this.version = version;
        this.profileId = profileId;
        this.dateBucket = dateBucket;
        this.witnessNullifier = witnessNullifier.clone();
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        if (version >= VERSION) {
            WitnessReputationProtocol.validateDateBucket(dateBucket);
            WitnessReputationProtocol.validateNullifier(witnessNullifier);
        } else {
            NetworkDataValidation.validateDate(dateBucket);
            NetworkDataValidation.validateByteArray(witnessNullifier, 0);
        }
    }

    @Override
    public bisq.user.protobuf.AuthorizedAccountAgeData.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AuthorizedAccountAgeData.newBuilder()
                .setProfileId(profileId)
                .setDateBucket(dateBucket)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version)
                .setWitnessNullifier(ByteString.copyFrom(witnessNullifier));
    }

    @Override
    public bisq.user.protobuf.AuthorizedAccountAgeData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static AuthorizedAccountAgeData fromProto(bisq.user.protobuf.AuthorizedAccountAgeData proto)
            throws UnresolvableProtobufException {
        try {
            return new AuthorizedAccountAgeData(
                    proto.getVersion(),
                    proto.getProfileId(),
                    proto.getDateBucket(),
                    proto.getWitnessNullifier().toByteArray(),
                    proto.getStaticPublicKeysProvided()
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedAccountAgeData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }

    // Oracle nodes of earlier versions published it with the key of their network ID
    @Override
    public boolean isLegacyOwnerAccepted() {
        return true;
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    public boolean isCurrentVersion() {
        return version == VERSION;
    }

    public byte[] getWitnessNullifier() {
        return witnessNullifier.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizedAccountAgeData that)) {
            return false;
        }
        return version == that.version &&
                dateBucket == that.dateBucket &&
                profileId.equals(that.profileId) &&
                Arrays.equals(witnessNullifier, that.witnessNullifier);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(version, profileId, dateBucket);
        return 31 * result + Arrays.hashCode(witnessNullifier);
    }

    @Override
    public String toString() {
        return "AuthorizedAccountAgeData{" +
                ",\r\n                    profileId=" + profileId +
                ",\r\n                    witnessNullifier=" + bisq.common.encoding.Hex.encode(witnessNullifier) +
                ",\r\n                    dateBucket=" + new Date(dateBucket) +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}
