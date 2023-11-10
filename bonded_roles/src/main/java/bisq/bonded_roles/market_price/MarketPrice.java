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

import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class MarketPrice implements Proto {
    private final PriceQuote priceQuote;
    private final String code;
    private final long timestamp;
    private final String provider;

    public MarketPrice(PriceQuote priceQuote, String code, long timestamp, String provider) {
        this.priceQuote = priceQuote;
        this.code = code;
        this.timestamp = timestamp;
        this.provider = provider;
    }

    public bisq.bonded_roles.protobuf.MarketPrice toProto() {
        return bisq.bonded_roles.protobuf.MarketPrice.newBuilder()
                .setPriceQuote(priceQuote.toProto())
                .setCode(code)
                .setTimestamp(timestamp)
                .setProvider(provider)
                .build();
    }

    public static MarketPrice fromProto(bisq.bonded_roles.protobuf.MarketPrice proto) {
        return new MarketPrice(PriceQuote.fromProto(proto.getPriceQuote()),
                proto.getCode(),
                proto.getTimestamp(),
                proto.getProvider());
    }

    public Market getMarket() {
        return priceQuote.getMarket();
    }
}