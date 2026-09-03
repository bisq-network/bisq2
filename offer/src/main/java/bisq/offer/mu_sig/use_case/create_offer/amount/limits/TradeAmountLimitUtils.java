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

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class TradeAmountLimitUtils {
    /**
     * The market-price rates one limit computation depends on, captured once so minimum and
     * maximum are derived from the same values and a vanished rate degrades softly instead of
     * throwing inside a map observer.
     */
    public record Rates(PriceQuote btcUsdPriceQuote, Optional<PriceQuote> btcFiatPriceQuote) {
    }

    public static Optional<Rates> findRates(MarketPriceService marketPriceService, Market market) {
        checkNotNull(marketPriceService, "marketPriceService must not be null");
        checkNotNull(market, "market must not be null");
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        Optional<PriceQuote> btcUsd = marketPriceService.findMarketPriceQuote(usdBitcoinMarket)
                .filter(quote -> quote.getValue() > 0);
        if (btcUsd.isEmpty()) {
            return Optional.empty();
        }
        if (market.isBtcFiatMarket()) {
            // The USD market's fiat leg IS the BTC/USD quote: reusing the first read keeps the
            // two legs of one context from straddling a concurrent map mutation.
            Optional<PriceQuote> btcFiat = market.equals(usdBitcoinMarket)
                    ? btcUsd
                    : marketPriceService.findMarketPriceQuote(market).filter(quote -> quote.getValue() > 0);
            if (btcFiat.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Rates(btcUsd.get(), btcFiat));
        }
        return Optional.of(new Rates(btcUsd.get(), Optional.empty()));
    }

    public static TradeAmount toTradeAmountLimit(Rates rates,
                                                 Market market,
                                                 PriceQuote priceQuote,
                                                 Fiat usdAmount) {
        checkNotNull(rates, "rates must not be null");
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(usdAmount, "usdAmount must not be null");

        Monetary quoteSideAmount;
        if (market.isBtcFiatMarket()) {
            quoteSideAmount = AmountConversion.usdToFiat(rates.btcUsdPriceQuote(),
                    rates.btcFiatPriceQuote().orElseThrow(),
                    usdAmount);
        } else {
            quoteSideAmount = AmountConversion.usdToBtc(rates.btcUsdPriceQuote(), usdAmount);
        }
        Monetary baseSideAmount = priceQuote.toBaseSideMonetary(quoteSideAmount);
        return new TradeAmount(baseSideAmount, quoteSideAmount);
    }

    /**
     * Like {@link #toTradeAmountLimit}, but every conversion leg fails with an
     * ArithmeticException when a converted amount does not fit into a long instead of silently
     * wrapping. The take flow compares and publishes limit pairs on either side, so a wrapped
     * value (which can stay positive) would corrupt the comparison or bypass a cap; failing
     * closed refuses the take when the limit cannot be represented. The caller captures one
     * {@link Rates} snapshot for a whole limit set so its limits never mix market prices.
     */
    public static TradeAmount toTradeAmountLimitExact(Rates rates,
                                                      Market market,
                                                      PriceQuote priceQuote,
                                                      Fiat usdAmount) {
        checkNotNull(rates, "rates must not be null");
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(usdAmount, "usdAmount must not be null");

        Monetary quoteSideAmount;
        if (market.isBtcFiatMarket()) {
            quoteSideAmount = AmountConversion.usdToFiatExact(rates.btcUsdPriceQuote(),
                    rates.btcFiatPriceQuote().orElseThrow(),
                    usdAmount);
        } else {
            quoteSideAmount = AmountConversion.usdToBtcExact(rates.btcUsdPriceQuote(), usdAmount);
        }
        Monetary baseSideAmount = priceQuote.toBaseSideMonetaryExact(quoteSideAmount);
        return new TradeAmount(baseSideAmount, quoteSideAmount);
    }
}
