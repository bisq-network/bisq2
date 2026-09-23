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

package bisq.network.tor;

import bisq.common.platform.LinuxDistribution;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
final class ExternalTorConfigHeuristics {
    private static final Path SYSTEM_TOR_CONTROL_SOCKET_PATH = Path.of("/var/run/tor/control");
    private static final String TAILS_ONION_GRATER_ADDRESS = "127.0.0.1:951";
    private static final List<Path> SYSTEM_TOR_COOKIE_AUTH_FILE_CANDIDATES = List.of(
            Path.of("/var/run/tor/control.authcookie"),
            Path.of("/run/tor/control.authcookie")
    );

    private ExternalTorConfigHeuristics() {
    }

    static String apply(String torConfig) {
        return apply(torConfig, SYSTEM_TOR_CONTROL_SOCKET_PATH, SYSTEM_TOR_COOKIE_AUTH_FILE_CANDIDATES, LinuxDistribution.isTails());
    }

    static String apply(String torConfig,
                        Path systemTorControlSocketPath,
                        List<Path> cookieAuthFileCandidates,
                        boolean isTails) {
        boolean controlSocketExists = Files.exists(systemTorControlSocketPath);
        boolean controlSocketAccessible = controlSocketExists
                && Files.isReadable(systemTorControlSocketPath)
                && Files.isWritable(systemTorControlSocketPath);

        Optional<Path> cookieAuthFilePath = cookieAuthFileCandidates.stream()
                .filter(path -> Files.exists(path) && Files.isReadable(path))
                .findFirst();

        String configWithDetectedValues = torConfig;
        String source;

        if (controlSocketAccessible) {
            configWithDetectedValues = configWithDetectedValues
                    .replace("## ControlSocket /path/to/tor/control.socket", "ControlSocket " + systemTorControlSocketPath)
                    .replace("#UseExternalTor 1", "UseExternalTor 1");
            source = "Detected accessible system Tor control socket at '" + systemTorControlSocketPath + "'";
        } else if (isTails) {
            configWithDetectedValues = configWithDetectedValues
                    .replace("ControlPort 127.0.0.1:9051", "ControlPort " + TAILS_ONION_GRATER_ADDRESS)
                    .replace("#UseExternalTor 1", "UseExternalTor 1");
            source = "Tails OS detected, using onion grater at '" + TAILS_ONION_GRATER_ADDRESS + "'";
        } else {
            log.debug("No external Tor auto-configured: controlSocket exists={}, accessible={}, isTails={}, " +
                            "cookieAuthFile present={}. socketPath='{}', cookieAuthCandidates={}",
                    controlSocketExists, controlSocketAccessible, isTails, cookieAuthFilePath.isPresent(),
                    systemTorControlSocketPath, cookieAuthFileCandidates);
            return torConfig;
        }

        if (cookieAuthFilePath.isPresent()) {
            configWithDetectedValues = configWithDetectedValues
                    .replace("CookieAuthentication 0", "CookieAuthentication 1")
                    .replace("CookieAuthFile /path/to/control_auth_cookie", "CookieAuthFile " + cookieAuthFilePath.get());
            log.info("{}. Writing external_tor.config with UseExternalTor=1, CookieAuthentication=1 and CookieAuthFile='{}'.",
                    source, cookieAuthFilePath.get());
        } else {
            log.info("{}. Writing external_tor.config with UseExternalTor=1 and leaving CookieAuthentication disabled because no readable auth cookie file was found.",
                    source);
        }

        return configWithDetectedValues;
    }
}
