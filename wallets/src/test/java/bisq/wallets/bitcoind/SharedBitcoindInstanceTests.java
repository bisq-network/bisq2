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

package bisq.wallets.bitcoind;

import bisq.common.util.FileUtils;
import bisq.wallets.bitcoind.rpc.RpcClient;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SharedBitcoindInstanceTests {
    protected RpcClient rpcClient;
    protected BitcoindProcess bitcoindProcess;

    protected BitcoindChainBackend minerChainBackend;
    protected BitcoindWalletBackend minerWalletBackend;

    protected Path tmpDirPath;
    protected Path walletFilePath;

    @BeforeAll
    public void startBitcoind() throws IOException {
        bitcoindProcess = BitcoindRegtestSetup.createAndStartBitcoind();
    }

    @AfterAll
    public void stopBitcoind() {
        bitcoindProcess.stopAndWaitUntilStopped();
    }

    @BeforeEach
    public void setUp() throws IOException {
        tmpDirPath = FileUtils.createTempDir();
        walletFilePath = tmpDirPath.resolve("wallet");
        assertFalse(walletFilePath.toFile().exists());

        RpcConfig rpcConfig = bitcoindProcess.getRpcConfig();
        rpcClient = new RpcClient(rpcConfig);

        minerChainBackend = new BitcoindChainBackend(rpcClient);
        minerChainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        RpcClient walletRpcClient = BitcoindRegtestSetup.createWalletRpcClient(rpcConfig, walletFilePath);
        minerWalletBackend = new BitcoindWalletBackend(walletRpcClient);
        minerWalletBackend.walletPassphrase(BitcoindRegtestSetup.WALLET_PASSPHRASE, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
    }

    @AfterEach
    public void cleanUp() {
        minerChainBackend.unloadWallet(walletFilePath);
    }
}
