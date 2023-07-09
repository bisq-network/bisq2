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

import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedReputationData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedBondedReputationData.class.getSimpleName());

    private final long amount;
    private final long lockTime;
    private final long time;
    private final byte[] hash;

    public AuthorizedBondedReputationData(long amount, long time, byte[] hash, long lockTime) {
        this.amount = amount;
        this.time = time;
        this.hash = hash;
        this.lockTime = lockTime;
    }

    @Override
    public bisq.user.protobuf.AuthorizedBondedReputationData toProto() {
        return bisq.user.protobuf.AuthorizedBondedReputationData.newBuilder()
                .setAmount(amount)
                .setLockTime(lockTime)
                .setTime(time)
                .setHash(ByteString.copyFrom(hash))
                .build();
    }

    public static AuthorizedBondedReputationData fromProto(bisq.user.protobuf.AuthorizedBondedReputationData proto) {
        return new AuthorizedBondedReputationData(
                proto.getAmount(),
                proto.getTime(),
                proto.getHash().toByteArray(),
                proto.getLockTime());
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
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public String toString() {
        return "AuthorizedBondedReputationData{" +
                ",\r\n     amount=" + amount +
                ",\r\n     time=" + new Date(time) +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     lockTime=" + lockTime +
                "\r\n}";
    }
}