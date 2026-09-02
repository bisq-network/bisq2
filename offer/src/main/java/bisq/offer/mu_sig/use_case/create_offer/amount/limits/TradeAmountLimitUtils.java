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

package bisq.offer.mu_sig.use_case.create_offer.amount.limits;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.AmountConversion;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;

import static com.google.common.base.Preconditions.checkNotNull;

class TradeAmountLimitUtils {
    static TradeAmount toTradeAmountLimit(MarketPriceService marketPriceService,
                                          Market market,
                                          PriceQuote priceQuote,
                                          Fiat usdAmount) {
        checkNotNull(marketPriceService, "marketPriceService must not be null");
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(usdAmount, "usdAmount must not be null");

        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        PriceQuote btcUsdPriceQuote = marketPriceService.getMarketPriceQuoteOrThrow(usdBitcoinMarket);
        Monetary quoteSideAmount;
        if (market.isBtcFiatMarket()) {
            PriceQuote btcFiatPriceQuote = marketPriceService.getMarketPriceQuoteOrThrow(market);
            // For Fiat markets we convert the USD value to the Fiat currency (quote side) by using the market price and use
            // that as stable side.
            // The Bitcoin side (base side) will get adjusted by the price quote.
            quoteSideAmount = AmountConversion.usdToFiat(btcUsdPriceQuote, btcFiatPriceQuote, usdAmount);
        } else {
            // For non-Fiat markets we convert the USD value to Bitcoin (quote side) by using the market price and use
            // that as stable side.
            // The altcoin side (base side) will get adjusted by the price quote.
            quoteSideAmount = AmountConversion.usdToBtc(btcUsdPriceQuote, usdAmount);
        }
        Monetary baseSideAmount = priceQuote.toBaseSideMonetary(quoteSideAmount);
        return new TradeAmount(baseSideAmount, quoteSideAmount);
    }
}
