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
import bisq.common.util.OperatingSystem;
import bisq.common.util.OsUtils;
import bisq.presentation.notifications.linux.LinuxNotificationSender;
import bisq.presentation.notifications.osx.OsxNotificationSender;
import bisq.presentation.notifications.other.AwtNotificationSender;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class SendNotificationService implements Service {
    private NotificationSender sender;

    public SendNotificationService() {
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    public void send(Notification notification) {
        getSender().send(notification.getTitle(), notification.getMessage());
    }

    private NotificationSender getSender() {
        if (sender == null) {
            if (OsUtils.getOperatingSystem() == OperatingSystem.LINUX &&
                    LinuxNotificationSender.isSupported()) {
                sender = new LinuxNotificationSender();
            } else if (OsUtils.getOperatingSystem() == OperatingSystem.MAC &&
                    OsxNotificationSender.isSupported()) {
                sender = new OsxNotificationSender();
            } else {
                sender = new AwtNotificationSender();
            }
        }
        return sender;
    }
}
