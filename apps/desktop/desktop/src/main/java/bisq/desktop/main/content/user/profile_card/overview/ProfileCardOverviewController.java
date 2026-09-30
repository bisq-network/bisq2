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
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfileCardOverviewController implements Controller {
    @Getter
    private final ProfileCardOverviewView view;
    private final ProfileCardOverviewModel model;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;
    private final UserProfileService userProfileService;

    private UIScheduler livenessUpdateScheduler;

    public ProfileCardOverviewController(ServiceProvider serviceProvider) {
        ChatService chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        reputationService = serviceProvider.getUserService().getReputationService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        model = new ProfileCardOverviewModel();
        view = new ProfileCardOverviewView(model, this);
    }

    public void setUserProfile(UserProfile userProfile) {
        model.setUserProfile(userProfile);
        String terms = userProfile.getTerms();
        model.setTradeTerms(terms.isBlank() ? "-" : terms);
        model.setStatement(userProfile.getStatement().isBlank() ? "-" : userProfile.getStatement());

        String userProfileId = userProfile.getId();
        // TODO Need to add support for swapping base and quote currency when crypto market is used
        model.setTotalBaseOfferAmountToBuy(formatTotalBaseOfferAmount(
                getTotalBaseOfferAmount(userProfileId, offer -> offer.getDisplayDirection().isBuy())));

        // TODO Need to add support for swapping base and quote currency when crypto market is used
        model.setTotalBaseOfferAmountToSell(formatTotalBaseOfferAmount(
                getTotalBaseOfferAmount(userProfileId, offer -> offer.getDisplayDirection().isSell())));

        model.setSellingLimit(String.valueOf(AmountFormatter.formatQuoteAmount(getSellingAmountLimitInUsd(userProfileId))));

        model.setProfileAge(reputationService.getProfileAgeService().getProfileAge(userProfile)
                .map(TimeFormatter::formatAgeInDaysAndYears)
                .orElseGet(() -> Res.get("data.na")));
    }

    @Override
    public void onActivate() {
        UserProfile userProfile = model.getUserProfile();

        if (livenessUpdateScheduler != null) {
            livenessUpdateScheduler.stop();
        }
        livenessUpdateScheduler = UIScheduler.run(() -> {
                    // We need to use the userprofile from our userProfileService as only
                    // that will have a publishDate (set by the p2p network storage layer).
                    // For userProfiles we have persisted in the contact list and which are
                    // expired (have not been online in the past 15 days), we set it
                    // to 0 and display "N/A".
                    long now = System.currentTimeMillis();
                    String lastUserActivity = userProfileService.findUserProfile(userProfile.getId())
                            .map(UserProfile::getPublishDate)
                            .filter(publishDate -> publishDate > 0)
                            .map(publishDate -> Math.max(0L, now - publishDate))
                            .map(TimeFormatter::formatAgeCompact)
                            .orElse(Res.get("data.na"));
                    model.getLastUserActivity().set(lastUserActivity);
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

    private Optional<Long> getTotalBaseOfferAmount(String userProfileId, Predicate<BisqEasyOffer> predicate) {
        List<Monetary> baseAmounts = getOffers(userProfileId)
                .map(message -> message.getBisqEasyOffer().orElseThrow())
                .filter(predicate)
                .flatMap(offer -> {
                    try {
                        return OfferAmountUtil.findBaseSideMaxOrFixedAmount(marketPriceService, offer).stream();
                    } catch (ArithmeticException e) {
                        // An offer whose amounts overflow at the current price cannot contribute
                        // to the total; it is hidden from the offer lists as invalid.
                        return Stream.<Monetary>empty();
                    }
                })
                .collect(Collectors.toList());
        return checkedBaseAmountSum(baseAmounts);
    }

    // Individually valid base amounts can still overflow a long in aggregate. Sum with
    // Math.addExact and treat an overflow as a value that cannot be represented.
    @VisibleForTesting
    static Optional<Long> checkedBaseAmountSum(List<Monetary> baseAmounts) {
        try {
            long total = 0L;
            for (Monetary baseAmount : baseAmounts) {
                long value = baseAmount.getValue();
                // Drop malformed non-positive per-offer amounts (e.g. a negative from a wrapped
                // conversion) so they cannot corrupt the total.
                if (value <= 0) {
                    continue;
                }
                total = Math.addExact(total, value);
            }
            return Optional.of(total);
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static String formatTotalBaseOfferAmount(Optional<Long> totalBaseOfferAmount) {
        return totalBaseOfferAmount
                .map(value -> AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(value)))
                .orElseGet(() -> Res.get("data.na"));
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
