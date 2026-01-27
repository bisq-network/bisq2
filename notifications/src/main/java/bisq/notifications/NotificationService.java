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


import bisq.common.application.Service;
import bisq.notifications.mobile_push.registration.DeviceRegistrationService;
import bisq.notifications.system.OsSpecificNotificationService;
import bisq.notifications.system.SystemNotification;
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
    private final DeviceRegistrationService deviceRegistrationService;

    public NotificationService(PersistenceService persistenceService, Optional<OsSpecificNotificationService> systemNotificationDelegate) {
        systemNotificationService = new SystemNotificationService(systemNotificationDelegate);
        deviceRegistrationService = new DeviceRegistrationService(persistenceService);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return systemNotificationService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return systemNotificationService.shutdown();
    }


    public void show(SystemNotification notification) {
        systemNotificationService.show(notification);
    }
}
