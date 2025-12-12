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

import bisq.chat.ChatChannelDomain;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.support.mediation.MediatorService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BisqEasyNotificationsService implements Service {
    private final ChatNotificationService chatNotificationService;
    private final MediatorService mediatorService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final SettingsService settingsService;
    private final BisqEasyTradeService bisqEasyTradeService;
    @Nullable
    private Pin tradesPin, channelsPin, cookieChangedPin, favouriteMarketsPin;
    @Getter
    private final Observable<Boolean> isNotificationPanelVisible = new Observable<>(false);
    @Getter
    private final ObservableSet<ChatNotification> tradeNotifications = new ObservableSet<>();
    // We do not persist the state of a closed notification panel as we prefer to show the panel again at restart.
    // If any new notification gets added the panel will also be shown again.
    private final ObservableSet<ChatNotification> dismissedNotifications = new ObservableSet<>();

    private final Set<ChatNotification> orphanedNotifications = new CopyOnWriteArraySet<>();

    public BisqEasyNotificationsService(ChatNotificationService chatNotificationService,
                                        MediatorService mediatorService,
                                        BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService,
                                        SettingsService settingsService,
                                        BisqEasyTradeService bisqEasyTradeService,
                                        BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService) {
        this.chatNotificationService = chatNotificationService;
        this.mediatorService = mediatorService;
        this.bisqEasyOfferbookChannelService = bisqEasyOfferbookChannelService;
        this.settingsService = settingsService;
        this.bisqEasyTradeService = bisqEasyTradeService;
        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        tradesPin = bisqEasyTradeService.getTrades().addObserver(this::handleTradesOrTradeChannelsChange);
        channelsPin = bisqEasyOpenTradeChannelService.getChannels().addObserver(this::handleTradesOrTradeChannelsChange);

        cookieChangedPin = settingsService.getCookieChanged().addObserver(cookieChanged ->
                updateBisqEasyOfferbookPredicate());
        favouriteMarketsPin = settingsService.getFavouriteMarkets().addObserver(this::updateBisqEasyOfferbookPredicate);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");

        if (tradesPin != null) {
            tradesPin.unbind();
            tradesPin = null;
        }

        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }

        if (cookieChangedPin != null) {
            cookieChangedPin.unbind();
            cookieChangedPin = null;
        }

        if (favouriteMarketsPin != null) {
            favouriteMarketsPin.unbind();
            favouriteMarketsPin = null;
        }

        return CompletableFuture.completedFuture(true);
    }

    public boolean isMediatorsNotification(ChatNotification notification) {
        if (notification != null && notification.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OPEN_TRADES) {
            Optional<UserIdentity> myMediatorUserIdentity = mediatorService.findMyMediatorUserIdentity(notification.getMediator());
            return myMediatorUserIdentity.isPresent();
        } else {
            return false;
        }
    }

    public Stream<ChatNotification> getMediatorsNotConsumedNotifications() {
        return chatNotificationService.getNotConsumedNotifications().filter(this::isMediatorsNotification);
    }

    public boolean hasMediatorNotConsumedNotifications() {
        return getMediatorsNotConsumedNotifications().findAny().isPresent();
    }

    public boolean hasTradeIdsOfNotConsumedNotifications() {
        return getTradeIdsOfNotConsumedNotifications().findAny().isPresent();
    }

    public Stream<String> getTradeIdsOfNotConsumedNotifications() {
        return chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .filter(notification -> !isMediatorsNotification(notification))
                .flatMap(chatNotification -> chatNotification.getTradeId().stream());
    }

    public long getNumNotifications(Set<ChatChannelDomain> chatChannelDomains) {
        return chatChannelDomains.stream()
                .flatMap(chatNotificationService::getNotConsumedNotifications)
                .count();
    }

    public long getNumNotificationsForDomains(Set<ChatChannelDomain> domains) {
        return chatNotificationService.getNotConsumedNotifications()
                .filter(notification -> domains.contains(notification.getChatChannelDomain()))
                .count();
    }

    public void dismissNotification() {
        dismissedNotifications.addAll(tradeNotifications);
        tradeNotifications.clear();
        updateNotificationVisibilityState();
    }

    private void handleNotification(@Nullable ChatNotification notification) {
        if (notification != null) {
            processNotifications(chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.BISQ_EASY_OPEN_TRADES));
        }
    }

    private void handleTradesOrTradeChannelsChange() {
        Set<ChatNotification> allNotifications = new HashSet<>(orphanedNotifications);
        Set<ChatNotification> unconsumedNotifications = chatNotificationService
                .getNotConsumedNotifications(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .collect(Collectors.toSet());
        allNotifications.addAll(unconsumedNotifications);
        processNotifications(allNotifications.stream());
    }

    private void processNotifications(Stream<ChatNotification> notifications) {
        tradeNotifications.setAll(notifications
                .filter(chatNotification -> !dismissedNotifications.contains(chatNotification))
                .flatMap(notification -> {
                    if (notification.getTradeId()
                            .flatMap(bisqEasyTradeService::findTrade)
                            .filter(trade -> bisqEasyOpenTradeChannelService.findChannelByTradeId(trade.getId()).isPresent())
                            .isPresent()) {
                        orphanedNotifications.remove(notification);
                        return Stream.of(notification);
                    } else {
                        // In case we received a notification without the associated trade or trade channel present, we
                        // store it for later reprocessing when the trade or trade channel changes, to allow to apply
                        // the notification. This can happen if the notification arrives before we have received the
                        // messages which trigger the trade or trade channel creation.
                        // Besides that, we might receive notifications for closed trades, those we would ignore.
                        orphanedNotifications.add(notification);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet()));
        updateNotificationVisibilityState();
    }

    private void updateNotificationVisibilityState() {
        isNotificationPanelVisible.set(!tradeNotifications.isEmpty());
    }

    private void updateBisqEasyOfferbookPredicate() {
        String cookie = settingsService.getCookie().asString(CookieKey.MARKETS_FILTER).orElse(null);
        boolean isFavoritesOnlyFilterSet = BisqEasyMarketFilter.FAVOURITES.name().equals(cookie);
        boolean isMarketsWithOffersFilterSet = BisqEasyMarketFilter.WITH_OFFERS.name().equals(cookie);

        if (!isFavoritesOnlyFilterSet && !isMarketsWithOffersFilterSet) {
            // No filter selected, we show all
            chatNotificationService.putPredicate(ChatChannelDomain.BISQ_EASY_OFFERBOOK, notification -> true);
        } else {
            Predicate<ChatNotification> favouriteMarketsPredicate = notification -> bisqEasyOfferbookChannelService.findChannel(notification.getChatChannelId())
                    .map(BisqEasyOfferbookChannel::getMarket)
                    .map(market -> settingsService.getFavouriteMarkets().stream().anyMatch(m -> m.equals(market)))
                    .orElse(false);

            if (isFavoritesOnlyFilterSet) {
                // We show only favorites
                chatNotificationService.putPredicate(ChatChannelDomain.BISQ_EASY_OFFERBOOK, favouriteMarketsPredicate);
            } else {
                // We show markets with offers + favorites
                Predicate<ChatNotification> predicate = notification -> favouriteMarketsPredicate.test(notification) ||
                        getMarketsWithOffersPredicate(bisqEasyOfferbookChannelService).test(notification);
                chatNotificationService.putPredicate(ChatChannelDomain.BISQ_EASY_OFFERBOOK, predicate);
            }
        }
    }

    private static Predicate<ChatNotification> getMarketsWithOffersPredicate(BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService) {
        return notification -> bisqEasyOfferbookChannelService.findChannel(notification.getChatChannelId())
                .map(channel -> channel.getChatMessages().stream().anyMatch(BisqEasyOfferbookMessage::hasBisqEasyOffer))
                .orElse(false);
    }
}