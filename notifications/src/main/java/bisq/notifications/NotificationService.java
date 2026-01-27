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


    public void show(Notification notification) {
        systemNotificationService.show(notification);
        mobileNotificationService.show(notification);
    }
}
