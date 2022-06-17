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

import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class BitcoindSendUnconfirmedTxIntegrationTests extends SharedBitcoindInstanceTests {

    private BitcoindWallet receiverBackend;

    @Override
    @BeforeAll
    public void start() throws IOException, InterruptedException {
        super.start();
        regtestSetup.mineInitialRegtestBlocks();
        receiverBackend = regtestSetup.createNewWallet("receiver_wallet");
    }

    @Test
    public void sendBtcAndCheckIfUnconfirmedBalanceIncluded() {
        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWallet.sendToAddress(Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE), receiverAddress, 1);

        assertThat(receiverBackend.getBalance())
                .isEqualTo(1);
    }
}
