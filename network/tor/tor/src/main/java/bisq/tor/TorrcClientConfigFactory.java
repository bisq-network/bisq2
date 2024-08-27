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

import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import bisq.network.tor.common.torrc.ClientTorrcGenerator;
import bisq.network.tor.common.torrc.TestNetworkTorrcGenerator;
import bisq.network.tor.common.torrc.TorrcConfigGenerator;
import lombok.Builder;
import net.freehaven.tor.control.PasswordDigest;

import java.nio.file.Path;
import java.util.Map;

@Builder
public class TorrcClientConfigFactory {
    public static final String DISABLE_NETWORK_CONFIG_KEY = "DisableNetwork";

    private final boolean isTestNetwork;
    private final Path dataDir;
    private final PasswordDigest hashedControlPassword;

    public TorrcClientConfigFactory(boolean isTestNetwork,
                                    Path dataDir,
                                    PasswordDigest hashedControlPassword) {
        this.isTestNetwork = isTestNetwork;
        this.dataDir = dataDir;
        this.hashedControlPassword = hashedControlPassword;
    }

    public Map<String, String> torrcClientConfigMap(Map<String, String> torrcOverrides) {
        Map<String, String> torrcClientConfig = clientTorrcGenerator().generate();
        torrcClientConfig.putAll(torrcOverrides);
        torrcClientConfig.put(DISABLE_NETWORK_CONFIG_KEY, "1");
        return torrcClientConfig;
    }

    private TorrcConfigGenerator clientTorrcGenerator() {
        TorrcConfigGenerator baseTorrcGenerator = baseTorrcGenerator();
        if (isTestNetwork) {
            baseTorrcGenerator = testNetworkTorrcGenerator(baseTorrcGenerator);
        }

        return ClientTorrcGenerator.builder()
                .baseTorrcConfigGenerator(baseTorrcGenerator)
                .build();
    }

    private TorrcConfigGenerator baseTorrcGenerator() {
        return BaseTorrcGenerator.builder()
                .dataDirPath(dataDir)
                .hashedControlPassword(hashedControlPassword.getHashedPassword())
                .isTestNetwork(isTestNetwork)
                .build();
    }

    private static TorrcConfigGenerator testNetworkTorrcGenerator(TorrcConfigGenerator baseTorrcGenerator) {
        return TestNetworkTorrcGenerator.builder()
                .baseTorrcConfigGenerator(baseTorrcGenerator)
                .build();
    }
}
