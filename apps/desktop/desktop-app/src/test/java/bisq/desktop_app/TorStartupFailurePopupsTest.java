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

import bisq.i18n.Res;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static bisq.network.p2p.node.transport.TorTransportService.ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH;
import static bisq.network.p2p.node.transport.TorTransportService.BRIDGES_CONFIG_PATH;
import static bisq.network.p2p.node.transport.TorTransportService.BUILT_IN_BRIDGES_CONFIG_PATH;
import static bisq.network.p2p.node.transport.TorTransportService.SANDBOX_ENABLED_CONFIG_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class TorStartupFailurePopupsTest {
    private static final Path CONFIG_FILE_PATH = Path.of("data", "bisq.conf").toAbsolutePath();
    private static final String ERROR = "Bisq did not start Tor, because it cannot run Tor in its sandbox on this system";
    private static final TorStartupFailure SANDBOX_UNAVAILABLE = new TorStartupFailure.SandboxUnavailable();
    private static final TorStartupFailure TRANSPORTS_NOT_ALLOWED =
            new TorStartupFailure.PluggableTransportsNotAllowed(List.of("obfs4", "snowflake"));
    private static final Map<String, Object> NO_BRIDGES = Map.of(BUILT_IN_BRIDGES_CONFIG_PATH, "",
            BRIDGES_CONFIG_PATH, List.of());

    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void aSandboxWhichCannotRunTorOffersToTurnItOff() {
        StartupFailurePopupContent content = create(SANDBOX_UNAVAILABLE, false, Set.of());

        assertThat(content.headline()).isEqualTo("Tor cannot run in its security sandbox");
        assertThat(content.message())
                .startsWith(Res.get("popup.startup.error", ERROR))
                .contains(SANDBOX_ENABLED_CONFIG_PATH + "=false to " + CONFIG_FILE_PATH);
        assertThat(entriesOf(content)).containsExactly(Map.of(SANDBOX_ENABLED_CONFIG_PATH, false));
    }

    // The popup cuts a message after 2200 characters. A refusal of sandy can quote 4 KiB of standard error, which must
    // not cut off the risk of the choice and how to undo it.
    @Test
    void aLongErrorIsShortenedSoThatTheRiskAndTheUndoStayVisible() {
        String longError = ERROR + "\n" + "x".repeat(5000);

        StartupFailurePopupContent content = TorStartupFailurePopups.create(SANDBOX_UNAVAILABLE, longError,
                false, path -> false, CONFIG_FILE_PATH);

        assertThat(content.message())
                .hasSizeLessThanOrEqualTo(2200)
                .contains("including your wallet and private keys")
                .contains("To turn the sandbox on again, open this file and remove that line")
                .contains("You can find the full error message in bisq.log");
        assertThat(TorStartupFailurePopups.shortenError(ERROR)).isEqualTo(ERROR);
    }

    // The command line overrides bisq.conf, so writing the choice there would not change the next start
    @Test
    void aSandboxWhichCannotRunTorOffersNoChoiceWhileTheCommandLineSetsIt() {
        StartupFailurePopupContent content = create(SANDBOX_UNAVAILABLE, false, Set.of(SANDBOX_ENABLED_CONFIG_PATH));

        assertThat(content.choices()).isEmpty();
        assertThat(content.message()).contains("an option that sets " + SANDBOX_ENABLED_CONFIG_PATH);
    }

    // Allowing the transports weakens the sandbox less than turning it off, so only that is offered
    @Test
    void refusedTransportsOfferToAllowThemOrToUseNoBridges() {
        StartupFailurePopupContent content = create(TRANSPORTS_NOT_ALLOWED, false, Set.of());

        assertThat(content.headline()).isEqualTo("Tor cannot use your bridges");
        assertThat(content.message())
                .contains("pluggable transports: obfs4, snowflake")
                .contains("Allowing them weakens some of the sandbox's protections")
                .contains("Bisq will save your choice in " + CONFIG_FILE_PATH);
        assertThat(entriesOf(content)).containsExactly(Map.of(ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH, true),
                NO_BRIDGES);
    }

    // On Windows Tor cannot start the transports in its sandbox, so allowing them would not help
    @Test
    void refusedTransportsOnWindowsOfferToUseNoBridgesOrToTurnTheSandboxOff() {
        StartupFailurePopupContent content = create(TRANSPORTS_NOT_ALLOWED, true, Set.of());

        assertThat(content.message())
                .contains("On Windows, Tor cannot start these helper programs inside its security sandbox")
                .contains("including your wallet and private keys")
                .doesNotContain("Allowing them");
        assertThat(entriesOf(content)).containsExactly(NO_BRIDGES, Map.of(SANDBOX_ENABLED_CONFIG_PATH, false));
    }

    @Test
    void refusedTransportsLeaveOutTheChoicesWhichTheCommandLineOverrides() {
        StartupFailurePopupContent onlyBridges = create(TRANSPORTS_NOT_ALLOWED, false,
                Set.of(ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH));
        assertThat(entriesOf(onlyBridges)).containsExactly(NO_BRIDGES);
        assertThat(onlyBridges.message()).contains("an option that sets " + ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH);

        StartupFailurePopupContent none = create(TRANSPORTS_NOT_ALLOWED, false,
                Set.of(ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH, BRIDGES_CONFIG_PATH));
        assertThat(none.choices()).isEmpty();
        assertThat(none.message())
                .contains("an option that sets " + ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH + ", " + BRIDGES_CONFIG_PATH)
                .doesNotContain("Bisq will save your choice");
    }

    // A renamed or retyped entry of the configuration would make a choice fail at the next start or have no effect
    @Test
    void everyEntryWhichAChoiceWritesHasTheTypeOfTheDesktopConfiguration() {
        Config desktopConfig = ConfigFactory.parseResources("desktop.conf").resolve();
        List<Map<String, Object>> allEntries = Stream.of(
                        create(SANDBOX_UNAVAILABLE, false, Set.of()),
                        create(TRANSPORTS_NOT_ALLOWED, false, Set.of()),
                        create(TRANSPORTS_NOT_ALLOWED, true, Set.of()))
                .flatMap(content -> entriesOf(content).stream())
                .toList();

        assertThat(allEntries).hasSize(5);
        allEntries.forEach(entries -> entries.forEach((path, value) -> {
            assertThat(desktopConfig.hasPath(path)).as(path).isTrue();
            assertThat(desktopConfig.getValue(path).valueType()).as(path)
                    .isEqualTo(ConfigValueFactory.fromAnyRef(value).valueType());
        }));
    }

    private static StartupFailurePopupContent create(TorStartupFailure failure,
                                                     boolean isWindows,
                                                     Set<String> pathsSetOnCommandLine) {
        Predicate<String> isSetOnCommandLine = pathsSetOnCommandLine::contains;
        return TorStartupFailurePopups.create(failure, ERROR, isWindows, isSetOnCommandLine, CONFIG_FILE_PATH);
    }

    private static List<Map<String, Object>> entriesOf(StartupFailurePopupContent content) {
        return content.choices().stream().map(StartupFailurePopupContent.Choice::configEntries).toList();
    }
}
