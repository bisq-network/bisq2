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
import bisq.wallets.core.model.AddressType;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindMineInitialRegtestBlocksIntegrationTest {

    private final BitcoindRegtestSetup regtestSetup;
    private final BitcoindDaemon daemon;
    private final BitcoindWallet minerWallet;

    public BitcoindMineInitialRegtestBlocksIntegrationTest(BitcoindRegtestSetup regtestSetup) {
        this.regtestSetup = regtestSetup;
        this.daemon = regtestSetup.getDaemon();
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @Test
    public void mineInitialRegtestBlocks() throws InterruptedException {
        String address = minerWallet.getNewAddress(AddressType.BECH32, "");
        List<String> blockHashes = daemon.generateToAddress(101, address);

        CountDownLatch nBlocksMinedLatch = regtestSetup.waitUntilBlocksMined(blockHashes);
        boolean allBlocksMined = nBlocksMinedLatch.await(1, TimeUnit.MINUTES);
        assertThat(allBlocksMined).isTrue();

        assertThat(minerWallet.getBalance())
                .isEqualTo(50);
    }
}
