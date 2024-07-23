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
public final class AuthorizedProofOfBurnData implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final long blockTime;
    private final long amount;
    private final byte[] hash;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final int blockHeight;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final String txId;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedProofOfBurnData(long blockTime,
                                     long amount,
                                     byte[] hash,
                                     int blockHeight,
                                     String txId,
                                     boolean staticPublicKeysProvided) {
        this(VERSION, blockTime, amount, hash, blockHeight, txId, staticPublicKeysProvided);
    }

    public AuthorizedProofOfBurnData(int version,
                                     long blockTime,
                                     long amount,
                                     byte[] hash,
                                     int blockHeight,
                                     String txId,
                                     boolean staticPublicKeysProvided) {
        this.version = version;
        this.blockTime = blockTime;
        this.amount = amount;
        this.hash = hash;
        this.blockHeight = blockHeight;
        this.txId = txId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(amount > 0);
        NetworkDataValidation.validateDate(blockTime);
        NetworkDataValidation.validateHash(hash);
        if (version > 0) {
            NetworkDataValidation.validateBtcTxId(txId);
            checkArgument(blockHeight > 0);
        }
    }

    @Override
    public bisq.user.protobuf.AuthorizedProofOfBurnData.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AuthorizedProofOfBurnData.newBuilder()
                .setVersion(version)
                .setAmount(amount)
                .setBlockTime(blockTime)
                .setHash(ByteString.copyFrom(hash))
                .setBlockHeight(blockHeight)
                .setTxId(txId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided);
    }

    @Override
    public bisq.user.protobuf.AuthorizedProofOfBurnData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedProofOfBurnData fromProto(bisq.user.protobuf.AuthorizedProofOfBurnData proto) {
        return new AuthorizedProofOfBurnData(
                proto.getVersion(),
                proto.getBlockTime(),
                proto.getAmount(),
                proto.getHash().toByteArray(),
                proto.getBlockHeight(),
                proto.getTxId(),
                proto.getStaticPublicKeysProvided());
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
        return "AuthorizedProofOfBurnData{" +
                ",\r\n                    version=" + version +
                ",\r\n                    amount=" + amount +
                ",\r\n                    blockTime=" + blockTime + " (" + new Date(blockTime) + ")" +
                ",\r\n                    hash=" + Hex.encode(hash) +
                ",\r\n                    blockHeight=" + blockHeight +
                ",\r\n                    txId=" + txId +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}