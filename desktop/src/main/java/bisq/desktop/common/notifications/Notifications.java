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

import bisq.common.util.OperatingSystem;
import bisq.common.util.OsUtils;
import bisq.desktop.common.notifications.linux.LinuxNotifications;
import bisq.desktop.common.notifications.osx.OsxNotifications;

/**
 * Shows OS native notifications if supported, otherwise AWT notifications.
 * On Linux we use notify-send is available.
 * On OSX NotificationCenter if supported.
 * On Windows and in cases of lack of support for native notifications we use AWT.
 * <p>
 * TODO test on Linux and Windows. Create binaries for all OS and see how it behaves. When run as java app the
 * app name is Java, but with binary it should be Bisq. The notification icon should be the Bisq icon (at on OSX its a terminal icon)
 * AWT notifications are mapped to OS notifications. Check what happens if no mapping from java to OS native notifications is supported.
 */
public class Notifications {
    private static Notifications instance;

    private static Notifications getInstance() {
        if (instance == null) {
            instance = new Notifications();
        }
        return instance;
    }

    public static void notify(String title, String message) {
        getInstance().delegate.notify(title, message);
    }

    private final NotificationsDelegate delegate;

    private Notifications() {
        if (OsUtils.getOperatingSystem() == OperatingSystem.LINUX &&
                LinuxNotifications.isSupported()) {
            delegate = new LinuxNotifications();
            return;
        }
        if (OsUtils.getOperatingSystem() == OperatingSystem.MAC &&
                OsxNotifications.isSupported()) {
            delegate = new OsxNotifications();
            return;
        }
        delegate = new AwtNotifications();
    }
}