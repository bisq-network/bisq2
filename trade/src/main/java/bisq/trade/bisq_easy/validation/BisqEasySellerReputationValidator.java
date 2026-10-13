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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.trade.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.reputation.ReputationService;

import java.util.concurrent.TimeUnit;

/**
 * The seller's reputation limits the trade amount in Bisq Easy. The buyer sends fiat first, so the seller's
 * reputation is the only security the buyer has. The policy is applied on the side which bears the risk:
 * the buyer as taker checks the maker's reputation before it creates the trade
 * ({@code BisqEasyTradeService.takerCreatesProtocol}, for every client), the buyer as maker checks the
 * taker's reputation in the take offer request validator.
 * Specified in docs/specifications/trade/bisq-easy-seller-reputation-limit.md.
 */
public final class BisqEasySellerReputationValidator {
    // The price node takes each fiat price from one provider. Yadio (for example BOB, EGP, ETB, PYG) publishes
    // its rates about every 5 minutes, and the price node polls it every minute. A Yadio price can therefore be
    // about 6 minutes older than the USD price of the same price node response. We allow twice that difference.
    // The 5 minute age after which the UI shows a market price as stale is too small for this check.
    public static final long MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE = TimeUnit.MINUTES.toMillis(12);

    private BisqEasySellerReputationValidator() {
    }

    // The node values the trade amount in USD with its market prices. The seller's score must cover the score which this
    // value requires. The node has a score for the seller only after it received the seller's user profile and
    // reputation data. Until then the score is 0 and the check fails.
    public static void validateSellersReputationForTradeAmount(MarketPriceService marketPriceService,
                                                               ReputationService reputationService,
                                                               Market market,
                                                               Monetary quoteSideAmount,
                                                               String sellersUserProfileId) {
        MarketPrice fiatMarketPrice = marketPriceService.findMarketPrice(market)
                .orElseThrow(() -> new MarketPriceNotCurrentException("No market price available. market=" + market));
        MarketPrice usdMarketPrice = marketPriceService.findMarketPrice(MarketRepository.getUSDBitcoinMarket())
                .orElseThrow(() -> new MarketPriceNotCurrentException("No USD market price available."));
        validateMarketPrices(fiatMarketPrice, usdMarketPrice);
        long requiredReputationScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(fiatMarketPrice.getPriceQuote(),
                usdMarketPrice.getPriceQuote(),
                quoteSideAmount);
        long sellersReputationScore = reputationService.getReputationScore(sellersUserProfileId).getTotalScore();
        validateSellersReputation(sellersReputationScore, requiredReputationScore);
    }

    public static void validateSellersReputation(long sellersReputationScore, long requiredReputationScore) {
        if (!BisqEasyTradeAmountLimits.isCoveredWithTolerance(sellersReputationScore, requiredReputationScore)) {
            throw new TradeProtocolException(String.format("The seller's reputation score of %d is too low for " +
                            "the trade amount. The trade amount requires a reputation score of %d, " +
                            "the seller's score is below that even with the %d%% tolerance " +
                            "(at least %d extra reputation points for a positive score).",
                    sellersReputationScore, requiredReputationScore, Math.round(BisqEasyTradeAmountLimits.TOLERANCE * 100),
                    BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TOLERANCE),
                    TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW);
        }
    }

    // The USD value of the trade comes from two market prices, so both have to be valid and from about
    // the same time, otherwise the cross rate could understate the trade value.
    public static void validateMarketPrices(MarketPrice fiatMarketPrice, MarketPrice usdMarketPrice) {
        if (!fiatMarketPrice.isValidDate()) {
            throw new MarketPriceNotCurrentException("Market price is outdated. marketPrice=" + fiatMarketPrice);
        }
        if (!usdMarketPrice.isValidDate()) {
            throw new MarketPriceNotCurrentException("USD market price is outdated. marketPrice=" + usdMarketPrice);
        }
        long timestampDifference = Math.abs(fiatMarketPrice.getTimestamp() - usdMarketPrice.getTimestamp());
        if (timestampDifference > MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE) {
            throw new MarketPriceNotCurrentException("Market prices are not from the same time. fiatMarketPrice=" +
                    fiatMarketPrice + "; usdMarketPrice=" + usdMarketPrice);
        }
    }
}
