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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.DateTableItem;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class OpenTradeListItem implements DateTableItem {
    @EqualsAndHashCode.Include
    private final BisqEasyOpenTradeChannel channel;
    @EqualsAndHashCode.Include
    private final BisqEasyTrade trade;

    private final UserProfile myUserProfile, peersUserProfile;
    private final String offerId, tradeId, shortTradeId, myUserName, directionalTitle, peersUserName, dateString, timeString,
            market, priceString, baseAmountString, quoteAmountString, myRole, bitcoinSettlementMethod,
            fiatPaymentMethod;
    private final long date, price, baseAmount, quoteAmount;
    private final ChatNotificationService chatNotificationService;
    private final ReputationScore reputationScore;
    private final StringProperty peerNumNotificationsProperty = new SimpleStringProperty();
    private final StringProperty mediatorNumNotificationsProperty = new SimpleStringProperty();
    private final Pin changedChatNotificationPin, isInMediationPin;
    private final BitcoinPaymentRail bitcoinPaymentRail;
    private final FiatPaymentRail fiatPaymentRail;
    private final boolean isFiatPaymentMethodCustom;

    private long peerNumNotifications, mediatorNumNotifications;
    private String mediatorUserName = "";
    private boolean isInMediation;

    public OpenTradeListItem(BisqEasyOpenTradeChannel channel,
                             BisqEasyTrade trade,
                             ReputationService reputationService,
                             ChatNotificationService chatNotificationService,
                             UserProfileService userProfileService) {
        this.channel = channel;
        this.trade = trade;

        myUserProfile = userProfileService.getManagedUserProfile(channel.getMyUserIdentity().getUserProfile());
        peersUserProfile = userProfileService.getManagedUserProfile(channel.getPeer());
        this.chatNotificationService = chatNotificationService;
        peersUserName = peersUserProfile.getUserName();
        myUserName = channel.getMyUserIdentity().getUserName();
        directionalTitle = BisqEasyTradeFormatter.getDirectionalTitle(trade);
        offerId = channel.getBisqEasyOffer().getId();
        this.tradeId = trade.getId();
        shortTradeId = trade.getShortId();

        BisqEasyContract contract = trade.getContract();
        date = contract.getTakeOfferDate();
        dateString = DateFormatter.formatDate(date);
        timeString = DateFormatter.formatTime(date);
        market = trade.getOffer().getMarket().toString();
        price = BisqEasyTradeUtils.getPriceQuote(trade).getValue();
        priceString = BisqEasyTradeFormatter.formatPriceWithCode(trade);
        baseAmount = contract.getBaseSideAmount();
        baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
        quoteAmount = contract.getQuoteSideAmount();
        quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(trade);
        bitcoinPaymentRail = contract.getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
        fiatPaymentRail = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
        bitcoinSettlementMethod = contract.getBaseSidePaymentMethodSpec().getShortDisplayString();
        fiatPaymentMethod = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();
        isFiatPaymentMethodCustom = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod().isCustomPaymentMethod();

        myRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
        reputationScore = reputationService.getReputationScore(peersUserProfile);

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        isInMediationPin = channel.isInMediationObservable().addObserver(isInMediation -> {
            if (isInMediation == null) {
                return;
            }
            this.isInMediation = isInMediation;
            if (isInMediation) {
                mediatorUserName = channel.getMediator().map(UserProfile::getUserName).orElse("");
            }
        });
    }

    public void dispose() {
        changedChatNotificationPin.unbind();
        isInMediationPin.unbind();
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || !notification.getChatChannelId().equals(channel.getId())) {
            return;
        }
        UIThread.run(() -> {
            boolean isSenderMediator = notification.getSenderUserProfile().equals(channel.getMediator());
            boolean isNotificationFromMediator = notification.getMediator().equals(notification.getSenderUserProfile());
            long numNotifications = chatNotificationService.getNumNotifications(channel);
            if (isSenderMediator && isNotificationFromMediator) {
                mediatorNumNotifications = numNotifications - peerNumNotifications;
                String value = mediatorNumNotifications > 0 ? String.valueOf(mediatorNumNotifications) : "";
                mediatorNumNotificationsProperty.set(value);
            } else {
                peerNumNotifications = numNotifications - mediatorNumNotifications;
                String value = peerNumNotifications > 0 ? String.valueOf(peerNumNotifications) : "";
                peerNumNotificationsProperty.set(value);
            }
        });
    }
}
