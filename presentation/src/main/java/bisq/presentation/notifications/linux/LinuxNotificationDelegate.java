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
package bisq.presentation.notifications.linux;

import bisq.common.file.FileUtils;
import bisq.presentation.notifications.SystemNotificationDelegate;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LinuxNotificationDelegate implements SystemNotificationDelegate {
    private final SettingsService settingsService;
    @Nullable
    private String iconPath;

    public LinuxNotificationDelegate(Path baseDir, SettingsService settingsService) {
        this.settingsService = settingsService;

        String javaHomePath = System.getProperty("java.home");
        if (!javaHomePath.startsWith("/usr/lib/jvm/")) {
            // Running from binary
            // The Bisq2.png is generated by the packager from the package/linux/icon.png file
            iconPath = javaHomePath + "/../Bisq2.png";
        } else {
            // TODO Consider to use that code also when running from binary. Currently the jpackager build fails to
            // build a valid binary so it cannot be tested.

            // We are running from source code and use icon from resources as Bisq2.png is not available
            String fileName = "linux-notification-icon.png";
            File destination = Path.of(baseDir.toAbsolutePath().toString(), fileName).toFile();
            if (!destination.exists()) {
                try {
                    FileUtils.resourceToFile(fileName, destination);
                    iconPath = destination.getAbsolutePath();
                } catch (IOException e) {
                    log.error("Copying notificationIcon from resources failed", e);
                }
            } else {
                iconPath = destination.getAbsolutePath();
            }
        }
    }

    public static boolean isSupported() {
        try {
            String[] command = new String[]{"notify-send --help > nil"};
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public void show(String title, String message) {
        Boolean useTransientNotifications = settingsService.getCookie().asBoolean(CookieKey.USE_TRANSIENT_NOTIFICATIONS)
                .orElse(true);

        List<String> command = new ArrayList<>();
        command.add("notify-send");

        if (iconPath != null) {
            command.add("-i");
            command.add(iconPath);
        }

        // notify-send does not support removing notifications. To avoid that we fill up the notification center we
        // can set the notification transient.
        if (useTransientNotifications) {
            command.add("--hint=int:transient:1");
        }

        command.add("--app-name");
        command.add("Bisq");

        command.add(title);
        command.add(message);
        try {
            Runtime.getRuntime().exec(command.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException("Unable to notify with Notify OSD", e);
        }
    }
}
