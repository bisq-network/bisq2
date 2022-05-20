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

package bisq.wallets.bitcoind.zmq;

import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.core.model.AddressType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BitcoindZeroMqRawTxIntegrationTests extends AbstractBitcoindZeroMqTests {
    private BitcoindWallet receiverWallet;

    @BeforeAll
    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        receiverWallet = regtestSetup.createNewWallet("receiver_wallet");
    }

    @Test
    void detectReceiverAddress() throws InterruptedException {
        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        BitcoindWallet minerWallet = regtestSetup.getMinerWallet();
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");

        bitcoindZeroMq.getListeners().registerTxOutputAddressesListener(outputAddresses -> {
            if (outputAddresses.contains(receiverAddress)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        minerWallet.sendToAddress(receiverAddress, 1);

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }
    }

    @Test
    void detectWhetherMyInputInTx() throws InterruptedException {
        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        BitcoindWallet minerWallet = regtestSetup.getMinerWallet();
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        String myTxId = minerWallet.sendToAddress(receiverAddress, 2);

        bitcoindZeroMq.getListeners().registerTransactionIdInInputListener(txId -> {
            if (txId.equals(myTxId)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        regtestSetup.mineOneBlock();

        String someAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        for (int i = 0; i < 10; i++) {
            receiverWallet.sendToAddress(someAddress, 0.001);
        }

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }
    }
}
