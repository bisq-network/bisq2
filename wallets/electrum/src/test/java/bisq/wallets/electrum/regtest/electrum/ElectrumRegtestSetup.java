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
import bisq.wallets.core.RpcConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerRegtestProcess;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ElectrumRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator, ElectrumDaemon> {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup = new BitcoindRegtestSetup();

    private final int electrumXServerPort = NetworkUtils.findFreeSystemPort();
    private final ElectrumXServerRegtestProcess electrumXServerRegtestProcess =
            createElectrumXServerRegtestProcess(bitcoindRegtestSetup);

    private final ElectrumRegtest electrumRegtest;

    public ElectrumRegtestSetup() throws IOException {
        this(false);
    }

    public ElectrumRegtestSetup(boolean doCreateWallet) throws IOException {
        RemoteBitcoind remoteBitcoind = bitcoindRegtestSetup.getRemoteBitcoind();
        this.electrumRegtest = new ElectrumRegtest(remoteBitcoind, electrumXServerPort, doCreateWallet);
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindRegtestSetup, electrumXServerRegtestProcess, electrumRegtest)
        );
    }

    @Override
    public List<String> mineOneBlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fundWallet(ElectrumDaemon receiverWallet, double amount) throws InterruptedException {
        electrumRegtest.fundWallet(receiverWallet, amount);
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        return electrumRegtest.fundAddress(address, amount);
    }

    @Override
    public RpcConfig getRpcConfig() {
        throw new UnsupportedOperationException();
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

    public Path getElectrumDataDir() {
        return electrumRegtest.getElectrumProcess().getDataDir();
    }

    public ElectrumDaemon getElectrumDaemon() {
        return electrumRegtest.getElectrumDaemon();
    }

    public ElectrumCreateResponse getWalletInfo() {
        return electrumRegtest.getWalletInfo();
    }
}
