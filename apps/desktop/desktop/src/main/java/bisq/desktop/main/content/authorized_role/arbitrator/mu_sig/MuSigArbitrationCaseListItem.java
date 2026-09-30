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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.table.ActivatableTableItem;
import bisq.desktop.components.table.DateTableItem;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.support.arbitration.mu_sig.MuSigArbitrationCase;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.support.arbitration.mu_sig.MuSigArbitrationResult;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigArbitrationCaseListItem implements ActivatableTableItem, DateTableItem {
    @EqualsAndHashCode.Include
    private final MuSigArbitrationCase muSigArbitrationCase;
    private final ObjectProperty<Optional<MuSigOpenTradeChannel>> channel = new SimpleObjectProperty<>(Optional.empty());
    private final ChatNotificationService chatNotificationService;
    private final ReputationService reputationService;

    private final Trader maker, taker;
    private final long date, price, btcAmount, nonBtcAmount;
    private final String dateString, timeString, tradeId, shortTradeId, directionalTitle, market,
            priceString, btcAmountString, nonBtcAmountString, paymentMethod;
    private final boolean isMakerRequester;
    private final Badge makersBadge = new Badge();
    private final Badge takersBadge = new Badge();
    private Long closeCaseDate = 0L;
    private final StringProperty closeCaseDateString = new SimpleStringProperty("");
    private final StringProperty closeCaseTimeString = new SimpleStringProperty("");

    private Pin arbitratorHasLeftChatPin;
    private Pin changedChatNotificationPin;
    private Pin muSigArbitrationResultPin;

    MuSigArbitrationCaseListItem(ServiceProvider serviceProvider,
                                 MuSigArbitrationCase muSigArbitrationCase,
                                 Optional<MuSigOpenTradeChannel> channel) {
        this.muSigArbitrationCase = muSigArbitrationCase;
        this.channel.set(channel);

        reputationService = serviceProvider.getUserService().getReputationService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        MuSigArbitrationRequest arbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        MuSigContract contract = arbitrationRequest.getContract();
        MuSigOffer offer = contract.getOffer();
        List<UserProfile> traders = List.of(arbitrationRequest.getRequester(), arbitrationRequest.getPeer());

        Trader trader1 = new Trader(traders.get(0), reputationService);
        Trader trader2 = new Trader(traders.get(1), reputationService);
        if (offer.getMakerNetworkId().getId().equals(trader1.getUserProfile().getId())) {
            maker = trader1;
            taker = trader2;
        } else {
            maker = trader2;
            taker = trader1;
        }
        isMakerRequester = arbitrationRequest.getRequester().equals(maker.userProfile);

        tradeId = arbitrationRequest.getTradeId();
        shortTradeId = tradeId.substring(0, 8);
        directionalTitle = offer.getDirectionalTitle();
        date = contract.getTakeOfferDate();
        dateString = DateFormatter.formatDate(date);
        timeString = DateFormatter.formatTime(date);
        market = offer.getMarket().toString();
        price = MuSigTradeUtils.getPriceQuote(contract).getValue();
        priceString = MuSigTradeFormatter.formatPriceWithCode(contract);
        btcAmount = contract.getBtcSideAmount();
        btcAmountString = MuSigTradeFormatter.formatBtcSideAmount(contract);
        nonBtcAmount = contract.getNonBtcSideAmount();
        nonBtcAmountString = AmountFormatter.formatAmountWithCode(MuSigTradeUtils.getNonBtcSideMonetary(contract), true);
        paymentMethod = offer.getMarket().isBaseCurrencyBitcoin() ? contract.getNonBtcSidePaymentMethodSpec().getShortDisplayString() : "";

        onActivate();
    }

    @Override
    public void onActivate() {
        arbitratorHasLeftChatPin = muSigArbitrationCase.arbitratorHasLeftChatObservable().addObserver(hasLeftChat ->
                UIThread.run(() -> applyChannel(hasLeftChat)));
        muSigArbitrationResultPin = muSigArbitrationCase.muSigArbitrationResultObservable().addObserver(optionalResult ->
                UIThread.run(() -> applyCloseCaseDate(optionalResult.map(MuSigArbitrationResult::getDate))));

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    @Override
    public void onDeactivate() {
        if (arbitratorHasLeftChatPin != null) {
            arbitratorHasLeftChatPin.unbind();
            arbitratorHasLeftChatPin = null;
        }
        if (muSigArbitrationResultPin != null) {
            muSigArbitrationResultPin.unbind();
            muSigArbitrationResultPin = null;
        }
        changedChatNotificationPin.unbind();
    }

    public Optional<MuSigOpenTradeChannel> getChannel() {
        return channel.get();
    }

    public ReadOnlyObjectProperty<Optional<MuSigOpenTradeChannel>> channelProperty() {
        return channel;
    }

    private void applyChannel(boolean hasLeftChat) {
        // Leaving chat is one-way for the arbitrator UI. Once the case is marked as left,
        // we detach the channel and do not restore it from later state changes here.
        if (hasLeftChat) {
            this.channel.set(Optional.empty());
            makersBadge.setText("");
            takersBadge.setText("");
        }
    }

    public String getCloseCaseDateString() {
        return closeCaseDateString.get();
    }

    public StringProperty getCloseCaseDateStringProperty() {
        return closeCaseDateString;
    }

    public String getCloseCaseTimeString() {
        return closeCaseTimeString.get();
    }

    public StringProperty getCloseCaseTimeStringProperty() {
        return closeCaseTimeString;
    }

    private void applyCloseCaseDate(Optional<Long> optionalCloseCaseDate) {
        closeCaseDate = optionalCloseCaseDate.orElse(0L);
        closeCaseDateString.set(optionalCloseCaseDate.map(DateFormatter::formatDate).orElse(""));
        closeCaseTimeString.set(optionalCloseCaseDate.map(DateFormatter::formatTime).orElse(""));
    }

    private void handleNotification(ChatNotification notification) {
        Optional<MuSigOpenTradeChannel> currentChannel = channel.get();
        if (notification == null || currentChannel.isEmpty() || !notification.getChatChannelId().equals(currentChannel.orElseThrow().getId())) {
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
        return channel.get().map(chatNotificationService::getNotConsumedNotifications)
                .orElseGet(java.util.stream.Stream::empty)
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
                    .orElseGet(() -> Res.get("data.na"));
        }
    }
}
