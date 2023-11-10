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
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MarketPrice implements Proto {
    private final PriceQuote priceQuote;
    private final long timestamp;
    private final MarketPriceProvider marketPriceProvider;

    public MarketPrice(PriceQuote priceQuote, long timestamp, MarketPriceProvider marketPriceProvider) {
        this.priceQuote = priceQuote;
        this.timestamp = timestamp;
        this.marketPriceProvider = marketPriceProvider;
    }

    public bisq.bonded_roles.protobuf.MarketPrice toProto() {
        return bisq.bonded_roles.protobuf.MarketPrice.newBuilder()
                .setPriceQuote(priceQuote.toProto())
                .setTimestamp(timestamp)
                .setMarketPriceProvider(marketPriceProvider.toProto())
                .build();
    }

    public static MarketPrice fromProto(bisq.bonded_roles.protobuf.MarketPrice proto) {
        return new MarketPrice(PriceQuote.fromProto(proto.getPriceQuote()),
                proto.getTimestamp(),
                MarketPriceProvider.fromProto(proto.getMarketPriceProvider()));
    }

    public Market getMarket() {
        return priceQuote.getMarket();
    }

    public String getProviderName() {
        return Optional.ofNullable(marketPriceProvider.getDisplayName()).orElse(Res.get("data.na"));
    }

    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
}