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

import bisq.wallets.bitcoind.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.calls.BitcoindCreateWalletRpcCall;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindCreateWalletNameDocumentationIntegrationTests {
    private final Path dataDir;
    private final DaemonRpcClient rpcClient;
    private final BitcoindDaemon daemon;

    public BitcoindCreateWalletNameDocumentationIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.dataDir = regtestSetup.getDataDir();
        this.daemon = regtestSetup.getDaemon();
        RpcConfig rpcConfig = regtestSetup.getRpcConfig();
        this.rpcClient = RpcClientFactory.createLegacyDaemonRpcClient(rpcConfig);
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void createWalletWithAbsolutePath(@TempDir Path walletPath) {
        String walletName = walletPath.toAbsolutePath().toString();
        var request = BitcoindCreateWalletRpcCall.Request.builder()
                .walletName(walletName)
                .passphrase(AbstractRegtestSetup.WALLET_PASSPHRASE)
                .build();

        var rpcCall = new BitcoindCreateWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);

        File walletFile = walletPath.resolve("wallet.dat")
                .toFile();
        assertThat(walletFile).exists();

        daemon.unloadWallet(walletName);
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void createWalletWithRelativePath() throws IOException {
        File bitcoindWalletsDir = dataDir.resolve("regtest")
                .resolve("wallets")
                .toFile();
        Path newWalletDir = Files.createTempDirectory(bitcoindWalletsDir.toPath(), "bisq_");

        String walletName = newWalletDir.getFileName() + "/b";
        var request = BitcoindCreateWalletRpcCall.Request.builder()
                .walletName(walletName)
                .passphrase(AbstractRegtestSetup.WALLET_PASSPHRASE)
                .build();

        var rpcCall = new BitcoindCreateWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);

        File expectedWalletFile = newWalletDir.resolve("b")
                .resolve("wallet.dat")
                .toFile();
        assertThat(expectedWalletFile).exists();

        daemon.unloadWallet(walletName);
    }
}
