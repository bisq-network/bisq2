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

package bisq.wallets.elementsd.zmq;

import bisq.wallets.core.model.AddressType;
import bisq.wallets.elementsd.regtest.ElementsdRegtestSetup;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdGetAddressInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ElementsdZeroMqRawTxIntegrationTests extends AbstractElementsdZeroMqTests {

    @Test
    void detectReceiverAddress() throws InterruptedException {
        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        ElementsdWallet minerWallet = elementsdRegtestSetup.getMinerWallet();
        String blindedReceiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");

        ElementsdGetAddressInfoResponse addressInfo = receiverWallet.getAddressInfo(blindedReceiverAddress);
        String unblindedReceiverAddress = addressInfo.getResult().getUnconfidential();

        receiverWalletZmqConnection.getListeners().registerTxOutputAddressesListener(outputAddresses -> {
            if (outputAddresses.contains(unblindedReceiverAddress)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        minerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), blindedReceiverAddress, 0.001);

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }
    }

    @Test
    void detectWhetherMyInputInTx() throws InterruptedException {
        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        ElementsdWallet minerWallet = elementsdRegtestSetup.getMinerWallet();
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        String myTxId = minerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), receiverAddress, 2);

        minerWalletZmqConnection.getListeners().registerTransactionIdInInputListener(txId -> {
            if (txId.equals(myTxId)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        elementsdRegtestSetup.mineOneBlock();

        String someAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        for (int i = 0; i < 10; i++) {
            receiverWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), someAddress, 0.001);
        }

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }
    }
}
