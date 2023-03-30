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

import bisq.presentation.notifications.NotificationsDelegate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinuxNotifications implements NotificationsDelegate {
    public static boolean isSupported() {
        try {
            return Runtime.getRuntime().exec("notify-send --help > nil").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void notify(String title, String message) {
        List<String> command = new ArrayList<>();
        command.add("notify-send");

        command.add("-i");
        command.add(System.getProperty("java.home") + "/../Bisq_2.png");

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
