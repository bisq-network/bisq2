package bisq.desktop.main.content.authorized_role.mediator;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.ActivatableTableItem;
import bisq.desktop.components.table.DateTableItem;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.presentation.notifications.NotificationsService;
import bisq.support.mediation.MediationCase;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
@EqualsAndHashCode
public class MediationCaseListItem implements ActivatableTableItem, DateTableItem {
    private final NotificationsService notificationsService;
    private final ChatNotificationService chatNotificationService;
    private final ReputationService reputationService;

    private final BisqEasyOpenTradeChannel channel;
    private final MediationCase mediationCase;
    private final Trader maker, taker;
    private final long date, price, baseAmount, quoteAmount;
    private final String dateString, timeString, tradeId, shortTradeId, offerId, direction, market,
            priceString, baseAmountString, quoteAmountString, paymentMethod;
    private final StringProperty numTradeNotification = new SimpleStringProperty();
    private final boolean isMakerRequester;

    public MediationCaseListItem(ServiceProvider serviceProvider,
                                 MediationCase mediationCase,
                                 BisqEasyOpenTradeChannel channel) {
        reputationService = serviceProvider.getUserService().getReputationService();
        notificationsService = serviceProvider.getNotificationsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();

        this.channel = channel;
        this.mediationCase = mediationCase;
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
        offerId = offer.getId();
        direction = BisqEasyTradeFormatter.getDirection(offer.getDirection());
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
    }

    @Override
    public void onActivate() {
        notificationsService.subscribe(this::updateNumNotifications);
    }

    @Override
    public void onDeactivate() {
        notificationsService.unsubscribe(this::updateNumNotifications);
    }

    private void updateNumNotifications(String notificationId) {
        UIThread.run(() -> {
            int numNotifications = chatNotificationService.getNumNotificationsByChannel(channel);
            numTradeNotification.set(numNotifications > 0 ?
                    String.valueOf(numNotifications) :
                    "");
        });
    }

    @Getter
    @EqualsAndHashCode
    static class Trader {
        private final String userName;
        private final UserProfile userProfile;
        private final String totalReputationScoreString;
        private final String profileAgeString;
        private final ReputationScore reputationScore;
        private final long totalReputationScore, profileAge;

        Trader(UserProfile userProfile, ReputationService reputationService) {
            this.userProfile = userProfile;
            userName = userProfile.getUserName();

            reputationScore = reputationService.getReputationScore(userProfile);
            totalReputationScore = reputationScore.getTotalScore();
            totalReputationScoreString = String.valueOf(reputationScore);

            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na"));
        }
    }
}
