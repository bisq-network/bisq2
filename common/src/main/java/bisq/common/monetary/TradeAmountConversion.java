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

package bisq.common.monetary;

import bisq.common.market.Market;

import static com.google.common.base.Preconditions.checkNotNull;

public class TradeAmountConversion {
    public static TradeAmount toTradeAmount(Market market, PriceQuote priceQuote, Monetary amount) {
        checkNotNull(market, "Market must not be null");
        checkNotNull(amount, "amount must not be null");

        if (isBaseSideAmount(market, amount)) {
            return new TradeAmount(amount, priceQuote.toQuoteSideMonetary(amount));
        } else {
            return new TradeAmount(priceQuote.toBaseSideMonetary(amount), amount);
        }
    }

    public static boolean isBaseSideAmount(Market market, Monetary amount) {
        return isBaseSideAmount(market.getBaseCurrencyCode(), amount);
    }

    public static boolean isBaseSideAmount(String baseCurrencyCode, Monetary amount) {
        return baseCurrencyCode.equals(amount.getCode());
    }
}
