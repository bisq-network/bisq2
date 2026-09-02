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

package bisq.bonded_roles.market_price;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;

import java.util.Optional;

public class MarketBasedAmountConversion {

    /* --------------------------------------------------------------------- */
    // Bitcoin - Fiat conversions
    /* --------------------------------------------------------------------- */

    public static Optional<Monetary> fiatToBtc(MarketPriceService marketPriceService,
                                               Market btcFiatMarket,
                                               Monetary fiatAmount) {
        return findPositiveMarketPriceQuote(marketPriceService, btcFiatMarket)
                .map(priceQuote -> priceQuote.toBaseSideMonetary(fiatAmount));
    }

    public static Optional<Monetary> btcToFiat(MarketPriceService marketPriceService,
                                               Market btcFiatMarket,
                                               Monetary btcAmount) {
        return findPositiveMarketPriceQuote(marketPriceService, btcFiatMarket)
                .map(priceQuote -> priceQuote.toQuoteSideMonetary(btcAmount));
    }


    /* --------------------------------------------------------------------- */
    // Bitcoin - USD conversions
    /* --------------------------------------------------------------------- */

    public static Optional<Monetary> usdToBtc(MarketPriceService marketPriceService, Monetary usdAmount) {
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        return fiatToBtc(marketPriceService, usdBitcoinMarket, usdAmount);
    }

    public static Optional<Monetary> btcToUsd(MarketPriceService marketPriceService, Monetary btcAmount) {
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        return btcToFiat(marketPriceService, usdBitcoinMarket, btcAmount);
    }


    /* --------------------------------------------------------------------- */
    // USD - Fiat conversions
    /* --------------------------------------------------------------------- */

    // Convert USD to Bitcoin and then back to the Fiat derived from the Fiat market
    public static Optional<Monetary> usdToFiat(MarketPriceService marketPriceService,
                                               Market btcFiatMarket,
                                               Monetary usdAmount) {
        return usdToBtc(marketPriceService, usdAmount)
                .flatMap(btc -> btcToFiat(marketPriceService, btcFiatMarket, btc));
    }

    public static Optional<Monetary> fiatToUsd(MarketPriceService marketPriceService,
                                               Market btcFiatMarket,
                                               Monetary fiatAmount) {
        return fiatToBtc(marketPriceService, btcFiatMarket, fiatAmount)
                .flatMap(btc -> btcToUsd(marketPriceService, btc));
    }


    /* --------------------------------------------------------------------- */
    // OtherCrypto - Bitcoin conversions
    /* --------------------------------------------------------------------- */

    public static Optional<Monetary> btcToOtherCrypto(MarketPriceService marketPriceService,
                                                      Market otherCryptoBtcMarket,
                                                      Monetary btcAmount) {
        return findPositiveMarketPriceQuote(marketPriceService, otherCryptoBtcMarket)
                .map(priceQuote -> priceQuote.toBaseSideMonetary(btcAmount));
    }

    public static Optional<Monetary> otherCryptoToBtc(MarketPriceService marketPriceService,
                                                      Market otherCryptoBtcMarket,
                                                      Monetary otherCryptoAmount) {
        return findPositiveMarketPriceQuote(marketPriceService, otherCryptoBtcMarket)
                .map(priceQuote -> priceQuote.toQuoteSideMonetary(otherCryptoAmount));
    }


    /* --------------------------------------------------------------------- */
    // OtherCrypto - USD conversions
    /* --------------------------------------------------------------------- */

    // Convert USD to Bitcoin and then back to the other crypto derived from the otherCryptoBtcMarket
    public static Optional<Monetary> usdToOtherCrypto(MarketPriceService marketPriceService,
                                                      Market otherCryptoBtcMarket,
                                                      Monetary usdAmount) {
        return usdToBtc(marketPriceService, usdAmount)
                .flatMap(btc -> btcToOtherCrypto(marketPriceService, otherCryptoBtcMarket, btc));
    }

    public static Optional<Monetary> otherCryptoToUsd(MarketPriceService marketPriceService,
                                                      Market otherCryptoBtcMarket,
                                                      Monetary otherCryptoAmount) {
        return otherCryptoToBtc(marketPriceService, otherCryptoBtcMarket, otherCryptoAmount)
                .flatMap(btc -> btcToUsd(marketPriceService, btc));

    }


    /* --------------------------------------------------------------------- */
    // Create TradeAmount for USD input and market
    /* --------------------------------------------------------------------- */

    public static TradeAmount tradeAmountFromUsdAmount(MarketPriceService marketPriceService,
                                                       Market market,
                                                       Fiat amountInUsd) {
        return findTradeAmountFromUsdAmount(marketPriceService, market, amountInUsd).orElseThrow();
    }

    public static Optional<TradeAmount> findTradeAmountFromUsdAmount(MarketPriceService marketPriceService,
                                                                     Market market,
                                                                     Fiat amountInUsd) {
        return usdToBtc(marketPriceService, amountInUsd)
                .flatMap(amountInBtc -> market.isBtcFiatMarket()
                        ? btcToFiat(marketPriceService, market, amountInBtc)
                                .map(amountInFiat -> new TradeAmount(amountInBtc, amountInFiat))
                        : btcToOtherCrypto(marketPriceService, market, amountInBtc)
                                .map(amountInOtherCrypto -> new TradeAmount(amountInOtherCrypto, amountInBtc)));
    }

    // A non-positive quote passes MarketPrice.verify (it validates only the timestamp) but is
    // not a usable rate: converting with it would produce negative or zero amounts.
    private static Optional<PriceQuote> findPositiveMarketPriceQuote(MarketPriceService marketPriceService,
                                                                     Market market) {
        return marketPriceService.findMarketPriceQuote(market)
                .filter(priceQuote -> priceQuote.getValue() > 0);
    }
}
