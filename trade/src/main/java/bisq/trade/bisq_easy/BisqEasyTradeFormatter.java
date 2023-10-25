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

import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;

public class BisqEasyTradeFormatter {
    public static String formatBaseSideAmount(BisqEasyTrade trade) {
        return AmountFormatter.formatAmount(BisqEasyTradeUtils.getBaseSideMonetary(trade), false);
    }

    public static String formatQuoteSideAmount(BisqEasyTrade trade) {
        return AmountFormatter.formatAmount(BisqEasyTradeUtils.getQuoteSideMonetary(trade), true);
    }

    public static String formatQuoteSideAmountWithCode(BisqEasyTrade trade) {
        return AmountFormatter.formatAmountWithCode(BisqEasyTradeUtils.getQuoteSideMonetary(trade), true);
    }

    public static String formatPriceWithCode(BisqEasyTrade trade) {
        return PriceFormatter.formatWithCode(BisqEasyTradeUtils.getPriceQuote(trade));
    }

    public static String getDirection(BisqEasyTrade trade) {
        switch (trade.getTradeRole()) {
            case BUYER_AS_TAKER:
            case BUYER_AS_MAKER:
                return Res.get("bisqEasy.openTrades.table.direction.buyer");
            case SELLER_AS_TAKER:
            case SELLER_AS_MAKER:
                return Res.get("bisqEasy.openTrades.table.direction.seller");
            default:
                throw new RuntimeException("Invalid trade role");
        }
    }

    public static String getMakerTakerRole(BisqEasyTrade trade) {
        switch (trade.getTradeRole()) {
            case BUYER_AS_TAKER:
            case SELLER_AS_TAKER:
                return Res.get("bisqEasy.openTrades.table.makerTakerRole.taker");
            case BUYER_AS_MAKER:
            case SELLER_AS_MAKER:
                return Res.get("bisqEasy.openTrades.table.makerTakerRole.maker");
            default:
                throw new RuntimeException("Invalid trade role");
        }
    }
}