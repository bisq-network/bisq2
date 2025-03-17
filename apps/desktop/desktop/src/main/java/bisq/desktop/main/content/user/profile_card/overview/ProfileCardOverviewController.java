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

package bisq.desktop.main.content.user.profile_card.overview;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ProfileCardOverviewController implements Controller {
    @Getter
    private final ProfileCardOverviewView view;
    private final ProfileCardOverviewModel model;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;

    private UIScheduler livenessUpdateScheduler;

    public ProfileCardOverviewController(ServiceProvider serviceProvider) {
        ChatService chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        reputationService = serviceProvider.getUserService().getReputationService();

        model = new ProfileCardOverviewModel();
        view = new ProfileCardOverviewView(model, this);
    }

    public void setUserProfile(UserProfile userProfile) {
        model.setUserProfile(userProfile);
        String terms = userProfile.getTerms();
        model.setTradeTerms(terms.isBlank() ? "-" : terms);
        model.setStatement(userProfile.getStatement().isBlank() ? "-" : userProfile.getStatement());

        String userProfileId = userProfile.getId();
        long totalBaseOfferAmountToBuy = getTotalBaseOfferAmount(userProfileId, offer -> offer.getDirection().isBuy());
        String formattedTotalBaseOfferAmountToBuy = AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(totalBaseOfferAmountToBuy));
        model.setTotalBaseOfferAmountToBuy(formattedTotalBaseOfferAmountToBuy);

        long totalBaseOfferAmountToSell = getTotalBaseOfferAmount(userProfileId, offer -> offer.getDirection().isSell());
        String formattedTotalBaseOfferAmountToSell = AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(totalBaseOfferAmountToSell));
        model.setTotalBaseOfferAmountToSell(formattedTotalBaseOfferAmountToSell);

        model.setSellingLimit(String.valueOf(AmountFormatter.formatQuoteAmount(getSellingAmountLimitInUsd(userProfileId))));

        model.setProfileAge(reputationService.getProfileAgeService().getProfileAge(userProfile)
                .map(TimeFormatter::formatAgeInDaysAndYears)
                .orElse(Res.get("data.na")));
    }

    @Override
    public void onActivate() {
        UserProfile userProfile = model.getUserProfile();

        if (livenessUpdateScheduler != null) {
            livenessUpdateScheduler.stop();
        }
        livenessUpdateScheduler = UIScheduler.run(() -> {
                    long publishDate = userProfile.getPublishDate();
                    if (publishDate == 0) {
                        model.getLastUserActivity().set(Res.get("data.na"));
                    } else {
                        long age = Math.max(0, System.currentTimeMillis() - publishDate);
                        model.getLastUserActivity().set(TimeFormatter.formatAgeCompact(age));
                    }
                })
                .periodically(0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onDeactivate() {
        if (livenessUpdateScheduler != null) {
            livenessUpdateScheduler.stop();
            livenessUpdateScheduler = null;
        }
    }

    private long getTotalBaseOfferAmount(String userProfileId, Predicate<BisqEasyOffer> predicate) {
        return getOffers(userProfileId)
                .map(message -> message.getBisqEasyOffer().orElseThrow())
                .filter(predicate)
                .flatMap(offer -> OfferAmountUtil.findBaseSideMaxOrFixedAmount(marketPriceService, offer).stream())
                .mapToLong(Monetary::getValue)
                .sum();
    }

    private Stream<BisqEasyOfferbookMessage> getOffers(String userProfileId) {
        return bisqEasyOfferbookChannelService.getChannels().stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                .filter(message -> message.getAuthorUserProfileId().equals(userProfileId));
    }

    private Monetary getSellingAmountLimitInUsd(String userProfileId) {
        long userReputationScore = reputationService.getReputationScore(userProfileId).getTotalScore();
        return BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(userReputationScore);
    }
}
