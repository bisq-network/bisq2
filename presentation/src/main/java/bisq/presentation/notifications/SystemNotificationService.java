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

package bisq.presentation.notifications;


import bisq.common.application.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SystemNotificationService implements Service {
    private final Optional<OsSpecificNotificationService> systemNotificationDelegate;
    private boolean isInitialized;

    public SystemNotificationService(Optional<OsSpecificNotificationService> systemNotificationDelegate) {
        this.systemNotificationDelegate = systemNotificationDelegate;
    }

    public CompletableFuture<Boolean> initialize() {
        isInitialized = true;
        log.info("initialize");
        systemNotificationDelegate.ifPresent(Service::initialize);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        systemNotificationDelegate.ifPresent(Service::shutdown);
        return CompletableFuture.completedFuture(true);
    }


    public void show(Notification notification) {
        if (isInitialized) {
            systemNotificationDelegate.ifPresent(service -> service.show(notification.getTitle(), notification.getMessage()));
        }
    }
}
