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
package bisq.desktop.common.notifications.osx;

import bisq.common.util.OsUtils;
import bisq.desktop.common.notifications.NotificationsDelegate;
import bisq.desktop.common.notifications.osx.foundation.Foundation;
import bisq.desktop.common.notifications.osx.foundation.ID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsxNotifications implements NotificationsDelegate {
    public static boolean isSupported() {
        try {
            // Requires at least Mountain Lion
            if (OsUtils.getVersion().below("10.8.0")) {
                return false;
            }

            // If native lib would not be supported it throws an exception
            Foundation.init();
            return true;
        } catch (Throwable t) {
            log.error("No native OSX support for notifications. OSX version: " + OsUtils.getVersion(), t);
            return false;
        }
    }

    @Override
    public void notify(String title, String message) {
        // TODO Check with binary build if Bisq icon and Bisq as app name is shown
        ID notification = Foundation.invoke(Foundation.getObjcClass("NSUserNotification"), "new");
        Foundation.invoke(notification, "setTitle:",
                Foundation.nsString(title));
        Foundation.invoke(notification, "setInformativeText:",
                Foundation.nsString(message.replace("\n", "\r")));
        ID center = Foundation.invoke(Foundation.getObjcClass("NSUserNotificationCenter"),
                "defaultUserNotificationCenter");
        Foundation.invoke(center, "deliverNotification:", notification);
    }
}
