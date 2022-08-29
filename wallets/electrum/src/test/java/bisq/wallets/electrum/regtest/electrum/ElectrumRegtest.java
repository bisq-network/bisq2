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
import bisq.wallets.electrum.ElectrumConfig;
import bisq.wallets.electrum.ElectrumProcess;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import bisq.wallets.process.BisqProcess;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;

public class ElectrumRegtest implements BisqProcess {

    private final RemoteBitcoind remoteBitcoind;
    @Getter
    private final ElectrumProcess electrumProcess;
    private final boolean doCreateWallet;

    @Getter
    private ElectrumDaemon electrumDaemon;
    @Getter
    private ElectrumCreateResponse walletInfo;

    public ElectrumRegtest(RemoteBitcoind remoteBitcoind, String electrumXHost, int electrumXServerPort, boolean doCreateWallet) throws IOException {
        this.remoteBitcoind = remoteBitcoind;
        this.electrumProcess = createElectrumProcess(electrumXHost, electrumXServerPort);
        this.doCreateWallet = doCreateWallet;
    }

    @Override
    public void start() {
        electrumProcess.start();

        electrumDaemon = electrumProcess.getElectrumDaemon().orElseThrow();
        if (doCreateWallet) {
            walletInfo = electrumDaemon.create(AbstractRegtestSetup.WALLET_PASSPHRASE);
            electrumDaemon.loadWallet(AbstractRegtestSetup.WALLET_PASSPHRASE);
        }
    }

    @Override
    public void shutdown() {
        electrumProcess.shutdown();
    }

    public void fundWallet(ElectrumDaemon receiverWallet, double amount) throws InterruptedException {
        String unusedAddress = receiverWallet.getUnusedAddress();
        fundAddress(unusedAddress, amount);
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        return remoteBitcoind.fundAddress(address, amount);
    }

    private ElectrumProcess createElectrumProcess(String electrumXHost, int electrumXServerPort) throws IOException {
        Path electrumRootDataDir = FileUtils.createTempDir();
        ElectrumConfig electrumConfig = ElectrumConfig.builder()
                .rpcPort(String.valueOf(NetworkUtils.findFreeSystemPort()))
                .rpcUser("bisq")
                .rpcPassword("bisq")
                .build();

        ElectrumProcessConfig config = ElectrumProcessConfig.builder()
                .dataDir(electrumRootDataDir)
                .electrumXServerHost(electrumXHost)
                .electrumXServerPort(electrumXServerPort)
                .electrumConfig(electrumConfig)
                .build();
        return new ElectrumProcess(electrumRootDataDir, config);
    }
}
