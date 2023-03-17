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

package bisq.desktop.common.notifications;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class AwtNotifications implements NotificationsDelegate {

    public void notify(String title, String message) {
        SystemTray systemTray = SystemTray.getSystemTray();
        Image awtImage = Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("images/task_bar_icon_windows.png"));
        TrayIcon trayIcon = new TrayIcon(awtImage, "Bisq");
        trayIcon.setImageAutoSize(true);
        try {
            systemTray.add(trayIcon);
            // todo on OSX it shows ugly java icon and Java as app name
            // check how it behaves on Windows 
            // With MessageType.NONE the line for the application (would be likely Bisq.exe in binary) 
            // should not be displayed on windows
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
            // Trayicon does not remove itself in all cases for Linux
            trayIcon.addActionListener(l -> systemTray.remove(trayIcon));
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }


    }
}