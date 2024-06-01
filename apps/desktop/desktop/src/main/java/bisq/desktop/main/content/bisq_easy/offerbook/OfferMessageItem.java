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
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OfferMessageItem {
    @EqualsAndHashCode.Include
    private final BisqEasyOfferbookMessage bisqEasyOfferbookMessage;
    private final BisqEasyOffer bisqEasyOffer;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;
    private final UserProfile userProfile;
    private final String userNickname;
    private final Pair<Monetary, Monetary> minMaxAmount;
    private final String minMaxAmountAsString;
    private final long lastSeen;
    private final String lastSeenAsString;
    private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
    private long totalScore;
    private double priceSpecAsPercent;
    private Pin marketPriceByCurrencyMapPin, reputationChangedPin;

    OfferMessageItem(BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                     UserProfile userProfile,
                     ReputationService reputationService,
                     MarketPriceService marketPriceService,
                     UserProfileService userProfileService) {
        this.bisqEasyOfferbookMessage = bisqEasyOfferbookMessage;
        this.bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().orElseThrow();
        this.userProfile = userProfile;
        this.reputationService = reputationService;
        this.marketPriceService = marketPriceService;
        userNickname = userProfile.getNickName();
        minMaxAmount = retrieveMinMaxAmount();
        minMaxAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, bisqEasyOffer, false);
        lastSeen = userProfileService.getLastSeen(userProfile);
        lastSeenAsString = TimeFormatter.formatAge(lastSeen);

        initialize();
    }

    void dispose() {
        marketPriceByCurrencyMapPin.unbind();
        reputationChangedPin.unbind();
    }

    Monetary getMinAmount() {
        return minMaxAmount.getFirst();
    }

    boolean isBuyOffer() {
        return bisqEasyOffer.getDirection() == Direction.BUY;
    }

    private void initialize() {
        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(this::updatePriceSpecAsPercent));
        updatePriceSpecAsPercent();

        reputationChangedPin = reputationService.getChangedUserProfileScore().addObserver(userProfileId ->
                UIThread.run(this::applyReputationScore));
        applyReputationScore();
    }

    private Pair<Monetary, Monetary> retrieveMinMaxAmount() {
        Monetary minAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow();
        Monetary maxAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow();
        return new Pair<>(minAmount, maxAmount);
    }

    private void updatePriceSpecAsPercent() {
        priceSpecAsPercent = PriceUtil.findPercentFromMarketPrice(marketPriceService, bisqEasyOffer).orElseThrow();
    }

    private void applyReputationScore() {
        reputationScore.set(reputationService.getReputationScore(userProfile));
        totalScore = reputationScore.get().getTotalScore();
    }
}
