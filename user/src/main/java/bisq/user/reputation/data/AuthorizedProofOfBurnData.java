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
public final class AuthorizedProofOfBurnData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedProofOfBurnData.class.getSimpleName());

    private final long amount;
    private final long time;
    private final byte[] hash;

    public AuthorizedProofOfBurnData(long amount, long time, byte[] hash) {
        this.amount = amount;
        this.time = time;
        this.hash = hash;
    }

    @Override
    public bisq.user.protobuf.AuthorizedProofOfBurnData toProto() {
        return bisq.user.protobuf.AuthorizedProofOfBurnData.newBuilder()
                .setAmount(amount)
                .setTime(time)
                .setHash(ByteString.copyFrom(hash))
                .build();
    }

    public static AuthorizedProofOfBurnData fromProto(bisq.user.protobuf.AuthorizedProofOfBurnData proto) {
        return new AuthorizedProofOfBurnData(
                proto.getAmount(),
                proto.getTime(),
                proto.getHash().toByteArray());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedProofOfBurnData.class));
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
        return "AuthorizedProofOfBurnData{" +
                ",\r\n     amount=" + amount +
                ",\r\n     time=" + new Date(time) +
                ",\r\n     hash=" + Hex.encode(hash) +
                "\r\n}";
    }
}