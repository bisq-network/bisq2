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
import bisq.i18n.Res;
import com.google.common.base.Enums;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
public enum MarketPriceProvider implements ProtoEnum {
    BISQAGGREGATE(Res.get("component.marketPrice.provider.BISQAGGREGATE")),
    COINGECKO("CoinGecko"),
    POLO("Poloniex"),
    BITFINEX("Bitfinex"),
    OTHER;

    public static MarketPriceProvider fromName(String name) {
        return Enums.getIfPresent(MarketPriceProvider.class, name).or(OTHER);
    }

    @Getter
    private final transient Optional<String> displayName;

    MarketPriceProvider() {
        this.displayName = Optional.empty();
    }

    MarketPriceProvider(String displayName) {
        this.displayName = Optional.of(displayName);
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceProvider toProtoEnum() {
        return bisq.bonded_roles.protobuf.MarketPriceProvider.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MarketPriceProvider fromProto(bisq.bonded_roles.protobuf.MarketPriceProvider proto) {
        return ProtobufUtils.enumFromProto(MarketPriceProvider.class, proto.name(), OTHER);
    }
}
