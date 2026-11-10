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

package bisq.desktop_app;

import bisq.common.platform.PlatformUtils;
import bisq.i18n.Res;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DesktopApplicationServiceTest {
    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    // The popup adds a period after the message in every language, so a message which ends with one would end with two.
    @Test
    void theStartupErrorLeavesTheFinalPeriodToThePopup() {
        String message = DesktopApplicationService.getStartupErrorMessage(new CompletionException(
                new IllegalStateException("Run Bisq on a Whonix-Workstation, which uses the Tor of the Gateway.")));

        assertThat(message).isEqualTo("Run Bisq on a Whonix-Workstation, which uses the Tor of the Gateway");
        assertThat(Res.get("popup.startup.error", message)).endsWith("Gateway.");
        assertThat(DesktopApplicationService.getStartupErrorMessage(new IllegalStateException("Tor did not start")))
                .isEqualTo("Tor did not start");
    }

    // The report button of the error popup copies the message into a bug report, so the message masks the home
    // directory, as the log zip does. The Tor library names the path of external_tor.config in its message.
    @Test
    void theStartupErrorMasksTheHomeDirectory() {
        Path homeDirectoryPath = PlatformUtils.getHomeDirectoryPath().toAbsolutePath();
        assumeTrue(homeDirectoryPath.getNameCount() > 0, "The home directory is the root of the file system");
        Path configPathInHome = Path.of(".local", "share", "Bisq2", "tor", "external_tor.config");

        String message = DesktopApplicationService.getStartupErrorMessage(new CompletionException(
                new IllegalStateException("Start the external Tor or correct " +
                        homeDirectoryPath.resolve(configPathInHome) + ".")));

        assertThat(message)
                .doesNotContain(homeDirectoryPath.toString())
                .isEqualTo("Start the external Tor or correct <HOME_DIR>" + File.separator + configPathInHome);
    }
}
