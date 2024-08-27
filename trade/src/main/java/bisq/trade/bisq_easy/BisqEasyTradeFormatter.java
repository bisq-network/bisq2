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

import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;

public class BisqEasyTradeFormatter {
    public static String formatBaseSideAmount(BisqEasyTrade trade) {
        return formatBaseSideAmount(trade.getContract());
    }

    public static String formatBaseSideAmount(BisqEasyContract contract) {
        return AmountFormatter.formatAmount(BisqEasyTradeUtils.getBaseSideMonetary(contract), false);
    }

    public static String formatQuoteSideAmount(BisqEasyTrade trade) {
        return AmountFormatter.formatAmount(BisqEasyTradeUtils.getQuoteSideMonetary(trade), true);
    }

    public static String formatQuoteSideAmountWithCode(BisqEasyTrade trade) {
        return formatQuoteSideAmountWithCode(trade.getContract());
    }

    public static String formatQuoteSideAmountWithCode(BisqEasyContract contract) {
        return AmountFormatter.formatAmountWithCode(BisqEasyTradeUtils.getQuoteSideMonetary(contract), true);
    }

    public static String formatPriceWithCode(BisqEasyTrade trade) {
        return formatPriceWithCode(trade.getContract());
    }

    public static String formatPriceWithCode(BisqEasyContract contract) {
        return PriceFormatter.formatWithCode(BisqEasyTradeUtils.getPriceQuote(contract));
    }

    public static String getDirection(BisqEasyTrade trade) {
        return switch (trade.getTradeRole()) {
            case BUYER_AS_TAKER, BUYER_AS_MAKER -> getDirection(Direction.BUY);
            case SELLER_AS_TAKER, SELLER_AS_MAKER -> getDirection(Direction.SELL);
        };
    }

    public static String getDirection(Direction direction) {
        return direction.getDisplayStringForTraderPair();
    }

    public static String getMakerTakerRole(BisqEasyTrade trade) {
        return switch (trade.getTradeRole()) {
            case BUYER_AS_TAKER, SELLER_AS_TAKER -> Res.get("bisqEasy.openTrades.table.makerTakerRole.taker");
            case BUYER_AS_MAKER, SELLER_AS_MAKER -> Res.get("bisqEasy.openTrades.table.makerTakerRole.maker");
        };
    }
}