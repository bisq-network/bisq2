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
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerRegtestProcess;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

public class ElectrumRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator, ElectrumDaemon> {

    private static final String LOCALHOST = "127.0.0.1";
    private final Path electrumDataDir = FileUtils.createTempDir();

    private final boolean doCreateWallet;

    // Mine initial regtest blocks automatically
    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup = new BitcoindRegtestSetup(true);
    private final ElectrumXServerRegtestProcess electrumXServerRegtestProcess =
            createElectrumXServerRegtestProcess(bitcoindRegtestSetup);
    private final ElectrumRegtestProcess electrumRegtestProcess =
            createElectrumRegtestProcess(electrumXServerRegtestProcess);

    private ElectrumDaemon electrumDaemon;
    @Getter
    private ElectrumCreateResponse walletInfo;

    public ElectrumRegtestSetup() throws IOException {
        this(false);
    }

    public ElectrumRegtestSetup(boolean doCreateWallet) throws IOException {
        this.doCreateWallet = doCreateWallet;
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindRegtestSetup, electrumXServerRegtestProcess, electrumRegtestProcess)
        );
    }

    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        bitcoindRegtestSetup.mineInitialRegtestBlocks();

        electrumDaemon = createElectrumDaemon();

        if (doCreateWallet) {
            walletInfo = electrumDaemon.create(WALLET_PASSPHRASE);
            electrumDaemon.loadWallet(WALLET_PASSPHRASE);
        }
    }

    @Override
    public List<String> mineOneBlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fundWallet(ElectrumDaemon receiverWallet, double amount) {
        String address = electrumDaemon.getUnusedAddress();
        fundAddress(address, amount);
    }

    public String fundAddress(String address, double amount) {
        return bitcoindRegtestSetup.fundAddress(address, amount);
    }

    @Override
    public RpcConfig getRpcConfig() {
        return electrumRegtestProcess.getRpcConfig();
    }

    public ElectrumDaemon createElectrumDaemon() throws MalformedURLException {
        RpcConfig rpcConfig = electrumRegtestProcess.getRpcConfig();
        DaemonRpcClient daemonRpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new ElectrumDaemon(daemonRpcClient);
    }

    private ElectrumXServerRegtestProcess createElectrumXServerRegtestProcess(BitcoindRegtestSetup bitcoindRegtestSetup)
            throws IOException {
        Path electrumXDataDir = FileUtils.createTempDir();
        int electrumXServerPort = NetworkUtils.findFreeSystemPort();
        ElectrumXServerConfig electrumXServerConfig = ElectrumXServerConfig.builder()
                .dataDir(electrumXDataDir)
                .port(electrumXServerPort)
                .rpcPort(NetworkUtils.findFreeSystemPort())
                .bitcoindRpcConfig(bitcoindRegtestSetup.getRpcConfig())
                .build();

        return new ElectrumXServerRegtestProcess(electrumXServerConfig);
    }

    private ElectrumRegtestProcess createElectrumRegtestProcess(ElectrumXServerRegtestProcess
                                                                        electrumXServerRegtestProcess) {
        int freePort = NetworkUtils.findFreeSystemPort();
        var rpcHostSpec = new RpcHostSpec(LOCALHOST, freePort);
        int electrumXServerPort = electrumXServerRegtestProcess.getPort();
        return new ElectrumRegtestProcess(electrumXServerPort, rpcHostSpec, electrumDataDir);
    }

    public Path getDataDir() {
        return electrumRegtestProcess.getDataDir();
    }
}
