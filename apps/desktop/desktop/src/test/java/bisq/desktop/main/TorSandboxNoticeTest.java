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

package bisq.desktop.main;

import bisq.common.platform.OS;
import bisq.i18n.Res;
import bisq.network.p2p.node.transport.TorSandboxWeakness;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static bisq.network.p2p.node.transport.TorSandboxWeakness.HOST_NETWORK_FOR_TRANSPORTS;
import static bisq.network.p2p.node.transport.TorSandboxWeakness.HOST_NETWORK_OF_SYSTEM;
import static bisq.network.p2p.node.transport.TorSandboxWeakness.SIGNALS_NOT_ISOLATED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The notice about a weak level of the Tor sandbox says what the sandbox cannot do on this system and, where possible,
 * how to get a stronger level. It needs no JavaFX.
 */
class TorSandboxNoticeTest {
    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    // Also the case of a user who turned the sandbox off and of an external Tor
    @Test
    void fullSandboxGetsNoNotice() {
        assertThat(TorSandboxNotice.find(Set.of(), OS.LINUX)).isEmpty();
    }

    @Test
    void transportsWhichNeedTheNetworkOfTheHostNameObfs4OnLinux() {
        String message = TorSandboxNotice.find(Set.of(HOST_NETWORK_FOR_TRANSPORTS), OS.LINUX).orElseThrow().message();

        assertThat(message)
                .startsWith(Res.get("network.torSandbox.notice.intro"))
                .contains(Res.get("network.torSandbox.notice.hostNetworkForTransports.linux"))
                .contains("obfs4");
    }

    // On macOS every pluggable transport needs the network of the host
    @Test
    void transportsWhichNeedTheNetworkOfTheHostOnMacOsNameOnlyNoBridges() {
        String message = TorSandboxNotice.find(Set.of(HOST_NETWORK_FOR_TRANSPORTS), OS.MAC_OS).orElseThrow().message();

        assertThat(message)
                .contains(Res.get("network.torSandbox.notice.hostNetworkForTransports"))
                .doesNotContain("obfs4");
    }

    // Bisq does not prepare the system for user namespaces, so the notice names no way to a stronger level
    @Test
    void networkOfTheHostBecauseOfTheSystemNamesTheCause() {
        String message = TorSandboxNotice.find(Set.of(HOST_NETWORK_OF_SYSTEM), OS.LINUX).orElseThrow().message();

        assertThat(message)
                .contains(Res.get("network.torSandbox.notice.hostNetworkOfSystem.linux"))
                .contains("Ubuntu 23.10");
    }

    @Test
    void signalsWhichAreNotIsolatedNameTheLinuxVersionWhichIsolatesThem() {
        String message = TorSandboxNotice.find(EnumSet.of(HOST_NETWORK_OF_SYSTEM, SIGNALS_NOT_ISOLATED), OS.LINUX)
                .orElseThrow()
                .message();

        assertThat(message)
                .contains(Res.get("network.torSandbox.notice.hostNetworkOfSystem.linux"))
                .contains(Res.get("network.torSandbox.notice.signalsNotIsolated"))
                .contains("Linux 6.12");
    }

    // A user who turned off the notice of one weakness still gets the notice of another
    @Test
    void eachSetOfWeaknessesHasItsOwnDontShowAgainKey() {
        String hostNetwork = dontShowAgainId(EnumSet.of(HOST_NETWORK_OF_SYSTEM));
        String hostNetworkAndSignals = dontShowAgainId(EnumSet.of(SIGNALS_NOT_ISOLATED, HOST_NETWORK_OF_SYSTEM));
        String transports = dontShowAgainId(EnumSet.of(HOST_NETWORK_FOR_TRANSPORTS));

        assertThat(hostNetwork).isEqualTo("torSandbox.weakLevel.HOST_NETWORK_OF_SYSTEM");
        assertThat(hostNetworkAndSignals)
                .isEqualTo("torSandbox.weakLevel.HOST_NETWORK_OF_SYSTEM.SIGNALS_NOT_ISOLATED");
        assertThat(Set.of(hostNetwork, hostNetworkAndSignals, transports)).hasSize(3);
    }

    private static String dontShowAgainId(Set<TorSandboxWeakness> weaknesses) {
        return TorSandboxNotice.find(weaknesses, OS.LINUX).orElseThrow().dontShowAgainId();
    }
}
