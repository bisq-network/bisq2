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

package bisq.bonded_roles.market_price;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.DEFAULT_PRIORITY;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedMarketPriceData implements AuthorizedDistributedData {
    private static final int VERSION = 1;
    public static final long TTL = TimeUnit.MINUTES.toMillis(10);

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL, DEFAULT_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    // We need deterministic sorting or the map, so we use a treemap
    private final TreeMap<Market, MarketPrice> marketPriceByCurrencyMap;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedMarketPriceData(TreeMap<Market, MarketPrice> marketPriceByCurrencyMap,
                                     boolean staticPublicKeysProvided) {
        this(VERSION, marketPriceByCurrencyMap, staticPublicKeysProvided);
    }

    public AuthorizedMarketPriceData(int version,
                                      TreeMap<Market, MarketPrice> marketPriceByCurrencyMap,
                                      boolean staticPublicKeysProvided) {
        this.version = version;
        this.marketPriceByCurrencyMap = marketPriceByCurrencyMap;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(marketPriceByCurrencyMap.size() < 200,
                "marketPriceByCurrencyMap size must be < 200" + marketPriceByCurrencyMap.size());
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedMarketPriceData.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.AuthorizedMarketPriceData.newBuilder()
                .putAllMarketPriceByCurrencyMap(marketPriceByCurrencyMap.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().getMarketCodes(),
                                e -> e.getValue().toProto(serializeForHash))))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedMarketPriceData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedMarketPriceData fromProto(bisq.bonded_roles.protobuf.AuthorizedMarketPriceData proto) {
        Map<Market, MarketPrice> map = proto.getMarketPriceByCurrencyMapMap().entrySet().stream()
                .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey()).isPresent())
                .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey()).orElseThrow(),
                        e -> MarketPrice.fromProto(e.getValue())));
        return new AuthorizedMarketPriceData(
                proto.getVersion(),
                new TreeMap<>(map),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedMarketPriceData.class));
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
        return "AuthorizedMarketPriceData{" +
                ",\r\n                    marketPriceByCurrencyMap=" + marketPriceByCurrencyMap +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}