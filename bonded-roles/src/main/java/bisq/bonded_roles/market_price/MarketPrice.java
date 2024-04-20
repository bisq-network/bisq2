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
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@EqualsAndHashCode
public final class MarketPrice implements NetworkProto {
    private static final long INVALID_AGE = TimeUnit.HOURS.toMillis(12);
    private static final long STALE_AGE = TimeUnit.MINUTES.toMillis(5);

    public enum Source {
        PERSISTED,
        PROPAGATED_IN_NETWORK,
        REQUESTED_FROM_PRICE_NODE
    }

    private final PriceQuote priceQuote;
    private final long timestamp;
    private final MarketPriceProvider marketPriceProvider;
    @Setter
    private transient Source source;

    public MarketPrice(PriceQuote priceQuote, long timestamp, MarketPriceProvider marketPriceProvider) {
        this.priceQuote = priceQuote;
        this.timestamp = timestamp;
        this.marketPriceProvider = marketPriceProvider;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPrice.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.MarketPrice.newBuilder()
                .setPriceQuote(priceQuote.toProto(serializeForHash))
                .setTimestamp(timestamp)
                .setMarketPriceProvider(marketPriceProvider.toProtoEnum());
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPrice toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
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

    public boolean isStale() {
        return System.currentTimeMillis() - timestamp > STALE_AGE;
    }

    public boolean isValidDate() {
        return System.currentTimeMillis() - timestamp < INVALID_AGE;
    }

    @Override
    public String toString() {
        return "MarketPrice{" +
                "priceQuote=" + priceQuote +
                ", timestamp=" + new Date(timestamp) +
                ", marketPriceProvider=" + marketPriceProvider +
                ", source=" + source +
                '}';
    }
}