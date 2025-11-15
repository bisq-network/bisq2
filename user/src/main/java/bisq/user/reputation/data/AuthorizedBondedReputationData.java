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
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedReputationData implements AuthorizedDistributedData, PublishDateAware {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final long blockTime;
    private final long amount;
    private final byte[] hash;
    private final long lockTime;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final int blockHeight;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final String lockupTxId;

    // Added in v2.2.0
    // Once most users have updated, we can change to version 2 and later remove the excludeOnlyInVersions param
    @ExcludeForHash(excludeOnlyInVersions = {1})
    private final Optional<String> unlockTxId;  // Only set at unlock tx

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    @EqualsAndHashCode.Exclude
    @Setter
    private transient long publishDate;

    public AuthorizedBondedReputationData(long blockTime,
                                          long amount,
                                          byte[] hash,
                                          long lockTime,
                                          int blockHeight,
                                          String lockupTxId,
                                          Optional<String> unlockTxId,
                                          boolean staticPublicKeysProvided) {
        this(VERSION, blockTime, amount, hash, lockTime, blockHeight, lockupTxId, unlockTxId, staticPublicKeysProvided);
    }

    private AuthorizedBondedReputationData(int version,
                                           long blockTime,
                                           long amount,
                                           byte[] hash,
                                           long lockTime,
                                           int blockHeight,
                                           String lockupTxId,
                                           Optional<String> unlockTxId,
                                           boolean staticPublicKeysProvided) {
        this.version = version;
        this.blockTime = blockTime;
        this.amount = amount;
        this.hash = hash;
        this.lockTime = lockTime;
        this.blockHeight = blockHeight;
        this.lockupTxId = lockupTxId;
        this.unlockTxId = unlockTxId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(amount > 0);
        NetworkDataValidation.validateDate(blockTime);
        NetworkDataValidation.validateHash(hash);
        checkArgument(lockTime >= 50_000);
        if (version > 0) {
            NetworkDataValidation.validateBtcTxId(lockupTxId);
            checkArgument(blockHeight > 0);
            unlockTxId.ifPresent(NetworkDataValidation::validateBtcTxId);
        }
    }

    @Override
    public bisq.user.protobuf.AuthorizedBondedReputationData.Builder getBuilder(boolean serializeForHash) {
        bisq.user.protobuf.AuthorizedBondedReputationData.Builder builder = bisq.user.protobuf.AuthorizedBondedReputationData.newBuilder()
                .setVersion(version)
                .setAmount(amount)
                .setLockTime(lockTime)
                .setBlockTime(blockTime)
                .setHash(ByteString.copyFrom(hash))
                .setBlockHeight(blockHeight)
                .setLockupTxId(lockupTxId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided);
        unlockTxId.ifPresent(builder::setUnlockTxId);
        return builder;
    }

    @Override
    public bisq.user.protobuf.AuthorizedBondedReputationData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedBondedReputationData fromProto(bisq.user.protobuf.AuthorizedBondedReputationData proto) {
        return new AuthorizedBondedReputationData(
                proto.getVersion(),
                proto.getBlockTime(),
                proto.getAmount(),
                proto.getHash().toByteArray(),
                proto.getLockTime(),
                proto.getBlockHeight(),
                proto.getLockupTxId(),
                proto.hasUnlockTxId() ? Optional.of(proto.getUnlockTxId()) : Optional.empty(),
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
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "AuthorizedBondedReputationData{" +
                ",\r\n                    version=" + version +
                ",\r\n                    amount=" + amount +
                ",\r\n                    blockTime=" + blockTime + " (" + new Date(blockTime) + ")" +
                ",\r\n                    hash=" + Hex.encode(hash) +
                ",\r\n                    lockTime=" + lockTime +
                ",\r\n                    blockHeight=" + blockHeight +
                ",\r\n                    lockupTxId=" + lockupTxId +
                ",\r\n                    unlockTxId=" + unlockTxId +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}