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
import bisq.wallets.bitcoind.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(BitcoindExtension.class)
public class BitcoindWalletCreationAndListIntegrationTests {

    private final BitcoindDaemon daemon;
    private final BitcoindWallet minerWallet;

    public BitcoindWalletCreationAndListIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.daemon = regtestSetup.getDaemon();
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @Test
    public void createFreshWallet() {
        assertEquals(0, minerWallet.getBalance());
    }

    @Test
    public void loadWalletIfExisting() throws IOException {
        Path tmpDir = FileUtils.createTempDir();
        Path walletFilePath = tmpDir.resolve("wallet");
        assertThat(walletFilePath).doesNotExist();

        // Create Wallet
        daemon.createOrLoadWallet(walletFilePath, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));
        assertThat(walletFilePath).exists();

        // Unload and reload existing wallet
        daemon.unloadWallet(walletFilePath);
        daemon.createOrLoadWallet(walletFilePath, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));

        assertThat(minerWallet.getBalance())
                .isZero();

        // Cleanup, otherwise other tests don't start on a clean state.
        daemon.unloadWallet(walletFilePath);
    }

    @Test
    void listWallets() {
        List<String> results = daemon.listWallets();
        assertThat(results).hasSize(1);
    }
}
