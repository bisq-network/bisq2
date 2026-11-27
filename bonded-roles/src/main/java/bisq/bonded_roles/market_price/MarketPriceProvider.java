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

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.i18n.Res;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public enum MarketPriceProvider implements ProtoEnum {
    BISQAGGREGATE,
    COINGECKO,
    POLO,
    BITFINEX,
    OTHER;

    public static MarketPriceProvider fromName(String name) {
        return Enums.getIfPresent(MarketPriceProvider.class, name).or(OTHER);
    }

    // Res is called here and not at class initialization: the i18n bundles may not be loaded yet when the class is
    // initialized, and a failed class initialization makes every later use of the class fail.
    public Optional<String> getDisplayName() {
        return switch (this) {
            case BISQAGGREGATE -> Optional.of(Res.get("component.marketPrice.provider.BISQAGGREGATE"));
            case COINGECKO -> Optional.of("CoinGecko");
            case POLO -> Optional.of("Poloniex");
            case BITFINEX -> Optional.of("Bitfinex");
            case OTHER -> Optional.empty();
        };
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceProvider toProtoEnum() {
        return bisq.bonded_roles.protobuf.MarketPriceProvider.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MarketPriceProvider fromProto(bisq.bonded_roles.protobuf.MarketPriceProvider proto)
            throws UnresolvableProtobufException {
        return ProtobufUtils.enumFromProto(MarketPriceProvider.class, proto.name(), OTHER);
    }
}
