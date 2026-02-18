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

package bisq.trade.mu_sig;

import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;

public final class MuSigTradeFormatter {
    public static String formatBaseSideAmount(MuSigTrade trade) {
        return formatBaseSideAmount(trade.getContract());
    }

    public static String formatBaseSideAmount(MuSigContract contract) {
        return AmountFormatter.formatBaseAmount(MuSigTradeUtils.getBaseSideMonetary(contract));
    }

    public static String formatBaseSideAmountWithCode(MuSigTrade trade) {
        return formatBaseSideAmountWithCode(trade.getContract());
    }

    public static String formatBaseSideAmountWithCode(MuSigContract contract) {
        return AmountFormatter.formatBaseAmountWithCode(MuSigTradeUtils.getBaseSideMonetary(contract));
    }

    public static String formatQuoteSideAmount(MuSigTrade trade) {
        return AmountFormatter.formatQuoteAmount(MuSigTradeUtils.getQuoteSideMonetary(trade));
    }

    public static String formatQuoteSideAmount(MuSigContract contract) {
        return AmountFormatter.formatQuoteAmount(MuSigTradeUtils.getQuoteSideMonetary(contract));
    }

    public static String formatQuoteSideAmountWithCode(MuSigTrade trade) {
        return formatQuoteSideAmountWithCode(trade.getContract());
    }

    public static String formatQuoteSideAmountWithCode(MuSigContract contract) {
        return AmountFormatter.formatQuoteAmountWithCode(MuSigTradeUtils.getQuoteSideMonetary(contract));
    }

    public static String formatPriceWithCode(MuSigTrade trade) {
        return formatPriceWithCode(trade.getContract());
    }

    public static String formatPriceWithCode(MuSigContract contract) {
        return PriceFormatter.formatWithCode(MuSigTradeUtils.getPriceQuote(contract));
    }

    public static String getDirectionalTitle(MuSigTrade trade) {
        return trade.getDisplayDirection().getDirectionalTitle();
    }

    public static String getMakerTakerRole(MuSigTrade trade) {
        return switch (trade.getTradeRole()) {
            case BUYER_AS_TAKER, SELLER_AS_TAKER -> Res.get("bisqEasy.openTrades.table.makerTakerRole.taker");
            case BUYER_AS_MAKER, SELLER_AS_MAKER -> Res.get("bisqEasy.openTrades.table.makerTakerRole.maker");
        };
    }
}