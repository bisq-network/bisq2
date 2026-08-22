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

package bisq.notifications;


import bisq.bonded_roles.mobile_notification_relay.MobileNotificationRelayClient;
import bisq.common.application.Service;
import bisq.notifications.mobile.MobileNotificationService;
import bisq.notifications.system.OsSpecificNotificationService;
import bisq.notifications.system.SystemNotificationService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NotificationService implements Service {
    @Getter
    private final SystemNotificationService systemNotificationService;
    @Getter
    private final MobileNotificationService mobileNotificationService;

    public NotificationService(PersistenceService persistenceService,
                               MobileNotificationRelayClient mobileNotificationRelayClient,
                               Optional<OsSpecificNotificationService> systemNotificationDelegate) {
        systemNotificationService = new SystemNotificationService(systemNotificationDelegate);
        mobileNotificationService = new MobileNotificationService(persistenceService, mobileNotificationRelayClient);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return systemNotificationService.initialize()
                .thenCompose(e -> mobileNotificationService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return systemNotificationService.shutdown()
                .thenCompose(e -> mobileNotificationService.shutdown());
    }

    public void dispatchNotification(Notification notification) {
        dispatchNotification(notification, true);
    }

    /**
     * Dispatches a notification to the system path (desktop OS-native) and conditionally
     * to the mobile relay path (FCM / APNs) based on {@code mobileEligible}.
     * <p>
     * The mobile path delivers push notifications to registered devices via the bisq-relay,
     * which has no in-app grouping or dismissal affordances per event. Callers therefore
     * filter low-value, high-volume events (e.g. Bisq Easy trade protocol log messages) so
     * the mobile inbox is reserved for things the user actually needs to act on. Desktop
     * remains noisy-by-default because the chat view groups and lets users dismiss in-app.
     * <p>
     * See {@code bisq-network/bisq-mobile#1450}.
     *
     * @param notification    the notification to dispatch
     * @param mobileEligible  if {@code false}, the mobile relay dispatch is suppressed;
     *                        the system (desktop) dispatch still happens
     */
    public void dispatchNotification(Notification notification, boolean mobileEligible) {
        systemNotificationService.dispatchNotification(notification);
        if (mobileEligible) {
            mobileNotificationService.dispatchNotification(notification);
        } else {
            log.debug("Mobile relay suppressed for notification '{}' (not mobile-eligible)", notification.getTitle());
        }
    }

    /**
     * Dispatches a notification ONLY to the mobile relay path. Skips the desktop
     * system notification.
     * <p>
     * Used when the desktop already surfaces the same event through another channel
     * (e.g. Bisq Easy trade state transitions are visible to desktop users via the
     * in-app trade chat's protocol log messages, and the trade detail header — we
     * don't want a duplicate OS-level toast on desktop too). The mobile inbox has
     * no such in-app surface, so a dedicated push is needed there.
     * <p>
     * See {@code bisq-network/bisq-mobile#1450}.
     */
    public void dispatchMobileOnlyNotification(Notification notification) {
        log.debug("Dispatching mobile-only notification '{}'", notification.getTitle());
        mobileNotificationService.dispatchNotification(notification);
    }
}
