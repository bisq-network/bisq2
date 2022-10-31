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

package bisq.wallets.electrum.regtest.electrumx;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.util.List;

public class ElectrumXServerRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator> {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup = new BitcoindRegtestSetup();

    private final int electrumXServerPort = NetworkUtils.findFreeSystemPort();
    @Getter
    private final ElectrumXServerConfig serverConfig = createServerConfig();
    private final ElectrumXServerRegtestProcess electrumXServerRegtestProcess =
            new ElectrumXServerRegtestProcess(serverConfig);

    public ElectrumXServerRegtestSetup() throws IOException {
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindRegtestSetup, electrumXServerRegtestProcess)
        );
    }

    @Override
    public List<String> mineOneBlock() throws InterruptedException {
        return bitcoindRegtestSetup.mineOneBlock();
    }

    @Override
    public RpcConfig getRpcConfig() {
        throw new UnsupportedOperationException();
    }

    private ElectrumXServerConfig createServerConfig() throws IOException {
        return ElectrumXServerConfig.builder()
                .dataDir(FileUtils.createTempDir())
                .port(electrumXServerPort)
                .rpcPort(NetworkUtils.findFreeSystemPort())
                .bitcoindRpcConfig(bitcoindRegtestSetup.getRpcConfig())
                .build();
    }
}
