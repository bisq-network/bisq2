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
import bisq.settings.DontShowAgainKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The notice which tells the user what the Tor sandbox cannot do on this system and, where possible, how to get a
 * stronger level. Without weaknesses there is no notice: Tor runs in the full sandbox, the user turned the sandbox off,
 * or Bisq uses an external Tor.
 *
 * @param dontShowAgainId the key with which the user stops this notice. Each set of weaknesses has its own, so that a
 *                        weakness which the user has not seen yet gets a notice.
 */
record TorSandboxNotice(String dontShowAgainId, String message) {
    static Optional<TorSandboxNotice> find(Set<TorSandboxWeakness> weaknesses, OS os) {
        if (weaknesses.isEmpty()) {
            return Optional.empty();
        }

        boolean isLinux = os == OS.LINUX;
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(Res.get("network.torSandbox.notice.intro"));
        if (weaknesses.contains(TorSandboxWeakness.HOST_NETWORK_FOR_TRANSPORTS)) {
            // On Linux obfs4 keeps the network of the sandbox; on macOS every transport needs the network of the host.
            paragraphs.add(Res.get(isLinux ?
                    "network.torSandbox.notice.hostNetworkForTransports.linux" :
                    "network.torSandbox.notice.hostNetworkForTransports"));
        }
        if (weaknesses.contains(TorSandboxWeakness.HOST_NETWORK_OF_SYSTEM)) {
            paragraphs.add(Res.get(isLinux ?
                    "network.torSandbox.notice.hostNetworkOfSystem.linux" :
                    "network.torSandbox.notice.hostNetworkOfSystem"));
        }
        if (weaknesses.contains(TorSandboxWeakness.SIGNALS_NOT_ISOLATED)) {
            paragraphs.add(Res.get("network.torSandbox.notice.signalsNotIsolated"));
        }

        String dontShowAgainId = DontShowAgainKey.TOR_SANDBOX_WEAK_LEVEL.getKey() + "." + weaknesses.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining("."));
        return Optional.of(new TorSandboxNotice(dontShowAgainId, String.join("\n\n", paragraphs)));
    }
}
