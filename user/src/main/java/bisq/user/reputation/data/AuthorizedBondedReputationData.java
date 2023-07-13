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

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedReputationData implements AuthorizedDistributedData, StaticallyAuthorizedPublicKeyValidation, DeferredAuthorizedPublicKeyValidation {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);

    // todo Production key not set yet - we use devMode key only yet
    private static final Set<String> AUTHORIZED_PUBLIC_KEYS = Set.of("3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63");


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
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return AUTHORIZED_PUBLIC_KEYS;
        }
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