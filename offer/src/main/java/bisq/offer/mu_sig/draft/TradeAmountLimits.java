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

package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.market_price.MarketBasedAmountConversion;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.AmountConversion;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeAmountLimits {
    public static final Fiat MIN_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(10, "USD");
    public static final Fiat MAX_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(10000, "USD");

    // todo
    public static Fiat getMaxTradeAmountInUsd() {
        return TradeAmountLimits.MAX_TRADE_AMOUNT_IN_USD;
    }

    public static Monetary getMinTradeAmountInBtc(MarketPriceService marketPriceService) {
        return MarketBasedAmountConversion.usdToBtc(marketPriceService, MIN_TRADE_AMOUNT_IN_USD)
                .orElseThrow();
    }

    public static Monetary getMaxTradeAmountInBtc(MarketPriceService marketPriceService) {
        return MarketBasedAmountConversion.usdToBtc(marketPriceService, MAX_TRADE_AMOUNT_IN_USD)
                .orElseThrow();
    }

    public static Fiat getMaxTradeLimitInUsd(PaymentRail paymentRail) {
        return getMaxTradeLimitInUsd(paymentRail, MAX_TRADE_AMOUNT_IN_USD);
    }

    public static String getFormattedMaxTradeLimitInUsd(PaymentRail paymentRail) {
        Fiat maxTradeLimit = getMaxTradeLimitInUsd(paymentRail);
        return AmountFormatter.formatQuoteAmount(maxTradeLimit);
    }

    public static Fiat getMaxTradeLimitInUsd(PaymentRail paymentRail, Fiat maxTradeLimitByProtocol) {
        if (paymentRail instanceof FiatPaymentRail fiatPaymentRail) {
            switch (fiatPaymentRail.getChargebackRisk()) {
                case VERY_LOW -> {
                    return maxTradeLimitByProtocol;
                }
                case LOW -> {
                    return maxTradeLimitByProtocol.multiply(0.8);
                }
                case MEDIUM -> {
                    return maxTradeLimitByProtocol.multiply(0.65);
                }
                case MODERATE -> {
                    return maxTradeLimitByProtocol.multiply(0.5);
                }
                default -> {
                    return maxTradeLimitByProtocol.multiply(0d);
                }
            }
        } else {
            return maxTradeLimitByProtocol;
        }
    }

    //todo
    public static Fiat getUserSpecificLimitInUsdAmount() {
        return Fiat.fromFaceValue(4000, "USD");
    }

    @VisibleForTesting
    static TradeAmountRange toTradeAmountLimits(Market market,
                                                PriceQuote priceQuote,
                                                PriceQuote btcUsdPriceQuote,
                                                PriceQuote btcFiatPriceQuote,
                                                Fiat minTradeAmountInUsd,
                                                Fiat maxTradeAmountInUsd) {
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(btcUsdPriceQuote, "btcUsdPriceQuote must not be null");
        checkNotNull(minTradeAmountInUsd, "minTradeAmountInUsd must not be null");
        checkNotNull(maxTradeAmountInUsd, "maxTradeAmountInUsd must not be null");
        checkArgument(minTradeAmountInUsd.getValue() <= maxTradeAmountInUsd.getValue(),
                "minTradeAmountInUsd must be <= maxTradeAmountInUsd");

        Monetary minQuoteSideAmount, maxQuoteSideAmount;
        if (market.isBtcFiatMarket()) {
            checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null for BTC/fiat market");
            // For Fiat markets we convert the USD value to the Fiat currency (quote side) by using the market price and use
            // that as stable side.
            // The Bitcoin side (base side) will get adjusted by the price quote.
            minQuoteSideAmount = AmountConversion.usdToFiat(btcUsdPriceQuote, btcFiatPriceQuote, minTradeAmountInUsd);
            maxQuoteSideAmount = AmountConversion.usdToFiat(btcUsdPriceQuote, btcFiatPriceQuote, maxTradeAmountInUsd);
        } else {
            // For non-Fiat markets we convert the USD value to Bitcoin (quote side) by using the market price and use
            // that as stable side.
            // The altcoin side (base side) will get adjusted by the price quote.
            minQuoteSideAmount = AmountConversion.usdToBtc(btcUsdPriceQuote, minTradeAmountInUsd);
            maxQuoteSideAmount = AmountConversion.usdToBtc(btcUsdPriceQuote, maxTradeAmountInUsd);
        }
        Monetary minBaseSideMonetary = priceQuote.toBaseSideMonetary(minQuoteSideAmount);
        Monetary maxBaseSideMonetary = priceQuote.toBaseSideMonetary(maxQuoteSideAmount);
        TradeAmount minTradeAmount = new TradeAmount(minBaseSideMonetary, minQuoteSideAmount);
        TradeAmount maxTradeAmount = new TradeAmount(maxBaseSideMonetary, maxQuoteSideAmount);
        return new TradeAmountRange(minTradeAmount, maxTradeAmount);
    }

    @VisibleForTesting
    static Optional<TradeAmount> toUserSpecificTradeAmountLimit(Direction direction,
                                                                Market market,
                                                                PriceQuote priceQuote,
                                                                PriceQuote btcUsdPriceQuote,
                                                                PriceQuote btcFiatPriceQuote,
                                                                Fiat limitInUsd) {
        checkNotNull(direction, "direction must not be null");
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(btcUsdPriceQuote, "btcUsdPriceQuote must not be null");
        checkNotNull(limitInUsd, "limitInUsd must not be null");
        if (direction.isSell()) {
            return Optional.empty();
        }

        Monetary quoteSideAmount;
        if (market.isBtcFiatMarket()) {
            checkNotNull(btcFiatPriceQuote, "btcFiatPriceQuote must not be null for BTC/fiat market");
            // For Fiat markets we convert the USD value to the Fiat currency (quote side) by using the market price and use
            // that as stable side.
            // The Bitcoin side (base side) will get adjusted by the price quote.
            quoteSideAmount = AmountConversion.usdToFiat(btcUsdPriceQuote, btcFiatPriceQuote, limitInUsd);
        } else {
            // For non-Fiat markets we convert the USD value to Bitcoin (quote side) by using the market price and use
            // that as stable side.
            // The altcoin side (base side) will get adjusted by the price quote.
            quoteSideAmount = AmountConversion.usdToBtc(btcUsdPriceQuote, limitInUsd);
        }
        Monetary minBaseSideMonetary = priceQuote.toBaseSideMonetary(quoteSideAmount);
        return Optional.of(new TradeAmount(minBaseSideMonetary, quoteSideAmount));
    }

    @VisibleForTesting
    static TradeAmount clampTradeAmount(TradeAmountRange tradeAmountLimits,
                                        Optional<TradeAmount> userSpecificTradeAmountLimit,
                                        TradeAmount tradeAmount,
                                        boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange limits = getClampLimits(tradeAmountLimits,
                userSpecificTradeAmountLimit,
                includeUserSpecificTradeAmountLimit);
        return clampTradeAmount(limits, tradeAmount);
    }

    @VisibleForTesting
    static Monetary clampBaseSideAmount(TradeAmountRange tradeAmountLimits,
                                        Optional<TradeAmount> userSpecificTradeAmountLimit,
                                        Monetary baseSideAmount,
                                        boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange limits = getClampLimits(tradeAmountLimits,
                userSpecificTradeAmountLimit,
                includeUserSpecificTradeAmountLimit);
        return clampBaseSideAmount(limits, baseSideAmount);
    }

    @VisibleForTesting
    static Monetary clampQuoteSideAmount(TradeAmountRange tradeAmountLimits,
                                         Optional<TradeAmount> userSpecificTradeAmountLimit,
                                         Monetary quoteSideAmount,
                                         boolean includeUserSpecificTradeAmountLimit) {
        TradeAmountRange limits = getClampLimits(tradeAmountLimits,
                userSpecificTradeAmountLimit,
                includeUserSpecificTradeAmountLimit);
        return clampQuoteSideAmount(limits, quoteSideAmount);
    }

    @VisibleForTesting
    static TradeAmountRange getClampLimits(TradeAmountRange tradeAmountLimits,
                                           Optional<TradeAmount> userSpecificTradeAmountLimit,
                                           boolean includeUserSpecificTradeAmountLimit) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        checkNotNull(userSpecificTradeAmountLimit, "userSpecificTradeAmountLimit must not be null");
        if (includeUserSpecificTradeAmountLimit) {
            return userSpecificTradeAmountLimit
                    .map(tradeAmount -> clampTradeAmount(tradeAmountLimits, tradeAmount))
                    .map(tradeAmount -> new TradeAmountRange(tradeAmountLimits.getMin(), tradeAmount))
                    .orElse(tradeAmountLimits);
        } else {
            return tradeAmountLimits;
        }
    }

    @VisibleForTesting
    static TradeAmount clampTradeAmount(TradeAmountRange tradeAmountLimits, TradeAmount tradeAmount) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        return tradeAmount.clamp(tradeAmountLimits);

    }

    @VisibleForTesting
    static Monetary clampBaseSideAmount(TradeAmountRange tradeAmountLimits, Monetary baseSideAmount) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        checkNotNull(baseSideAmount, "baseSideAmount must not be null");
        Monetary min = tradeAmountLimits.getMin().getBaseSideAmount();
        Monetary max = tradeAmountLimits.getMax().getBaseSideAmount();
        return baseSideAmount.clamp(min, max);
    }

    @VisibleForTesting
    static Monetary clampQuoteSideAmount(TradeAmountRange tradeAmountLimits, Monetary quoteSideAmount) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        checkNotNull(quoteSideAmount, "quoteSideAmount must not be null");
        Monetary min = tradeAmountLimits.getMin().getQuoteSideAmount();
        Monetary max = tradeAmountLimits.getMax().getQuoteSideAmount();
        return quoteSideAmount.clamp(min, max);
    }
}
