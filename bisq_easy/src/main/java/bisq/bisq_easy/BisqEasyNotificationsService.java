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
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.presentation.notifications.NotificationsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class BisqEasyNotificationsService implements Service {
    private final NotificationsService notificationsService;

    @Getter
    private final Observable<Boolean> isNotificationPanelVisible = new Observable<>();
    @Getter
    private final ObservableSet<String> tradeIdsOfNotifications = new ObservableSet<>();

    public BisqEasyNotificationsService(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        notificationsService.subscribe(changed -> {
            tradeIdsOfNotifications.setAll(notificationsService.getNotConsumedNotificationIds().stream()
                    .filter(id -> ChatNotificationService.getChatChannelDomain(id) == ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                    .flatMap(id -> ChatNotificationService.findTradeId(id).stream())
                    .collect(Collectors.toSet()));
            // Reset dismissed state if we get a new notification
            if (!tradeIdsOfNotifications.isEmpty()) {
                notificationsService.getIsNotificationPanelDismissed().set(false);
            }
            updateNotificationVisibilityState();
        });

        notificationsService.getIsNotificationPanelDismissed().addObserver(isNotificationPanelDismissed -> updateNotificationVisibilityState());

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    private void updateNotificationVisibilityState() {
        isNotificationPanelVisible.set(!notificationsService.getIsNotificationPanelDismissed().get() &&
                !tradeIdsOfNotifications.isEmpty());
    }
}