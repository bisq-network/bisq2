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
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
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
    public static final double TOLERANCE = 0.1;

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
                        .map(priceQuote -> {
                            Monetary quoteSideMonetary = priceQuote.toQuoteSideMonetary(defaultMaxBtcTradeAmount);
                            // quoteSideMonetary= Monetary.from(quoteSideMonetary,quoteSideMonetary.getValue()-10000*20);
                            return quoteSideMonetary;
                        }));
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


    public static OfferAmountLimitsResult checkOfferAmountLimits(ReputationService reputationService,
                                                                 BisqEasyService bisqEasyService,
                                                                 UserIdentityService userIdentityService,
                                                                 UserProfileService userProfileService,
                                                                 MarketPriceService marketPriceService,
                                                                 BisqEasyOffer peersOffer) {
        // From v2.1.1 on, we check if the max or fix amount of the offer is not exceeding the reputation-based limit.
        // Otherwise, we stick with the pre v2.1.1 checks, so that old offers do not pose a security risk.
        UserProfile makersUserProfile = userProfileService.findUserProfile(peersOffer.getMakersUserProfileId()).orElseThrow();
        ReputationScore makersReputationScore = reputationService.getReputationScore(makersUserProfile);

        Market market = peersOffer.getMarket();
        String offerId = peersOffer.getId();
        if (peersOffer.getTakersDirection().isBuy()) {
            // Taker as buyer
            long makersMaxAllowedQuoteSideTradeAmount = getMaxQuoteSideTradeAmount(marketPriceService, market, makersReputationScore)
                    .map(Monetary::getValue)
                    .orElse(0L);
            long offersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer)
                    .map(Monetary::getValue)
                    .orElse(0L);
            if (makersMaxAllowedQuoteSideTradeAmount >= offersQuoteSideMaxOrFixedAmount) {
                return OfferAmountLimitsResult.TAKER_AS_BUYER_AMOUNT_MATCH_LIMIT;
            } else {
                double makersLimitWithTolerance = MathUtils.roundDoubleToLong(makersMaxAllowedQuoteSideTradeAmount * (1 + TOLERANCE));
                if (makersLimitWithTolerance >= offersQuoteSideMaxOrFixedAmount) {
                    log.info("QuoteSideMaxOrFixedAmount of offer with ID {} is below the makersMaxAllowedQuoteSideTradeAmount with a 10% tolerance. " +
                                    "makersLimitWithTolerance={}; offersQuoteSideMaxOrFixedAmount={}",
                            offerId, makersLimitWithTolerance, offersQuoteSideMaxOrFixedAmount);
                    return OfferAmountLimitsResult.TAKER_AS_BUYER_AMOUNT_MATCH_TOLERANCE;
                } else {
                    // TODO Can be removed once most users have updated to 2.1.1.
                    // We have either a pre v2.1.1 offer or the offer was manipulated with a higher as allowed amount and is outside the tolerance (or the
                    // reputation data was not well distributed, resulting in different reputation scores of me and the peer).
                    log.warn("quoteSideMaxOrFixedAmount of offer with ID {} is above the makersMaxAllowedQuoteSideTradeAmount with a 10% tolerance. " +
                                    "makersLimitWithTolerance={}; offersQuoteSideMaxOrFixedAmount={}",
                            offerId, makersLimitWithTolerance, offersQuoteSideMaxOrFixedAmount);
                    long makerAsSellersScore = reputationService.getReputationScore(makersUserProfile).getTotalScore();
                    long myMinRequiredScore = bisqEasyService.getMinRequiredReputationScore().get();
                    boolean result = makerAsSellersScore >= myMinRequiredScore;
                    if (result) {
                        return OfferAmountLimitsResult.TAKER_AS_BUYER_AMOUNT_MATCH_MIN_REQ_REP_SCORE;
                    } else {
                        log.warn("makerAsSellersScore of offer with ID {} is below myMinRequiredScore. " +
                                        "makerAsSellersScore={}; myMinRequiredScore={}; makersMaxAllowedQuoteSideTradeAmount={}",
                                offerId, makerAsSellersScore, myMinRequiredScore, makersMaxAllowedQuoteSideTradeAmount);
                        return OfferAmountLimitsResult.TAKER_AS_BUYER_AMOUNT_NOT_MATCH_MIN_REQ_REP_SCORE;
                    }
                }
            }
        } else {
            // Taker as seller
            ReputationScore myReputationScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile());
            long myMaxAllowedQuoteSideTradeAmount = getMaxQuoteSideTradeAmount(marketPriceService, market, myReputationScore)
                    .map(Monetary::getValue)
                    .orElse(0L);
            long offersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer)
                    .map(Monetary::getValue)
                    .orElse(0L);

            if (myMaxAllowedQuoteSideTradeAmount >= offersQuoteSideMinOrFixedAmount) {
                return OfferAmountLimitsResult.TAKER_AS_SELLER_AMOUNT_MATCH_LIMIT;
            } else {
                double myLimitWithTolerance = MathUtils.roundDoubleToLong(myMaxAllowedQuoteSideTradeAmount * (1 + TOLERANCE));
                if (myLimitWithTolerance >= offersQuoteSideMinOrFixedAmount) {
                    log.info("QuoteSideMinOrFixedAmount of offer with ID {} is below the myMaxAllowedQuoteSideTradeAmount with a 10% tolerance. " +
                                    "myLimitWithTolerance={}; offersQuoteSideMinOrFixedAmount={}",
                            offerId, myLimitWithTolerance, offersQuoteSideMinOrFixedAmount);
                    return OfferAmountLimitsResult.TAKER_AS_SELLER_AMOUNT_MATCH_TOLERANCE;
                } else {
                    // TODO Can be removed once most users have updated to 2.1.1.
                    // We have either a pre v2.1.1 offer or the offer was manipulated with a higher as allowed min amount and is outside the tolerance (or the
                    // reputation data was not well distributed, resulting in different reputation scores of me and the peer).
                    log.warn("QuoteSideMinOrFixedAmount of offer with ID {} is above the myMaxAllowedQuoteSideTradeAmount with a 10% tolerance. " +
                                    "myLimitWithTolerance={}; offersQuoteSideMinOrFixedAmount={}",
                            offerId, myLimitWithTolerance, offersQuoteSideMinOrFixedAmount);
                    long myScoreAsSeller = myReputationScore.getTotalScore();
                    long offersRequiredScore = OfferOptionUtil.findRequiredTotalReputationScore(peersOffer).orElse(0L);
                    if (myScoreAsSeller >= offersRequiredScore) {
                        return OfferAmountLimitsResult.TAKER_AS_SELLER_AMOUNT_MATCH_MIN_REQ_REP_SCORE;
                    } else {
                        log.warn("RequiredScore of offer with ID {} is above myScoreAsSeller. " +
                                        "offersRequiredScore={}; myScoreAsSeller={}",
                                offerId, offersRequiredScore, myScoreAsSeller);
                        return OfferAmountLimitsResult.TAKER_AS_SELLER_AMOUNT_NOT_MATCH_MIN_REQ_REP_SCORE;
                    }
                }
            }
        }
    }

    @Getter
    public enum OfferAmountLimitsResult {
        TAKER_AS_BUYER_AMOUNT_MATCH_LIMIT,
        TAKER_AS_BUYER_AMOUNT_MATCH_TOLERANCE,
        TAKER_AS_BUYER_AMOUNT_MATCH_MIN_REQ_REP_SCORE,
        TAKER_AS_BUYER_AMOUNT_NOT_MATCH_MIN_REQ_REP_SCORE(false),

        TAKER_AS_SELLER_AMOUNT_MATCH_LIMIT,
        TAKER_AS_SELLER_AMOUNT_MATCH_TOLERANCE,
        TAKER_AS_SELLER_AMOUNT_MATCH_MIN_REQ_REP_SCORE,
        TAKER_AS_SELLER_AMOUNT_NOT_MATCH_MIN_REQ_REP_SCORE(false);

        private final boolean isValid;

        OfferAmountLimitsResult() {
            this(true);
        }

        OfferAmountLimitsResult(boolean isValid) {
            this.isValid = isValid;
        }
    }

}
