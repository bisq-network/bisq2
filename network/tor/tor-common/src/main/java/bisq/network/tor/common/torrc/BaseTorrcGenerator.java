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
import java.util.HashMap;
import java.util.Map;

import static bisq.network.tor.common.torrc.Torrc.Keys.*;
import static bisq.network.tor.common.torrc.Torrc.Values.EmbeddedTor.*;

public class BaseTorrcGenerator implements TorrcConfigGenerator {
    public static final String CONTROL_DIR_NAME = "control";

    private final Path dataDirPath;
    private final Path controlPortWriteFile;
    private final String hashedControlPassword;
    private final boolean isTestNetwork;

    @Builder
    public BaseTorrcGenerator(Path dataDirPath, String hashedControlPassword, boolean isTestNetwork) {
        this.dataDirPath = dataDirPath;
        this.controlPortWriteFile = dataDirPath.resolve(CONTROL_DIR_NAME).resolve("control");
        this.hashedControlPassword = hashedControlPassword;
        this.isTestNetwork = isTestNetwork;
    }

    @Override
    public Map<String, String> generate() {
        Map<String, String> torConfigMap = new HashMap<>();
        torConfigMap.put(DATA_DIRECTORY, dataDirPath.toAbsolutePath().toString());

        torConfigMap.put(CONTROL_PORT, CONTROL_PORT_AUTO);
        torConfigMap.put(CONTROL_PORT_WRITE_TO_FILE, controlPortWriteFile.toAbsolutePath().toString());
        torConfigMap.put(HASHED_CONTROL_PASSWORD, hashedControlPassword);

        String logLevel = isTestNetwork ? "debug" : "notice";
        torConfigMap.put("Log",
                logLevel + " file " + dataDirPath.resolve("debug.log").toAbsolutePath()
        );

        torConfigMap.put(SOCKS_PORT, SOCKS_PORT_DISABLED);
        return torConfigMap;
    }
}
