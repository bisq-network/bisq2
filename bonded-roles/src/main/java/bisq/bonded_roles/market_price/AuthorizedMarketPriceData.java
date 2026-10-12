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

import bisq.bonded_roles.oracle.OracleNodeAuthorizedData;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.DEFAULT_PRIORITY;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedMarketPriceData implements OracleNodeAuthorizedData {
    private static final int VERSION = 1;
    public static final long TTL = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_NUM_ENTRIES = 200;

    public static final MetaData META_DATA = new MetaData(TTL, DEFAULT_PRIORITY, AuthorizedMarketPriceData.class.getSimpleName());
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

    private AuthorizedMarketPriceData(int version,
                                      TreeMap<Market, MarketPrice> marketPriceByCurrencyMap,
                                      boolean staticPublicKeysProvided) {
        this.version = version;
        this.marketPriceByCurrencyMap = marketPriceByCurrencyMap;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(marketPriceByCurrencyMap.size() < MAX_NUM_ENTRIES,
                "marketPriceByCurrencyMap size must be < " + MAX_NUM_ENTRIES);
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
        return unsafeToProto(serializeForHash);
    }

    public static AuthorizedMarketPriceData fromProto(bisq.bonded_roles.protobuf.AuthorizedMarketPriceData proto)
            throws UnresolvableProtobufException {
        try {
            // We keep every entry, also of a market which this node does not know. The oracle node signs the data as
            // a whole and the proof of work of the message commits to its hash, so dropping an entry would make this
            // node reject the data and close the connection to the peer which sent it. The set of known markets
            // depends on the currency data of the runtime, which differs between JDK versions and Android.
            // See docs/specifications/bonded-roles/market-prices.md.
            // Checked before parsing the entries, so that an oversized map costs no parsing
            checkArgument(proto.getMarketPriceByCurrencyMapCount() < MAX_NUM_ENTRIES,
                    "marketPriceByCurrencyMap size must be < " + MAX_NUM_ENTRIES);
            TreeMap<Market, MarketPrice> map = new TreeMap<>();
            for (Map.Entry<String, bisq.bonded_roles.protobuf.MarketPrice> entry : proto.getMarketPriceByCurrencyMapMap().entrySet()) {
                map.put(toMarket(entry.getKey()), MarketPrice.fromProto(entry.getValue()));
            }
            return new AuthorizedMarketPriceData(
                    proto.getVersion(),
                    map,
                    proto.getStaticPublicKeysProvided()
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    private static Market toMarket(String marketCodes) {
        return MarketRepository.findAnyMarketByMarketCodes(marketCodes)
                .orElseGet(() -> {
                    // The key must be exactly the market codes. With the default limit, split would drop trailing
                    // empty parts, and "BTC/XYZ/" would replace the entry of "BTC/XYZ".
                    String[] codes = marketCodes.split("/", -1);
                    checkArgument(codes.length == 2, "marketCodes must have a base and a quote currency code");
                    return new Market(codes[0], codes[1], codes[0], codes[1]);
                });
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

    @Override
    public String toString() {
        return "AuthorizedMarketPriceData{" +
                ",\r\n                    marketPriceByCurrencyMap=" + marketPriceByCurrencyMap +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}