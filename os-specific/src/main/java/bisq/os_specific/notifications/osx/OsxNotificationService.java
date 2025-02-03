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

package bisq.os_specific.notifications.osx;

import bisq.common.platform.OS;
import bisq.common.platform.Version;
import bisq.common.threading.ExecutorFactory;
import bisq.os_specific.notifications.osx.foundation.Foundation;
import bisq.os_specific.notifications.osx.foundation.ID;
import bisq.presentation.notifications.OsSpecificNotificationService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OsxNotificationService implements OsSpecificNotificationService {
    private boolean isSupported;

    public OsxNotificationService() {
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture.runAsync(() -> {
            try {
                checkArgument(new Version(OS.getOsVersion()).aboveOrEqual("10.8.0"),
                        "OSX version must be at least 10.8.0 (Mountain Lion)");
                // If native lib would not be supported it throws an exception
                Foundation.init();
                isSupported = true;
            } catch (Exception e) {
                log.warn("OsxNotificationService not supported.", e);
                isSupported = false;
            }
        }, ExecutorFactory.newSingleThreadExecutor("initialize-NotificationService"));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void show(String title, String message) {
        if (isSupported) {
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
}
