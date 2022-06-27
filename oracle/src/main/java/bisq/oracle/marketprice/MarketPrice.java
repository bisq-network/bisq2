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

package bisq.oracle.marketprice;

import bisq.common.currency.Market;
import bisq.common.monetary.Quote;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class MarketPrice {
    private final Quote quote;
    private final String code;
    private final long timestamp;
    private final String provider;

    public MarketPrice(Quote quote, String code, long timestamp, String provider) {
        this.quote = quote;
        this.code = code;
        this.timestamp = timestamp;
        this.provider = provider;
    }

    public Market getMarket() {
        return quote.getMarket();
    }
}