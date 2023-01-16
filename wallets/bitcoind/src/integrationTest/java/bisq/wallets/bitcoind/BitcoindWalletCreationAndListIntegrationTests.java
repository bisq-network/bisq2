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
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindWalletCreationAndListIntegrationTests {

    private final Path dataDir;
    private final RpcConfig rpcConfig;
    private final BitcoindDaemon daemon;
    private final BitcoindWallet minerWallet;

    public BitcoindWalletCreationAndListIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.dataDir = regtestSetup.getDataDir();
        this.rpcConfig = regtestSetup.getRpcConfig();
        this.daemon = regtestSetup.getDaemon();
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @Test
    public void createFreshWallet() {
        String walletName = "fresh_wallet";

        Path walletFilePath = dataDir.resolve("regtest")
                .resolve("wallets")
                .resolve(walletName)
                .resolve("wallet.dat");
        assertThat(walletFilePath).doesNotExist();

        // Create Wallet
        daemon.createOrLoadWallet(walletName, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));
        assertThat(walletFilePath).exists();

        // Unload and reload existing wallet
        daemon.unloadWallet(walletName);
    }

    @Test
    public void loadWalletIfExisting() throws MalformedURLException {
        String walletName = "wallet_load_if_existing";

        Path walletFilePath = dataDir.resolve("regtest")
                .resolve("wallets")
                .resolve(walletName)
                .resolve("wallet.dat");
        assertThat(walletFilePath).doesNotExist();

        // Create Wallet
        daemon.createOrLoadWallet(walletName, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));
        assertThat(walletFilePath).exists();

        // Unload and reload existing wallet
        daemon.unloadWallet(walletName);
        daemon.createOrLoadWallet(walletName, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));

        var testWallet = new BitcoindWallet(daemon, rpcConfig, walletName);
        assertThat(testWallet.getBalance())
                .isZero();

        // Cleanup, otherwise other tests don't start on a clean state.
        daemon.unloadWallet(walletName);
    }

    @Test
    void listWallets() {
        List<String> results = daemon.listWallets();
        assertThat(results).contains(RemoteBitcoind.MINER_WALLET_NAME);
    }
}
