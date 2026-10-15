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

package bisq.os_specific.notifications.linux;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinuxNotificationServiceTest {
    // The user name and the chat message of another user can start with "-". notify-send reads an argument which starts
    // with "-" as an option unless it follows "--", so such a notification was not shown, or set its options.
    @Test
    void titleAndMessageFollowTheEndOfOptions() {
        List<String> command = LinuxNotificationService.createCommand("/icon.png",
                true,
                "-title",
                "--urgency=critical");

        assertEquals(List.of("notify-send",
                "-i", "/icon.png",
                "--hint=int:transient:1",
                "--app-name", "Bisq",
                "--",
                "-title",
                "--urgency=critical"), command);
    }
}
