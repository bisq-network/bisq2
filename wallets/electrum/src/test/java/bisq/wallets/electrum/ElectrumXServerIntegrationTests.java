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

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerRegtestProcess;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class ElectrumXServerIntegrationTests {
    @Test
    void startUpAndShutdownTest() throws IOException, InterruptedException {
        BitcoindRegtestSetup bitcoindRegtestSetup = new BitcoindRegtestSetup();
        bitcoindRegtestSetup.start();

        Path electrumXDataDir = FileUtils.createTempDir();
        ElectrumXServerConfig electrumXServerConfig = ElectrumXServerConfig.builder()
                .dataDir(electrumXDataDir)
                .port(NetworkUtils.findFreeSystemPort())
                .rpcPort(NetworkUtils.findFreeSystemPort())
                .bitcoindRpcConfig(bitcoindRegtestSetup.getRpcConfig())
                .build();

        var electrumXServerRegtestProcess = new ElectrumXServerRegtestProcess(electrumXServerConfig);
        electrumXServerRegtestProcess.start();

        electrumXServerRegtestProcess.shutdown();
        bitcoindRegtestSetup.shutdown();
    }
}
