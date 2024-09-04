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

import bisq.wallets.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindCreateWalletNameIntegrationTests {
    private final BitcoindDaemon daemon;

    public BitcoindCreateWalletNameIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.daemon = regtestSetup.getDaemon();
    }

    @Test
    void createWallet() {
        String walletName = "My_awesome_wallet_creation";
        daemon.createOrLoadWallet(walletName, Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE));

        List<String> loadedWallets = daemon.listWallets();
        assertThat(loadedWallets).contains(walletName);

        daemon.unloadWallet(walletName);
    }

    @Test
    void loadWallet() {
        String walletName = "My_awesome_wallet_loading";
        daemon.createOrLoadWallet(walletName, Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE));
        daemon.unloadWallet(walletName);

        daemon.createOrLoadWallet(walletName, Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE));

        List<String> loadedWallets = daemon.listWallets();
        assertThat(loadedWallets).contains(walletName);

        daemon.unloadWallet(walletName);
    }
}
