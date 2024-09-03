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

package bisq.bisq_easy;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.util.MathUtils;
import bisq.user.reputation.ReputationScore;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyTradeAmountLimits {
    public static final Coin DEFAULT_MIN_BTC_TRADE_AMOUNT = Coin.asBtcFromValue(10000); // 0.0001 BTC
    public static final Coin DEFAULT_MAX_BTC_TRADE_AMOUNT = Coin.asBtcFromValue(500000); // 0.005 BTC
    public static final Fiat DEFAULT_MIN_USD_TRADE_AMOUNT = Fiat.fromFaceValue(6, "USD");
    public static final Fiat DEFAULT_MAX_USD_TRADE_AMOUNT = Fiat.fromFaceValue(300, "USD");
    public static final Fiat MAX_USD_TRADE_AMOUNT = Fiat.fromFaceValue(600, "USD");
    public static final Fiat MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION = Fiat.fromFaceValue(25, "USD");
    private static final double REPUTAION_FACTOR = 1 / 200d;

    public static Optional<Monetary> getMinQuoteSideTradeAmount(MarketPriceService marketPriceService, Market market) {
        return marketPriceService.findMarketPriceQuote(MarketRepository.getUSDBitcoinMarket())
                .map(priceQuote -> priceQuote.toBaseSideMonetary(DEFAULT_MIN_USD_TRADE_AMOUNT))
                .flatMap(defaultMinBtcTradeAmount -> marketPriceService.findMarketPriceQuote(market)
                        .map(priceQuote -> priceQuote.toQuoteSideMonetary(defaultMinBtcTradeAmount)));
    }

    public static Optional<Monetary> getMaxQuoteSideTradeAmount(MarketPriceService marketPriceService,
                                                                Market market,
                                                                ReputationScore myReputationScore) {
        Fiat maxUsdTradeAmount = getMaxUsdTradeAmount(myReputationScore.getTotalScore());
        return marketPriceService.findMarketPriceQuote(MarketRepository.getUSDBitcoinMarket())
                .map(priceQuote -> priceQuote.toBaseSideMonetary(maxUsdTradeAmount))
                .flatMap(defaultMaxBtcTradeAmount -> marketPriceService.findMarketPriceQuote(market)
                        .map(priceQuote -> priceQuote.toQuoteSideMonetary(defaultMaxBtcTradeAmount)));
    }

    // TODO add BSQ/USD price into calculation to take into account the value of the investment (at burn time)
    private static Fiat getMaxUsdTradeAmount(long totalScore) {
        // A reputation score of 30k gives a max trade amount of 150 USD
        // Upper limit is 600 USD
        long value = Math.min(MAX_USD_TRADE_AMOUNT.getValue(), MathUtils.roundDoubleToLong(totalScore * REPUTAION_FACTOR));
        Fiat maxUsdTradeAmount = Fiat.fromFaceValue(value, "USD");

        // We tolerate up to 25 USD trade amount for users with no or low reputation (< 5000)
        if (maxUsdTradeAmount.isLessThan(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION)) {
            return MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION;
        }
        return maxUsdTradeAmount;
    }
}
