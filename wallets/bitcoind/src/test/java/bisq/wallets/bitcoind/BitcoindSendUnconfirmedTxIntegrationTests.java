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
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BitcoindSendUnconfirmedTxIntegrationTests {

    private final BitcoindRegtestSetup regtestSetup;
    private final ZmqListeners zmqListeners;
    private final BitcoindWallet minerWallet;

    private BitcoindWallet receiverWallet;

    public BitcoindSendUnconfirmedTxIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.regtestSetup = regtestSetup;
        this.zmqListeners = regtestSetup.getZmqListeners();
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @BeforeAll
    public void start() throws IOException, InterruptedException {
        regtestSetup.mineInitialRegtestBlocks();
        receiverWallet = regtestSetup.createAndInitializeNewWallet("receiver_wallet");
    }

    @Test
    public void sendBtcAndCheckIfUnconfirmedBalanceIncluded() throws InterruptedException {
        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        zmqListeners.registerTxOutputAddressesListener(outputAddresses -> {
            if (outputAddresses.contains(receiverAddress)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        minerWallet.sendToAddress(Optional.of(BitcoindRegtestSetup.WALLET_PASSPHRASE), receiverAddress, 1);

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }

        // Bitcoin Core needs a while until it updates the balance
        double balance;
        for (int i = 0; i < 10; i++) {
            balance = receiverWallet.getBalance();
            if (balance == 1) {
                break;
            }

            Thread.sleep(200);
        }

        assertThat(receiverWallet.getBalance())
                .isEqualTo(1);
    }
}
