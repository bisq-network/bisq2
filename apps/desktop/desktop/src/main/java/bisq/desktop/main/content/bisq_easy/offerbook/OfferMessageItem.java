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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.data.Pair;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
public class OfferMessageItem {
    private final BisqEasyOfferbookMessage message;
    private final BisqEasyOffer offer;
    private final MarketPriceService marketPriceService;

    private final Optional<UserProfile> senderUserProfile;
    private final String nickName;
    private final Pair<Monetary, Monetary> minMaxAmount;
    private final String minMaxAmountAsString;

    @EqualsAndHashCode.Exclude
    private final ReputationScore reputationScore;
    @EqualsAndHashCode.Exclude
    private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

    private Pin marketPriceByCurrencyMapPin;
    private double priceSpecAsPercent;

    OfferMessageItem(BisqEasyOfferbookMessage message,
                     BisqEasyOffer offer,
                     UserProfileService userProfileService,
                     ReputationService reputationService,
                     MarketPriceService marketPriceService) {
        this.message = message;
        this.offer = offer;
        this.marketPriceService = marketPriceService;
        senderUserProfile = userProfileService.findUserProfile(message.getAuthorUserProfileId());
        nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");
        reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);
        reputationScoreDisplay.setReputationScore(reputationScore);
        minMaxAmount = retrieveMinMaxAmount();
        minMaxAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, offer, false);

        initialize();
    }

    private void initialize() {
        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(this::updatePriceSpecAsPercent));
        updatePriceSpecAsPercent();
    }

    void dispose() {
        marketPriceByCurrencyMapPin.unbind();
    }

    private Pair<Monetary, Monetary> retrieveMinMaxAmount() {
        Monetary minAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, offer).orElseThrow();
        Monetary maxAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, offer).orElseThrow();
        return new Pair<>(minAmount, maxAmount);
    }

    private void updatePriceSpecAsPercent() {
        priceSpecAsPercent = PriceUtil.findPercentFromMarketPrice(marketPriceService, offer).orElseThrow();
    }
}
