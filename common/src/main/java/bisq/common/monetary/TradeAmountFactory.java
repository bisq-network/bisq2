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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TradeAmountFactory {
    public static TradeAmount fromBaseSideAmount(long baseSideAmountValue, PriceQuote priceQuote) {
        checkArgument(baseSideAmountValue >= 0, "baseSideAmountValue must be non-negative");
        checkNotNull(priceQuote, "priceQuote must not be null");
        Market market = priceQuote.getMarket();
        String baseCurrencyCode = market.getBaseCurrencyCode();
        Monetary baseSideAmount = Monetary.from(baseSideAmountValue, baseCurrencyCode);
        Monetary quoteSideAmount = priceQuote.toQuoteSideMonetary(baseSideAmount);
        return new TradeAmount(baseSideAmount, quoteSideAmount);
    }

    public static TradeAmount fromQuoteSideAmount(long quoteSideAmountValue, PriceQuote priceQuote) {
        checkArgument(quoteSideAmountValue >= 0, "quoteSideAmountValue must be non-negative");
        checkNotNull(priceQuote, "priceQuote must not be null");
        Market market = priceQuote.getMarket();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        Monetary quoteSideAmount = Monetary.from(quoteSideAmountValue, quoteCurrencyCode);
        Monetary baseSideAmount = priceQuote.toBaseSideMonetary(quoteSideAmount);
        return new TradeAmount(baseSideAmount, quoteSideAmount);
    }
}
