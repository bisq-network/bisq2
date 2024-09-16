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
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyTradeAmountLimits {
    public static final Coin DEFAULT_MIN_BTC_TRADE_AMOUNT = Coin.asBtcFromValue(10000); // 0.0001 BTC
    public static final Coin DEFAULT_MAX_BTC_TRADE_AMOUNT = Coin.asBtcFromValue(250000); // 0.0025 BTC // 150 USD @ 60k price
    public static final Fiat DEFAULT_MIN_USD_TRADE_AMOUNT = Fiat.fromFaceValue(6, "USD");
    public static final Fiat MAX_USD_TRADE_AMOUNT = Fiat.fromFaceValue(600, "USD");
    public static final Fiat MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION = Fiat.fromFaceValue(25, "USD");
    private static final double REQUIRED_REPUTAION_SCORE_PER_USD = 200d;
    public static final long MIN_REPUTAION_SCORE = 5000;
    public static final double TOLERANCE = 0.05;

    public static Optional<Monetary> getMinQuoteSideTradeAmount(MarketPriceService marketPriceService, Market market) {
        return marketPriceService.findMarketPriceQuote(MarketRepository.getUSDBitcoinMarket())
                .map(priceQuote -> priceQuote.toBaseSideMonetary(DEFAULT_MIN_USD_TRADE_AMOUNT))
                .flatMap(defaultMinBtcTradeAmount -> marketPriceService.findMarketPriceQuote(market)
                        .map(priceQuote -> priceQuote.toQuoteSideMonetary(defaultMinBtcTradeAmount)));
    }

    public static Optional<Monetary> getReputationBasedQuoteSideAmount(MarketPriceService marketPriceService,
                                                                       Market market,
                                                                       long myReputationScore) {
        Fiat maxUsdTradeAmount = getMaxUsdTradeAmount(myReputationScore);
        return marketPriceService.findMarketPriceQuote(MarketRepository.getUSDBitcoinMarket())
                .map(priceQuote -> priceQuote.toBaseSideMonetary(maxUsdTradeAmount))
                .flatMap(defaultMaxBtcTradeAmount -> marketPriceService.findMarketPriceQuote(market)
                        .map(priceQuote -> priceQuote.toQuoteSideMonetary(defaultMaxBtcTradeAmount)));
    }

    // TODO add BSQ/USD price into calculation to take into account the value of the investment (at burn time)
    private static Fiat getMaxUsdTradeAmount(long totalScore) {
        // A reputation score of 30k gives a max trade amount of 150 USD
        // Upper limit is 600 USD
        long value = Math.min(MAX_USD_TRADE_AMOUNT.getValue(), MathUtils.roundDoubleToLong(totalScore / REQUIRED_REPUTAION_SCORE_PER_USD));
        Fiat maxUsdTradeAmount = Fiat.fromFaceValue(value, "USD");

        // We tolerate up to 25 USD trade amount for users with no or low reputation (< 5000)
        if (maxUsdTradeAmount.isLessThan(MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION)) {
            return MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION;
        }
        return maxUsdTradeAmount;
    }


    public static Optional<Result> checkOfferAmountLimitForGivenAmount(ReputationService reputationService,
                                                                       UserIdentityService userIdentityService,
                                                                       UserProfileService userProfileService,
                                                                       MarketPriceService marketPriceService,
                                                                       Market market,
                                                                       Monetary fiatAmount,
                                                                       BisqEasyOffer peersOffer) {
        return findRequiredReputationScoreByFiatAmount(marketPriceService, market, fiatAmount)
                .map(requiredReputationScore -> {
                    long sellersReputationScore = getSellersReputationScore(reputationService, userIdentityService, userProfileService, peersOffer);
                    return getResult(sellersReputationScore, requiredReputationScore);
                });
    }

    public static Optional<Result> checkOfferAmountLimitForMinAmount(ReputationService reputationService,
                                                                     UserIdentityService userIdentityService,
                                                                     UserProfileService userProfileService,
                                                                     MarketPriceService marketPriceService,
                                                                     BisqEasyOffer peersOffer) {
        return findRequiredReputationScoreForMinAmount(marketPriceService, peersOffer)
                .map(requiredReputationScore -> {
                    long sellersReputationScore = getSellersReputationScore(reputationService, userIdentityService, userProfileService, peersOffer);
                    return getResult(sellersReputationScore, requiredReputationScore);
                });
    }

    public static Optional<Result> checkOfferAmountLimitForMaxOrFixedAmount(ReputationService reputationService,
                                                                            UserIdentityService userIdentityService,
                                                                            UserProfileService userProfileService,
                                                                            MarketPriceService marketPriceService,
                                                                            BisqEasyOffer peersOffer) {
        return findRequiredReputationScoreForMaxOrFixedAmount(marketPriceService, peersOffer)
                .map(requiredReputationScore -> {
                    long sellersReputationScore = getSellersReputationScore(reputationService, userIdentityService, userProfileService, peersOffer);
                    return getResult(sellersReputationScore, requiredReputationScore);
                });
    }

    public static long getSellersReputationScore(ReputationService reputationService,
                                                 UserIdentityService userIdentityService,
                                                 UserProfileService userProfileService,
                                                 BisqEasyOffer peersOffer) {
        UserProfile sellersUserProfile = peersOffer.getTakersDirection().isBuy()
                ? userProfileService.findUserProfile(peersOffer.getMakersUserProfileId()).orElseThrow()
                : userIdentityService.getSelectedUserIdentity().getUserProfile();
        return reputationService.getReputationScore(sellersUserProfile).getTotalScore();
    }

    private static Result getResult(long sellersReputationScore,
                                    long requiredReputationScore) {
        Result result;
        if (sellersReputationScore >= requiredReputationScore) {
            result = Result.MATCH_SCORE;
        } else if (withTolerance(sellersReputationScore) >= requiredReputationScore) {
            result = Result.MATCH_TOLERATED_SCORE;
        } else if (requiredReputationScore <= MIN_REPUTAION_SCORE) {
            result = Result.MATCH_MIN_SCORE;
        } else {
            result = Result.SCORE_TOO_LOW;
        }
        result.setRequiredReputationScore(requiredReputationScore);
        return result;
    }

    public static Optional<Long> findRequiredReputationScoreForMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                                                BisqEasyOffer offer) {
        return OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, offer)
                .flatMap(fiatAmount -> findRequiredReputationScoreByFiatAmount(marketPriceService, offer.getMarket(), fiatAmount));
    }

    private static Optional<Long> findRequiredReputationScoreForMinAmount(MarketPriceService marketPriceService,
                                                                          BisqEasyOffer offer) {
        return OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, offer)
                .flatMap(fiatAmount -> findRequiredReputationScoreByFiatAmount(marketPriceService, offer.getMarket(), fiatAmount));
    }

    public static Optional<Long> findRequiredReputationScoreByFiatAmount(MarketPriceService marketPriceService,
                                                                         Market market,
                                                                         Monetary fiatAmount) {
        return fiatToBtc(marketPriceService, market, fiatAmount)
                .flatMap(btc -> btcToUsd(marketPriceService, btc))
                .map(BisqEasyTradeAmountLimits::getRequiredReputationScoreByUsdAmount);
    }

    private static Optional<Monetary> fiatToBtc(MarketPriceService marketPriceService,
                                                Market market,
                                                Monetary fiatAmount) {
        return marketPriceService.findMarketPriceQuote(market)
                .map(btcFiatPriceQuote -> btcFiatPriceQuote.toBaseSideMonetary(fiatAmount));
    }

    private static Optional<Monetary> usdToBtc(MarketPriceService marketPriceService, Monetary usdAmount) {
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        return fiatToBtc(marketPriceService, usdBitcoinMarket, usdAmount);
    }

    private static Optional<Monetary> btcToFiat(MarketPriceService marketPriceService,
                                                Market market,
                                                Monetary btcAmount) {
        return marketPriceService.findMarketPriceQuote(market)
                .map(priceQuote -> priceQuote.toQuoteSideMonetary(btcAmount));
    }

    private static Optional<Monetary> btcToUsd(MarketPriceService marketPriceService, Monetary btcAmount) {
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        return btcToFiat(marketPriceService, usdBitcoinMarket, btcAmount);
    }

    public static Optional<Monetary> usdToFiat(MarketPriceService marketPriceService,
                                               Market market,
                                               Monetary usdAmount) {
        return usdToBtc(marketPriceService, usdAmount).
                flatMap(btc -> btcToFiat(marketPriceService, market, btc));
    }

    public static Optional<Monetary> fiatToUsd(MarketPriceService marketPriceService,
                                               Market market,
                                               Monetary fiatAmount) {
        return fiatToBtc(marketPriceService, market, fiatAmount).
                flatMap(btc -> btcToUsd(marketPriceService, btc));
    }


    public static long getRequiredReputationScoreByUsdAmount(Monetary usdAmount) {
        double faceValue = Monetary.toFaceValue(usdAmount.round(0), 0);
        return MathUtils.roundDoubleToLong(faceValue * REQUIRED_REPUTAION_SCORE_PER_USD);
    }

    public static Monetary getUsdAmountFromReputationScore(long reputationScore) {
        long usdAmount = MathUtils.roundDoubleToLong(reputationScore / REQUIRED_REPUTAION_SCORE_PER_USD);
        return Fiat.fromFaceValue(usdAmount, "USD");
    }

    public static long withTolerance(long makersReputationScore) {
        return MathUtils.roundDoubleToLong(makersReputationScore * (1 + TOLERANCE));
    }


    @Getter
    public enum Result {
        MATCH_SCORE,
        MATCH_TOLERATED_SCORE,
        MATCH_MIN_SCORE,
        SCORE_TOO_LOW;

        @Setter
        private Long requiredReputationScore;

        public boolean isValid() {
            return this != SCORE_TOO_LOW;
        }
    }
}
