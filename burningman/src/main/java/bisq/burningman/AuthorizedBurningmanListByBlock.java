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

package bisq.burningman;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

/**
 * Even the Burningman is not yet directly related to the Bisq user, it might become related in future in case we allow
 * users to become a burningman directly from Bisq 2 or use it for reputation and security aspects.
 * For that reason we leave that domain inside user instead of trade, where it is currently used, but it would feel
 * the right place as burningman is not directly part of the trade but just an auxiliary aspect of trade.
 */
@Slf4j
@Getter
public final class AuthorizedBurningmanListByBlock implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    // We use a rather long TTL to ensure in case the oracle nodes have issues to still have BM data in the network.
    // Oracle nodes remove BM data which are older than a certain number of blocks (e.g. 144 blocks or 1 day)
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());

    @ExcludeForHash
    private final int version;
    private final boolean staticPublicKeysProvided;
    private final int blockHeight;
    private final List<BurningmanData> burningmanDataList;

    public AuthorizedBurningmanListByBlock(boolean staticPublicKeysProvided,
                                           int blockHeight,
                                           List<BurningmanData> burningmanDataList
    ) {
        this(VERSION, staticPublicKeysProvided, blockHeight, burningmanDataList);
    }

    private AuthorizedBurningmanListByBlock(int version,
                                            boolean staticPublicKeysProvided,
                                            int blockHeight,
                                            List<BurningmanData> burningmanDataList
    ) {
        this.version = version;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.blockHeight = blockHeight;
        this.burningmanDataList = burningmanDataList;

        verify();
    }

    @Override
    public void verify() {
     /*   checkArgument(amount > 0);
        NetworkDataValidation.validateDate(blockTime);
        NetworkDataValidation.validateHash(hash);
        if (version > 0) {
            NetworkDataValidation.validateBtcTxId(txId);
            checkArgument(blockHeight > 0);
        }*/
    }

    @Override
    public bisq.burningman.protobuf.AuthorizedBurningmanListByBlock.Builder getBuilder(boolean serializeForHash) {
        return bisq.burningman.protobuf.AuthorizedBurningmanListByBlock.newBuilder()
                .setVersion(version)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setBlockHeight(blockHeight)
                .addAllBurningmanData(burningmanDataList.stream().map(e -> e.toProto(serializeForHash)).toList());
    }

    @Override
    public bisq.burningman.protobuf.AuthorizedBurningmanListByBlock toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedBurningmanListByBlock fromProto(bisq.burningman.protobuf.AuthorizedBurningmanListByBlock proto) {
        return new AuthorizedBurningmanListByBlock(
                proto.getVersion(),
                proto.getStaticPublicKeysProvided(),
                proto.getBlockHeight(),
                proto.getBurningmanDataList().stream().map(BurningmanData::fromProto).toList());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.burningman.protobuf.AuthorizedBurningmanListByBlock.class));
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
}