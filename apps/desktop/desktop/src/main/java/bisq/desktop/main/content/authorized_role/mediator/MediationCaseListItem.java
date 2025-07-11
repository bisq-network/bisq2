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

package bisq.desktop.main.content.authorized_role.mediator;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.table.ActivatableTableItem;
import bisq.desktop.components.table.DateTableItem;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.support.mediation.MediationCase;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MediationCaseListItem implements ActivatableTableItem, DateTableItem {
    @EqualsAndHashCode.Include
    private final MediationCase mediationCase;
    @EqualsAndHashCode.Include
    private final BisqEasyOpenTradeChannel channel;
    private final ChatNotificationService chatNotificationService;
    private final ReputationService reputationService;

    private final Trader maker, taker;
    private final long date, price, baseAmount, quoteAmount;
    private final String dateString, timeString, tradeId, shortTradeId, directionalTitle, market,
            priceString, baseAmountString, quoteAmountString, paymentMethod;
    private final boolean isMakerRequester;
    private final Badge makersBadge = new Badge();
    private final Badge takersBadge = new Badge();
    private Pin changedChatNotificationPin;
    private Long closeCaseDate = 0L;
    private String closeCaseDateString = "";
    private String closeCaseTimeString = "";

    MediationCaseListItem(ServiceProvider serviceProvider,
                          MediationCase mediationCase,
                          BisqEasyOpenTradeChannel channel) {
        this.mediationCase = mediationCase;
        this.channel = channel;

        reputationService = serviceProvider.getUserService().getReputationService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        BisqEasyContract contract = mediationCase.getMediationRequest().getContract();
        BisqEasyOffer offer = contract.getOffer();
        List<UserProfile> traders = new ArrayList<>(channel.getTraders());
        offer.getMakerNetworkId().getId();

        Trader trader1 = new Trader(traders.get(0), reputationService);
        Trader trader2 = new Trader(traders.get(1), reputationService);
        if (offer.getMakerNetworkId().getId().equals(trader1.getUserProfile().getId())) {
            maker = trader1;
            taker = trader2;
        } else {
            maker = trader2;
            taker = trader1;
        }
        isMakerRequester = mediationCase.getMediationRequest().getRequester().equals(maker.userProfile);

        tradeId = channel.getTradeId();
        shortTradeId = tradeId.substring(0, 8);
        directionalTitle = offer.getDirection().getDirectionalTitle();
        date = contract.getTakeOfferDate();
        dateString = DateFormatter.formatDate(date);
        timeString = DateFormatter.formatTime(date);
        market = offer.getMarket().toString();
        price = BisqEasyTradeUtils.getPriceQuote(contract).getValue();
        priceString = BisqEasyTradeFormatter.formatPriceWithCode(contract);
        baseAmount = contract.getBaseSideAmount();
        baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(contract);
        quoteAmount = contract.getQuoteSideAmount();
        quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(contract);
        paymentMethod = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();

        onActivate();
    }

    @Override
    public void onActivate() {
        Optional<Long> optionalCloseCaseDate = mediationCase.getCloseCaseDate();
        closeCaseDate = optionalCloseCaseDate.orElse(0L);
        closeCaseDateString = optionalCloseCaseDate.map(DateFormatter::formatDate).orElse("");
        closeCaseTimeString = optionalCloseCaseDate.map(DateFormatter::formatTime).orElse("");

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    @Override
    public void onDeactivate() {
        changedChatNotificationPin.unbind();
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || !notification.getChatChannelId().equals(channel.getId())) {
            return;
        }
        UIThread.run(() -> {
            long numNotificationsFromMaker = getNumNotifications(maker.getUserProfile());
            makersBadge.setText(numNotificationsFromMaker > 0 ?
                    String.valueOf(numNotificationsFromMaker) :
                    "");
            long numNotificationsFromTaker = getNumNotifications(taker.getUserProfile());
            takersBadge.setText(numNotificationsFromTaker > 0 ?
                    String.valueOf(numNotificationsFromTaker) :
                    "");
        });
    }

    private long getNumNotifications(UserProfile userProfile) {
        return chatNotificationService.getNotConsumedNotifications(channel)
                .filter(notification -> notification.getSenderUserProfile().isPresent())
                .filter(notification -> notification.getSenderUserProfile().get().equals(userProfile))
                .count();
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Trader {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;
        private final String userName;
        private final String totalReputationScoreString;
        private final String profileAgeString;
        private final ReputationScore reputationScore;
        private final long totalReputationScore, profileAge;

        Trader(UserProfile userProfile,
               ReputationService reputationService) {
            this.userProfile = userProfile;
            userName = userProfile.getUserName();

            reputationScore = reputationService.getReputationScore(userProfile);
            totalReputationScore = reputationScore.getTotalScore();
            totalReputationScoreString = String.valueOf(reputationScore);

            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDaysAndYears)
                    .orElse(Res.get("data.na"));
        }
    }
}
