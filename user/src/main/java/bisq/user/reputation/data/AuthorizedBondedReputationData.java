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
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_100_DAYS;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedReputationData implements AuthorizedDistributedData {
    private final MetaData metaData = new MetaData(TTL_100_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final long amount;
    private final long lockTime;
    private final boolean staticPublicKeysProvided;
    private final long time;
    private final byte[] hash;

    public AuthorizedBondedReputationData(long amount, long time, byte[] hash, long lockTime, boolean staticPublicKeysProvided) {
        this.amount = amount;
        this.time = time;
        this.hash = hash;
        this.lockTime = lockTime;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        NetworkDataValidation.validateDate(time);
        NetworkDataValidation.validateHash(hash);
        checkArgument(amount > 0);
        checkArgument(lockTime >= 50_000);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize());//38
    }

    @Override
    public bisq.user.protobuf.AuthorizedBondedReputationData toProto() {
        return bisq.user.protobuf.AuthorizedBondedReputationData.newBuilder()
                .setAmount(amount)
                .setLockTime(lockTime)
                .setTime(time)
                .setHash(ByteString.copyFrom(hash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static AuthorizedBondedReputationData fromProto(bisq.user.protobuf.AuthorizedBondedReputationData proto) {
        return new AuthorizedBondedReputationData(
                proto.getAmount(),
                proto.getTime(),
                proto.getHash().toByteArray(),
                proto.getLockTime(),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedBondedReputationData.class));
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
        return "AuthorizedBondedReputationData{" +
                ",\r\n                    amount=" + amount +
                ",\r\n                    time=" + new Date(time) +
                ",\r\n                    hash=" + Hex.encode(hash) +
                ",\r\n                    lockTime=" + lockTime +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}