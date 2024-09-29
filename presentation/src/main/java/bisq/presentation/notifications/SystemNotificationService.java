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
import bisq.common.platform.OS;
import bisq.presentation.notifications.linux.LinuxNotificationDelegate;
import bisq.presentation.notifications.osx.OsxNotificationDelegate;
import bisq.presentation.notifications.other.AwtNotificationDelegate;
import bisq.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;

import java.awt.SystemTray;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SystemNotificationService implements Service {
    private final Path baseDir;
    private final SettingsService settingsService;
    private SystemNotificationDelegate delegate;
    private boolean isInitialized;

    public SystemNotificationService(Path baseDir, SettingsService settingsService) {
        this.baseDir = baseDir;
        this.settingsService = settingsService;
    }

    public CompletableFuture<Boolean> initialize() {
        isInitialized = true;
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    public void show(Notification notification) {
        if (isInitialized) {
            getDelegate().ifPresent(delegate -> delegate.show(notification.getTitle(), notification.getMessage()));
        }
    }

    private Optional<SystemNotificationDelegate> getDelegate() {
        if (delegate == null) {
            if (OS.isLinux() && LinuxNotificationDelegate.isSupported()) {
                delegate = new LinuxNotificationDelegate(baseDir, settingsService);
            } else if (OS.isMacOs() && OsxNotificationDelegate.isSupported()) {
                delegate = new OsxNotificationDelegate();
            } else {
                boolean supported = false;
                try {
                    supported = SystemTray.isSupported();
                } catch (Exception e) {
                    log.warn("SystemTray.isSupported call failed", e);
                }
                try {
                    if (supported) {
                        delegate = new AwtNotificationDelegate();
                    }
                } catch (Exception e) {
                    log.warn("Creating AwtNotificationDelegate failed, even SystemTray.isSupported() returned true", e);
                }
            }
        }
        return Optional.ofNullable(delegate);
    }
}
