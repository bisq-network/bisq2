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

package bisq.presentation.notifications.other;

import bisq.presentation.notifications.NotificationSender;
import lombok.extern.slf4j.Slf4j;

import javax.swing.ImageIcon;
import java.awt.*;
import java.net.URL;

@Slf4j
public class AwtNotificationSender implements NotificationSender {
    private final TrayIcon trayIcon;

    public AwtNotificationSender() {
        URL image = getClass().getClassLoader().getResource("images/app_window/icon_128.png");
        trayIcon = new TrayIcon(new ImageIcon(image, "Bisq 2").getImage());
        trayIcon.setImageAutoSize(true);
        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String title, String message) {
        trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
    }
}
