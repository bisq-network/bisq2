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

package bisq.wallets.elementsd;

import bisq.common.util.FileUtils;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElementsdWalletCreationAndListIntegrationTests extends SharedElementsdInstanceTests {
    private Path walletFilePath;

    @BeforeEach
    void setUp() throws IOException {
        Path tmpDir = FileUtils.createTempDir();
        walletFilePath = tmpDir.resolve("wallet");
    }

    @Test
    public void createFreshWallet() {
        // SharedElementsdInstanceTests creates a wallet for the miner automatically.
        assertEquals(0, elementsdMinerWallet.getLBtcBalance());
    }

    @Test
    public void loadWalletIfExisting() {
        assertThat(walletFilePath).doesNotExist();

        // Create Wallet
        elementsdDaemon.createOrLoadWallet(walletFilePath, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));
        assertThat(walletFilePath).exists();

        // Unload and reload existing wallet
        elementsdDaemon.unloadWallet(walletFilePath);
        elementsdDaemon.createOrLoadWallet(walletFilePath, Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE));

        assertThat(elementsdMinerWallet.getLBtcBalance())
                .isZero();

        // Cleanup, otherwise other tests don't start on a clean state.
        elementsdDaemon.unloadWallet(walletFilePath);
    }

    @Test
    void listWallets() {
        List<String> results = elementsdDaemon.listWallets();
        // SharedElementsdInstanceTests creates a wallet for the miner automatically.
        assertThat(results).hasSize(1);
    }
}
