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
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BisqEasySellersReputationBasedTradeAmountService implements Service {
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final MarketPriceService marketPriceService;
    private final Map<String, Set<String>> sellOffersWithInsufficientReputationByMakersProfileId = new ConcurrentHashMap<>();
    // Guards a computation which started before an invalidation from re-inserting a stale result.
    private final Object cacheInvalidationLock = new Object();
    private long cacheInvalidationCount;
    private Pin scoreByUserProfileIdPin, marketPriceByCurrencyMapPin;

    public BisqEasySellersReputationBasedTradeAmountService(UserProfileService userProfileService,
                                                            ReputationService reputationService,
                                                            MarketPriceService marketPriceService) {
        this.userProfileService = userProfileService;
        this.reputationService = reputationService;
        this.marketPriceService = marketPriceService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        // The score map notifies on every put, unlike the equality-gated score change observable
        // which stays silent when the same profile changes twice in a row.
        scoreByUserProfileIdPin = reputationService.getScoreByUserProfileId().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String userProfileId, Long score) {
                userProfileScoreChanged(userProfileId);
            }
        });
        // The required reputation score derives from the offer amount in USD, so cached results
        // become stale whenever market prices change.
        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap()
                .addObserver(this::marketPricesChanged);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        if (scoreByUserProfileIdPin != null) {
            scoreByUserProfileIdPin.unbind();
            scoreByUserProfileIdPin = null;
        }
        if (marketPriceByCurrencyMapPin != null) {
            marketPriceByCurrencyMapPin.unbind();
            marketPriceByCurrencyMapPin = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    // If not my message and if offer message we filter sell offers of makers with too low reputation
    // This was needed at the v2.1.6 update and can be removed later once no invalid offers are expected anymore.
    public boolean hasSellerSufficientReputation(ChatMessage chatMessage) {
        return hasSellerSufficientReputation(chatMessage, true);
    }

    public boolean hasSellerSufficientReputation(ChatMessage chatMessage, boolean useCache) {
        if (chatMessage instanceof BisqEasyOfferbookMessage message && message.getBisqEasyOffer().isPresent()) {
            return hasSellerSufficientReputation(message.getBisqEasyOffer().get(), useCache);
        } else {
            return true;
        }
    }

    public boolean hasSellerSufficientReputation(BisqEasyOffer bisqEasyOffer) {
        return hasSellerSufficientReputation(bisqEasyOffer, true);
    }

    private boolean hasSellerSufficientReputation(BisqEasyOffer bisqEasyOffer, boolean useCache) {
        if (bisqEasyOffer.getDirection().isBuy()) {
            return true;
        }

        String offerId = bisqEasyOffer.getId();
        String makersUserProfileId = bisqEasyOffer.getMakersUserProfileId();
        long cacheInvalidationCountAtStart;
        synchronized (cacheInvalidationLock) {
            cacheInvalidationCountAtStart = cacheInvalidationCount;
        }
        if (useCache) {
            // The computeIfPresent method invocation is performed atomically.
            Set<String> sellOffersWithInsufficientReputation = sellOffersWithInsufficientReputationByMakersProfileId
                    .computeIfPresent(makersUserProfileId, (k, set) ->
                            set.contains(offerId) ? set : null
                    );
            if (sellOffersWithInsufficientReputation != null) {
                return false;
            }
        }

        Optional<Long> requiredReputationScoreForMaxOrFixedAmount;
        Optional<Long> requiredReputationScoreForMinAmount;
        try {
            requiredReputationScoreForMaxOrFixedAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
            requiredReputationScoreForMinAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinAmount(marketPriceService, bisqEasyOffer);
        } catch (ArithmeticException e) {
            // The offer amount converts to the offer's fiat currency but overflows in USD, so the
            // required score cannot be determined at current prices.
            log.debug("Required reputation score for offer {} cannot be calculated at current prices", offerId);
            return false;
        }
        if (requiredReputationScoreForMaxOrFixedAmount.isPresent()) {
            long requiredReputationScoreForMaxOrFixed = requiredReputationScoreForMaxOrFixedAmount.get();
            long requiredReputationScoreForMinOrFixed = requiredReputationScoreForMinAmount.orElse(requiredReputationScoreForMaxOrFixed);
            long sellersScore = userProfileService.findUserProfile(makersUserProfileId)
                    .map(reputationService::getReputationScore)
                    .map(ReputationScore::getTotalScore)
                    .orElse(0L);
            boolean hasInsufficientReputation = BisqEasyTradeAmountLimits.withTolerance(sellersScore) < requiredReputationScoreForMinOrFixed;
            if (hasInsufficientReputation) {
                if (useCache) {
                    synchronized (cacheInvalidationLock) {
                        if (cacheInvalidationCountAtStart == cacheInvalidationCount) {
                            // The compute method invocation is performed atomically.
                            sellOffersWithInsufficientReputationByMakersProfileId
                                    .compute(makersUserProfileId, (k, set) -> {
                                        if (set == null) {
                                            set = ConcurrentHashMap.newKeySet();
                                        }
                                        set.add(offerId);
                                        return set;
                                    });
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }

    private void userProfileScoreChanged(String userProfileId) {
        // We remove the cached data if we get any change of the users reputation score
        synchronized (cacheInvalidationLock) {
            cacheInvalidationCount++;
            sellOffersWithInsufficientReputationByMakersProfileId.remove(userProfileId);
        }
    }

    private void marketPricesChanged() {
        synchronized (cacheInvalidationLock) {
            cacheInvalidationCount++;
            sellOffersWithInsufficientReputationByMakersProfileId.clear();
        }
    }
}
