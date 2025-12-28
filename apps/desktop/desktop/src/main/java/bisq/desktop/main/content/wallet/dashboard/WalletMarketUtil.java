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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.presentation.formatters.AmountFormatter;

public class WalletMarketUtil {
    static String getFormattedConvertedAmount(Coin btcBalance, MarketPrice marketPrice, boolean useLowPrecisionForCoin) {
        Market market = marketPrice.getMarket();
        if (market.isXmr()) {
            String code = market.getBaseCurrencyCode();
            double value = btcBalance.asDouble() / marketPrice.getPriceQuote().asDouble();
            Coin coin = Coin.fromFaceValue(value, code);
            return AmountFormatter.formatAmount(coin, useLowPrecisionForCoin);
        } else if (market.isBtcFiatMarket()) {
            String code = market.getQuoteCurrencyCode();
            double value = btcBalance.asDouble() * marketPrice.getPriceQuote().asDouble();
            Fiat fiat = Fiat.fromFaceValue(value, code);
            return AmountFormatter.formatAmount(fiat, true);
        } else {
            throw new IllegalStateException("Market not supported for conversion. market=" + market);
        }
    }

    static String getMarketCode(Market market) {
        if (market.isXmr()) {
            return market.getBaseCurrencyCode();
        } else if (market.isBtcFiatMarket()) {
            return market.getQuoteCurrencyCode();
        } else {
            throw new IllegalStateException("Market not supported for conversion. market=" + market);
        }
    }
}
