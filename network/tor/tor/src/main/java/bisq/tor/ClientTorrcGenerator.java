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

package bisq.tor;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class ClientTorrcGenerator {
    public static final String DISABLE_NETWORK_CONFIG_KEY = "DisableNetwork";

    private final Path dataDirPath;
    private final int controlPort;
    private final int socksPort;
    private final String hashedControlPassword;
    private final Map<String, String> torConfigMap = new HashMap<>();

    public ClientTorrcGenerator(Path dataDirPath, int controlPort, int socksPort, String hashedControlPassword) {
        this.dataDirPath = dataDirPath;
        this.controlPort = controlPort;
        this.socksPort = socksPort;
        this.hashedControlPassword = hashedControlPassword;
    }

    public Map<String, String> generate() {
        return generate(Collections.emptyMap());
    }

    public Map<String, String> generate(Map<String, String> torrcOverrideConfigs) {
        torConfigMap.put("DataDirectory", dataDirPath.toAbsolutePath().toString());

        torConfigMap.put(DISABLE_NETWORK_CONFIG_KEY, "1");
        torConfigMap.put("ControlPort", "127.0.0.1:" + controlPort);
        torConfigMap.put("HashedControlPassword", hashedControlPassword);
        torConfigMap.put("Log",
                "debug file " + dataDirPath.resolve("debug.log").toAbsolutePath()
        );

        torConfigMap.put("SocksPort", String.valueOf(socksPort));

        torConfigMap.putAll(torrcOverrideConfigs);

        return torConfigMap;
    }
}
