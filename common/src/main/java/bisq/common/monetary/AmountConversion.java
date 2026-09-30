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

public class AmountConversion {

    /* --------------------------------------------------------------------- */
    // Bitcoin - Fiat conversions
    /* --------------------------------------------------------------------- */

    public static Monetary fiatToBtc(PriceQuote btcFiatPriceQuote,
                                     Monetary fiatAmount) {
        checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null");
        checkNotNull(fiatAmount, "fiatAmount must not be null");
        checkPriceQuoteCodes(btcFiatPriceQuote, "BTC", btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "fiatToBtc");
        checkAmountCode(fiatAmount, btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "fiatAmount", "fiatToBtc");
        return btcFiatPriceQuote.toBaseSideMonetary(fiatAmount);
    }

    public static Monetary btcToFiat(PriceQuote btcFiatPriceQuote,
                                     Monetary btcAmount) {
        checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null");
        checkNotNull(btcAmount, "btcAmount must not be null");
        checkPriceQuoteCodes(btcFiatPriceQuote, "BTC", btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "btcToFiat");
        checkAmountCode(btcAmount, "BTC", "btcAmount", "btcToFiat");
        return btcFiatPriceQuote.toQuoteSideMonetary(btcAmount);
    }


    /* --------------------------------------------------------------------- */
    // Bitcoin - USD conversions
    /* --------------------------------------------------------------------- */

    public static Monetary usdToBtc(PriceQuote btcUsdPriceQuote, Monetary usdAmount) {
        checkNotNull(btcUsdPriceQuote, "btcUsdPriceQuote must not be null");
        checkNotNull(usdAmount, "usdAmount must not be null");
        checkPriceQuoteCodes(btcUsdPriceQuote, "BTC", "USD", "usdToBtc");
        checkAmountCode(usdAmount, "USD", "usdAmount", "usdToBtc");
        return fiatToBtc(btcUsdPriceQuote, usdAmount);
    }

    public static Monetary btcToUsd(PriceQuote btcUsdPriceQuote, Monetary btcAmount) {
        checkNotNull(btcUsdPriceQuote, "btcUsdPriceQuote must not be null");
        checkNotNull(btcAmount, "btcAmount must not be null");
        checkPriceQuoteCodes(btcUsdPriceQuote, "BTC", "USD", "btcToUsd");
        checkAmountCode(btcAmount, "BTC", "btcAmount", "btcToUsd");
        return btcToFiat(btcUsdPriceQuote, btcAmount);
    }


    /* --------------------------------------------------------------------- */
    // USD - Fiat conversions
    /* --------------------------------------------------------------------- */

    // Convert USD to Bitcoin and then back to the Fiat derived from the Fiat market
    public static Monetary usdToFiat(PriceQuote btcUsdPriceQuote,
                                     PriceQuote btcFiatPriceQuote,
                                     Monetary usdAmount) {
        checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null");
        checkPriceQuoteCodes(btcFiatPriceQuote, "BTC", btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "usdToFiat");
        Monetary btc = usdToBtc(btcUsdPriceQuote, usdAmount);
        return btcToFiat(btcFiatPriceQuote, btc);
    }

    public static Monetary fiatToUsd(PriceQuote btcUsdPriceQuote,
                                     PriceQuote btcFiatPriceQuote,
                                     Monetary fiatAmount) {
        checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null");
        checkNotNull(fiatAmount, "fiatAmount must not be null");
        checkPriceQuoteCodes(btcFiatPriceQuote, "BTC", btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "fiatToUsd");
        checkAmountCode(fiatAmount, btcFiatPriceQuote.getQuoteSideMonetary().getCode(), "fiatAmount", "fiatToUsd");
        Monetary btc = fiatToBtc(btcFiatPriceQuote, fiatAmount);
        return btcToUsd(btcUsdPriceQuote, btc);
    }


    /* --------------------------------------------------------------------- */
    // OtherCrypto - Bitcoin conversions
    /* --------------------------------------------------------------------- */

    public static Monetary btcToOtherCrypto(PriceQuote btcOtherCryptoPriceQuote,
                                            Market otherCryptoBtcMarket,
                                            Monetary btcAmount) {
        checkNotNull(btcOtherCryptoPriceQuote, "btcOtherCryptoPriceQuote must not be null");
        checkNotNull(otherCryptoBtcMarket, "otherCryptoBtcMarket must not be null");
        checkNotNull(btcAmount, "btcAmount must not be null");
        checkMarketMatchesPriceQuote(otherCryptoBtcMarket, btcOtherCryptoPriceQuote, "btcToOtherCrypto");
        checkAmountCode(btcAmount, otherCryptoBtcMarket.getQuoteCurrencyCode(), "btcAmount", "btcToOtherCrypto");
        return btcOtherCryptoPriceQuote.toBaseSideMonetary(btcAmount);
    }

