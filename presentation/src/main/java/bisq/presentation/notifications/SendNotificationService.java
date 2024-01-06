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
import bisq.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SendNotificationService implements Service {
    private final Path baseDir;
    private final SettingsService settingsService;
    private NotificationSender sender;
    private boolean isInitialize;

    public SendNotificationService(Path baseDir, SettingsService settingsService) {
        this.baseDir = baseDir;
        this.settingsService = settingsService;
    }

    public CompletableFuture<Boolean> initialize() {
        isInitialize = true;
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    public void send(Notification notification) {
        if (isInitialize) {
            getSender().send(notification.getTitle(), notification.getMessage());
        }
    }

    private NotificationSender getSender() {
        if (sender == null) {
            if (OsUtils.getOperatingSystem() == OperatingSystem.LINUX &&
                    LinuxNotificationSender.isSupported()) {
                sender = new LinuxNotificationSender(baseDir, settingsService);
            } else if (OsUtils.getOperatingSystem() == OperatingSystem.MAC &&
                    OsxNotificationSender.isSupported()) {
                sender = new OsxNotificationSender();
            } else if (SystemTray.isSupported()) {
                sender = new AwtNotificationSender();
            }
        }
        return sender;
    }
}
