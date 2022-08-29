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

package bisq.wallets.electrum;

import bisq.common.util.NetworkUtils;
import bisq.wallets.electrum.regtest.ElectrumXServerExtension;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ElectrumXServerExtension.class)
public class ElectrumConfigGenerationIntegrationTests {
    private final ElectrumXServerConfig serverConfig;

    public ElectrumConfigGenerationIntegrationTests(ElectrumXServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Test
    void generateConfig(@TempDir Path dataDir) {
        var electrumConfig = ElectrumConfig.builder()
                .rpcUser("bisq")
                .rpcPassword("secret_password")
                .rpcPort(String.valueOf(NetworkUtils.findFreeSystemPort()))
                .build();

        var processConfig = ElectrumProcessConfig.builder()
                .dataDir(dataDir)
                .electrumXServerHost("127.0.0.1")
                .electrumXServerPort(serverConfig.getPort())
                .electrumConfig(electrumConfig)
                .build();

        ElectrumProcess electrumProcess = new ElectrumProcess(dataDir, processConfig);
        electrumProcess.start();
        electrumProcess.shutdown();

        File logsDir = dataDir.resolve("regtest")
                .resolve("logs")
                .toFile();

        assertThat(logsDir.exists()).isTrue();

        File[] logFile = logsDir.listFiles();
        Objects.requireNonNull(logFile);
        assertThat(logFile.length).isEqualTo(1);

        long fileSize = logFile[0].length();
        assertThat(fileSize).isGreaterThan(0);
    }
}
