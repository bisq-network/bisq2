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
package bisq.presentation.notifications.osx;

import bisq.common.platform.OS;
import bisq.common.platform.Version;
import bisq.presentation.notifications.SystemNotificationDelegate;
import bisq.presentation.notifications.osx.foundation.Foundation;
import bisq.presentation.notifications.osx.foundation.ID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsxNotificationDelegate implements SystemNotificationDelegate {
    public static boolean isSupported() {
        try {
            // Requires at least Mountain Lion
            if (new Version(OS.getOsVersion()).below("10.8.0")) {
                return false;
            }

            // If native lib would not be supported it throws an exception
            Foundation.init();
            return true;
        } catch (Throwable t) {
            log.error("No native OSX support for notifications. OSX version: " + OS.getOsVersion(), t);
            return false;
        }
    }

    @Override
    public void show(String title, String message) {
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
