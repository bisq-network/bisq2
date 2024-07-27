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
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.support.mediation.MediatorService;
import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BisqEasyNotificationsService implements Service {
    private final ChatNotificationService chatNotificationService;
    private final MediatorService mediatorService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final SettingsService settingsService;

    @Getter
    private final Observable<Boolean> isNotificationPanelVisible = new Observable<>();
    @Getter
    private final ObservableSet<String> tradeIdsOfNotifications = new ObservableSet<>();
    // We do not persist the state of a closed notification panel as we prefer to show the panel again at restart.
    // If any new notification gets added the panel will also be shown again.
    @Getter
    private final Observable<Boolean> isNotificationPanelDismissed = new Observable<>(false);

    public BisqEasyNotificationsService(ChatNotificationService chatNotificationService,
                                        MediatorService mediatorService,
                                        BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService,
                                        SettingsService settingsService) {
        this.chatNotificationService = chatNotificationService;
        this.mediatorService = mediatorService;
        this.bisqEasyOfferbookChannelService = bisqEasyOfferbookChannelService;
        this.settingsService = settingsService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotifications);
        chatNotificationService.getChangedNotification().addObserver(this::handleNotifications);

        isNotificationPanelDismissed.addObserver(isNotificationPanelDismissed -> updateNotificationVisibilityState());
        settingsService.getCookieChanged().addObserver(cookieChanged -> updateBisqEasyOfferbookPredicate());
        settingsService.getFavouriteMarkets().addObserver(this::updateBisqEasyOfferbookPredicate);

        return CompletableFuture.completedFuture(true);
    }

    private void handleNotifications(ChatNotification notification) {
        if (notification == null) {
            return;
        }

        tradeIdsOfNotifications.setAll(chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .flatMap(chatNotification -> chatNotification.getTradeId().stream())
                .collect(Collectors.toSet()));
        // Reset dismissed state if we get a new notification
        if (!tradeIdsOfNotifications.isEmpty()) {
            isNotificationPanelDismissed.set(false);
        }
        updateNotificationVisibilityState();
    }

    public CompletableFuture<Boolean> shutdown() {
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

    public long getNumNotifications(NavigationTarget navigationTarget) {
        return ChatChannelDomainNavigationTargetMapper.fromNavigationTarget(navigationTarget).stream()
                .flatMap(chatNotificationService::getNotConsumedNotifications)
                .count();
    }

    public long getNumNotificationsForDomains(Set<ChatChannelDomain> domains) {
        return chatNotificationService.getNotConsumedNotifications()
                .filter(notification -> domains.contains(notification.getChatChannelDomain()))
                .count();
    }

    private void updateNotificationVisibilityState() {
        isNotificationPanelVisible.set(!isNotificationPanelDismissed.get() &&
                !tradeIdsOfNotifications.isEmpty());
    }


    public void updateBisqEasyOfferbookPredicate() {
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