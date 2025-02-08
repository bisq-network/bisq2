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
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
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
    private Pin userProfileIdWithScoreChangePin;

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

        userProfileIdWithScoreChangePin = reputationService.getUserProfileIdWithScoreChange().addObserver(this::userProfileIdWithScoreChanged);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        if (userProfileIdWithScoreChangePin != null) {
            userProfileIdWithScoreChangePin.unbind();
            userProfileIdWithScoreChangePin = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    // If not my message and if offer message we filter sell offers of makers with too low reputation
    // This was needed at the v2.1.4 update and can be removed later once no invalid offers are expected anymore.
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
        String offerId = bisqEasyOffer.getId();
        String makersUserProfileId = bisqEasyOffer.getMakersUserProfileId();
        if (useCache &&
                sellOffersWithInsufficientReputationByMakersProfileId.containsKey(makersUserProfileId) &&
                sellOffersWithInsufficientReputationByMakersProfileId.get(makersUserProfileId).contains(offerId)) {
            return false;
        }

        if (bisqEasyOffer.getDirection().isSell()) {
            Optional<Long> requiredReputationScoreForMaxOrFixedAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(marketPriceService, bisqEasyOffer);
            if (requiredReputationScoreForMaxOrFixedAmount.isPresent()) {
                Optional<Long> requiredReputationScoreForMinAmount = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinAmount(marketPriceService, bisqEasyOffer);
                long requiredReputationScoreForMaxOrFixed = requiredReputationScoreForMaxOrFixedAmount.get();
                long requiredReputationScoreForMinOrFixed = requiredReputationScoreForMinAmount.orElse(requiredReputationScoreForMaxOrFixed);
                long sellersScore = userProfileService.findUserProfile(makersUserProfileId)
                        .map(reputationService::getReputationScore)
                        .map(ReputationScore::getTotalScore)
                        .orElse(0L);
                boolean hasInsufficientReputation = BisqEasyTradeAmountLimits.withTolerance(sellersScore) < requiredReputationScoreForMinOrFixed;
                if (hasInsufficientReputation) {
                    if (useCache) {
                        sellOffersWithInsufficientReputationByMakersProfileId.putIfAbsent(makersUserProfileId, new HashSet<>());
                        sellOffersWithInsufficientReputationByMakersProfileId.get(makersUserProfileId).add(offerId);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private void userProfileIdWithScoreChanged(String userProfileId) {
        if (userProfileId != null) {
            // We remove the cached data if we get any change of the users reputation score
            sellOffersWithInsufficientReputationByMakersProfileId.remove(userProfileId);
        }
    }
}
