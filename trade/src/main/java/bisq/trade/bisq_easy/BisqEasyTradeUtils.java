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

package bisq.trade.bisq_easy;

import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;

public class BisqEasyTradeUtils {
    public static Monetary getBaseSideMonetary(BisqEasyTrade trade) {
        return Monetary.from(trade.getContract().getBaseSideAmount(), trade.getOffer().getMarket().getBaseCurrencyCode());
    }

    public static Monetary getQuoteSideMonetary(BisqEasyTrade trade) {
        return Monetary.from(trade.getContract().getQuoteSideAmount(), trade.getOffer().getMarket().getQuoteCurrencyCode());
    }

    public static PriceQuote getPriceQuote(BisqEasyTrade trade) {
        return PriceQuote.from(getBaseSideMonetary(trade), getQuoteSideMonetary(trade));
    }
}