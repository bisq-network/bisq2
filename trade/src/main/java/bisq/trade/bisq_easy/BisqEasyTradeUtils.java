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
import bisq.contract.bisq_easy.BisqEasyContract;

public class BisqEasyTradeUtils {
    public static Monetary getBaseSideMonetary(BisqEasyTrade trade) {
        return getBaseSideMonetary(trade.getContract());
    }

    public static Monetary getBaseSideMonetary(BisqEasyContract contract) {
        return Monetary.from(contract.getBaseSideAmount(), contract.getOffer().getMarket().getBaseCurrencyCode());
    }

    public static Monetary getQuoteSideMonetary(BisqEasyTrade trade) {
        BisqEasyContract contract = trade.getContract();
        return Monetary.from(contract.getQuoteSideAmount(), contract.getOffer().getMarket().getQuoteCurrencyCode());
    }

    public static Monetary getQuoteSideMonetary(BisqEasyContract contract) {
        return Monetary.from(contract.getQuoteSideAmount(), contract.getOffer().getMarket().getQuoteCurrencyCode());
    }

    public static PriceQuote getPriceQuote(BisqEasyTrade trade) {
        return PriceQuote.from(getBaseSideMonetary(trade), getQuoteSideMonetary(trade));
    }

    public static PriceQuote getPriceQuote(BisqEasyContract contract) {
        return PriceQuote.from(getBaseSideMonetary(contract), getQuoteSideMonetary(contract));
    }
}