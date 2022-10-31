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

package bisq.wallets.electrum.regtest.electrum;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerRegtestProcess;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.util.List;

public class MacLinuxElectrumRegtestSetup extends ElectrumRegtestSetup {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup = new BitcoindRegtestSetup();

    private final int electrumXServerPort = NetworkUtils.findFreeSystemPort();
    private final ElectrumXServerRegtestProcess electrumXServerRegtestProcess =
            createElectrumXServerRegtestProcess(bitcoindRegtestSetup);

    public MacLinuxElectrumRegtestSetup() throws IOException {
        this(false);
    }

    public MacLinuxElectrumRegtestSetup(boolean doCreateWallet) throws IOException {
        RemoteBitcoind remoteBitcoind = bitcoindRegtestSetup.getRemoteBitcoind();
        this.electrumRegtest = new ElectrumRegtest(remoteBitcoind, "127.0.0.1", electrumXServerPort, doCreateWallet);
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindRegtestSetup, electrumXServerRegtestProcess, electrumRegtest)
        );
    }

    @Override
    public RemoteBitcoind getRemoteBitcoind() {
        return bitcoindRegtestSetup.getRemoteBitcoind();
    }

    private ElectrumXServerRegtestProcess createElectrumXServerRegtestProcess(BitcoindRegtestSetup bitcoindRegtestSetup)
            throws IOException {
        ElectrumXServerConfig electrumXServerConfig = ElectrumXServerConfig.builder()
                .dataDir(FileUtils.createTempDir())
                .port(electrumXServerPort)
                .rpcPort(NetworkUtils.findFreeSystemPort())
                .bitcoindRpcConfig(bitcoindRegtestSetup.getRpcConfig())
                .build();

        return new ElectrumXServerRegtestProcess(electrumXServerConfig);
    }
}
