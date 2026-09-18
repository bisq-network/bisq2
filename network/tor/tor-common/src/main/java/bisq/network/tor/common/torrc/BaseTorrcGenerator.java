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

package bisq.network.tor.common.torrc;

import lombok.Builder;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_PORT;
import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_PORT_WRITE_TO_FILE;
import static bisq.network.tor.common.torrc.Torrc.Keys.DATA_DIRECTORY;
import static bisq.network.tor.common.torrc.Torrc.Keys.HASHED_CONTROL_PASSWORD;
import static bisq.network.tor.common.torrc.Torrc.Keys.SOCKS_PORT;
import static bisq.network.tor.common.torrc.Torrc.Values.EmbeddedTor.CONTROL_PORT_AUTO;
import static bisq.network.tor.common.torrc.Torrc.Values.EmbeddedTor.SOCKS_PORT_DISABLED;

public class BaseTorrcGenerator implements TorrcConfigGenerator {
    public static final String CONTROL_DIR_NAME = "control";

    private final Path dataDirPath;
    private final Path controlPortWriteFilePath;
    private final String hashedControlPassword;
    private final boolean isTestNetwork;

    @Builder
    public BaseTorrcGenerator(Path dataDirPath, String hashedControlPassword, boolean isTestNetwork) {
        this.dataDirPath = dataDirPath;
        this.controlPortWriteFilePath = dataDirPath.resolve(CONTROL_DIR_NAME).resolve("control");
        this.hashedControlPassword = hashedControlPassword;
        this.isTestNetwork = isTestNetwork;
    }

    @Override
    public Map<String, List<String>> generate() {
        Map<String, List<String>> torConfigMap = new LinkedHashMap<>();
        torConfigMap.put(DATA_DIRECTORY, List.of(dataDirPath.toAbsolutePath().toString()));

        torConfigMap.put(CONTROL_PORT, List.of(CONTROL_PORT_AUTO));
        torConfigMap.put(CONTROL_PORT_WRITE_TO_FILE, List.of(controlPortWriteFilePath.toAbsolutePath().toString()));
        torConfigMap.put(HASHED_CONTROL_PASSWORD, List.of(hashedControlPassword));

        String logLevel = isTestNetwork ? "debug" : "notice";
        torConfigMap.put("Log",
                List.of(logLevel + " file " + dataDirPath.resolve("debug.log").toAbsolutePath())
        );

        torConfigMap.put(SOCKS_PORT, List.of(SOCKS_PORT_DISABLED));
        return torConfigMap;
    }
}