    public static Monetary otherCryptoToBtc(PriceQuote btcOtherCryptoPriceQuote,
                                            Market otherCryptoBtcMarket,
                                            Monetary otherCryptoAmount) {
        checkNotNull(btcOtherCryptoPriceQuote, "btcOtherCryptoPriceQuote must not be null");
        checkNotNull(otherCryptoBtcMarket, "otherCryptoBtcMarket must not be null");
        checkNotNull(otherCryptoAmount, "otherCryptoAmount must not be null");
        checkMarketMatchesPriceQuote(otherCryptoBtcMarket, btcOtherCryptoPriceQuote, "otherCryptoToBtc");
        checkAmountCode(otherCryptoAmount, otherCryptoBtcMarket.getBaseCurrencyCode(), "otherCryptoAmount", "otherCryptoToBtc");
        return btcOtherCryptoPriceQuote.toQuoteSideMonetary(otherCryptoAmount);
    }


    /* --------------------------------------------------------------------- */
    // OtherCrypto - USD conversions
    /* --------------------------------------------------------------------- */

    // Convert USD to Bitcoin and then back to the other crypto derived from the otherCryptoBtcMarket
    public static Monetary usdToOtherCrypto(PriceQuote btcUsdPriceQuote,
                                            PriceQuote btcOtherCryptoPriceQuote,
                                            Market otherCryptoBtcMarket,
                                            Monetary usdAmount) {
        checkNotNull(btcOtherCryptoPriceQuote, "btcOtherCryptoPriceQuote must not be null");
        checkNotNull(otherCryptoBtcMarket, "otherCryptoBtcMarket must not be null");
        checkMarketMatchesPriceQuote(otherCryptoBtcMarket, btcOtherCryptoPriceQuote, "usdToOtherCrypto");
        Monetary btc = usdToBtc(btcUsdPriceQuote, usdAmount);
        return btcToOtherCrypto(btcOtherCryptoPriceQuote, otherCryptoBtcMarket, btc);
    }

    public static Monetary otherCryptoToUsd(PriceQuote btcUsdPriceQuote,
                                            PriceQuote btcOtherCryptoPriceQuote,
                                            Market otherCryptoBtcMarket,
                                            Monetary otherCryptoAmount) {
        checkNotNull(btcOtherCryptoPriceQuote, "btcOtherCryptoPriceQuote must not be null");
        checkNotNull(otherCryptoBtcMarket, "otherCryptoBtcMarket must not be null");
        checkMarketMatchesPriceQuote(otherCryptoBtcMarket, btcOtherCryptoPriceQuote, "otherCryptoToUsd");
        Monetary btc = otherCryptoToBtc(btcOtherCryptoPriceQuote, otherCryptoBtcMarket, otherCryptoAmount);
        return btcToUsd(btcUsdPriceQuote, btc);
    }

    private static void checkMarketMatchesPriceQuote(Market market,
                                                     PriceQuote priceQuote,
                                                     String methodName) {
        checkArgument(market.getBaseCurrencyCode().equals(priceQuote.getBaseSideMonetary().getCode()),
                "%s expected market base code to match price quote base code. market.base=%s; quote.base=%s",
                methodName, market.getBaseCurrencyCode(), priceQuote.getBaseSideMonetary().getCode());
        checkArgument(market.getQuoteCurrencyCode().equals(priceQuote.getQuoteSideMonetary().getCode()),
                "%s expected market quote code to match price quote quote code. market.quote=%s; quote.quote=%s",
                methodName, market.getQuoteCurrencyCode(), priceQuote.getQuoteSideMonetary().getCode());
    }

    private static void checkAmountCode(Monetary amount,
                                        String expectedCode,
                                        String parameterName,
                                        String methodName) {
        checkArgument(expectedCode.equals(amount.getCode()),
                "%s expected %s code to be %s but was %s",
                methodName, parameterName, expectedCode, amount.getCode());
    }

    private static void checkPriceQuoteCodes(PriceQuote priceQuote,
                                             String expectedBaseCode,
                                             String expectedQuoteCode,
                                             String methodName) {
        checkArgument(expectedBaseCode.equals(priceQuote.getBaseSideMonetary().getCode()),
                "%s expected price quote base code %s but was %s",
                methodName, expectedBaseCode, priceQuote.getBaseSideMonetary().getCode());
        checkArgument(expectedQuoteCode.equals(priceQuote.getQuoteSideMonetary().getCode()),
                "%s expected price quote quote code %s but was %s",
                methodName, expectedQuoteCode, priceQuote.getQuoteSideMonetary().getCode());
    }
}
