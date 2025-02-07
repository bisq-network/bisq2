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

package bisq.os_specific.notifications.other;

import bisq.common.threading.ExecutorFactory;
import bisq.presentation.notifications.OsSpecificNotificationService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.ImageIcon;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AwtNotificationService implements OsSpecificNotificationService {
    private boolean isSupported;
    private TrayIcon trayIcon;

    public AwtNotificationService() {
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture.runAsync(() -> {
            try {
                checkArgument(SystemTray.isSupported(), "SystemTray is not supported");
                SystemTray systemTray = SystemTray.getSystemTray();
                URL image = getClass().getClassLoader().getResource("images/app_window/icon_128.png");
                trayIcon = new TrayIcon(new ImageIcon(image, "Bisq 2").getImage());
                trayIcon.setImageAutoSize(true);
                systemTray.add(trayIcon);
                isSupported = true;
            } catch (Exception e) {
                log.warn("AwtNotificationService not supported.", e);
                isSupported = false;
            }
        }, ExecutorFactory.newSingleThreadExecutor("initialize-NotificationService"));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public void show(String title, String message) {
        if (isSupported) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
        }
    }
}
