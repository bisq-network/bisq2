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

package bisq.trade.bisq_easy.validation;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.trade.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The seller's reputation limits the trade amount in Bisq Easy. The buyer sends fiat first, so the seller's
 * reputation is the only security the buyer has. The policy is applied on the side which bears the risk:
 * the buyer as taker checks the maker's reputation in the take offer UI, the buyer as maker checks the
 * taker's reputation in the take offer request handler.
 * Specified in docs/specifications/trade/bisq-easy-seller-reputation-limit.md.
 */
public final class BisqEasySellerReputationValidator {
    // The 5 minute age after which the UI shows a market price as stale.
    public static final long MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE = TimeUnit.MINUTES.toMillis(5);

    private BisqEasySellerReputationValidator() {
    }

    public static void validateSellersReputation(long sellersReputationScore, long requiredReputationScore) {
        if (BisqEasyTradeAmountLimits.withTolerance(sellersReputationScore) < requiredReputationScore) {
            throw new TradeProtocolException(String.format("The seller's reputation score of %d is too low for " +
                            "the trade amount. The trade amount requires a reputation score of %d, " +
                            "the seller's score is below that even with the %d%% tolerance.",
                    sellersReputationScore, requiredReputationScore, Math.round(BisqEasyTradeAmountLimits.TOLERANCE * 100)),
                    TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW);
        }
    }

    // The USD value of the trade comes from two market prices, so both have to be valid and from about
    // the same time, otherwise the cross rate could understate the trade value.
    public static void validateMarketPrices(MarketPrice fiatMarketPrice, MarketPrice usdMarketPrice) {
        checkArgument(fiatMarketPrice.isValidDate(), "Market price is outdated. marketPrice=%s", fiatMarketPrice);
        checkArgument(usdMarketPrice.isValidDate(), "USD market price is outdated. marketPrice=%s", usdMarketPrice);
        long timestampDifference = Math.abs(fiatMarketPrice.getTimestamp() - usdMarketPrice.getTimestamp());
        checkArgument(timestampDifference <= MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE,
                "Market prices are not from the same time. fiatMarketPrice=%s; usdMarketPrice=%s",
                fiatMarketPrice, usdMarketPrice);
    }
}
