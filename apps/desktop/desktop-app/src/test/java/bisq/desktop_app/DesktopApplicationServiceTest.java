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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DesktopApplicationServiceTest {
    private static final String ALLOW_PLUGGABLE_TRANSPORTS =
            "application.network.configByTransportType.tor.sandbox.allowPluggableTransports=true";
    private static final String SANDBOX_OFF = "application.network.configByTransportType.tor.sandbox.enabled=false";
    // A configuration entry which the message asks the user to add to bisq.conf
    private static final Pattern CONFIG_ENTRY = Pattern.compile("(application(?:\\.\\w+)+)=(true|false)");

    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void refusedPluggableTransportsOfferToAllowThemRemoveTheBridgesOrTurnTheSandboxOff() {
        String message = DesktopApplicationService.getPluggableTransportsNotAllowedMessage(
                List.of("obfs4", "snowflake"), false);

        assertThat(message)
                .contains("pluggable transports obfs4, snowflake")
                .contains(ALLOW_PLUGGABLE_TRANSPORTS)
                .contains("Remove the bridges")
                .contains(SANDBOX_OFF);
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

    // On Windows Tor cannot start pluggable transports in its sandbox, so allowing them would not help.
    @Test
    void refusedPluggableTransportsOnWindowsOfferToRemoveTheBridgesOrTurnTheSandboxOff() {
        String message = DesktopApplicationService.getPluggableTransportsNotAllowedMessage(List.of("obfs4"), true);

        assertThat(message)
                .contains("pluggable transports obfs4")
                .doesNotContain("allowPluggableTransports")
                .contains("Remove the bridges")
                .contains(SANDBOX_OFF);
    }

    // A renamed entry of the configuration would make the advice of the message fail without an error.
    @Test
    void everyEntryWhichTheMessagesNameIsABooleanOfTheDesktopConfiguration() {
        Config desktopConfig = ConfigFactory.parseResources("desktop.conf").resolve();
        String messages = DesktopApplicationService.getPluggableTransportsNotAllowedMessage(List.of("obfs4"), false) +
                DesktopApplicationService.getPluggableTransportsNotAllowedMessage(List.of("obfs4"), true);

        Matcher matcher = CONFIG_ENTRY.matcher(messages);
        int entries = 0;
        while (matcher.find()) {
            String path = matcher.group(1);
            assertThat(desktopConfig.hasPath(path)).as(path).isTrue();
            assertThat(desktopConfig.getValue(path).valueType()).as(path).isEqualTo(ConfigValueType.BOOLEAN);
            entries++;
        }
        assertThat(entries).isEqualTo(3);
    }
}
