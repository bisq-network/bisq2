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

import bisq.common.data.Pair;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.elementsd.regtest.ElementsdRegtestSetup;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdGetAddressInfoResponse;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementsdSendUnconfirmedTxIntegrationTests extends SharedElementsdInstanceTests {
    @Test
    public void sendOneLBtcToAddress() throws MalformedURLException, InterruptedException {
        peginBtc(20);
        var receiverWallet = elementsdRegtestSetup.createNewWallet("receiver_wallet");

        Pair<ZmqConnection, ZmqListeners> connectionAndListeners = elementsdRegtestSetup.initializeZmqListenersForWallet(receiverWallet);
        ZmqConnection zmqConnection = connectionAndListeners.getFirst();
        ZmqListeners zmqListeners = connectionAndListeners.getSecond();

        CountDownLatch didReceiveNotificationLatch = new CountDownLatch(1);

        String blindedReceiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        String unblindedReceiverAddress = getUnblindedAddress(receiverWallet, blindedReceiverAddress);

        zmqListeners.registerTxOutputAddressesListener(outputAddresses -> {
            if (outputAddresses.contains(unblindedReceiverAddress)) {
                didReceiveNotificationLatch.countDown();
            }
        });

        elementsdMinerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), blindedReceiverAddress, 1);

        boolean await = didReceiveNotificationLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't receive ZMQ notification after 1 minute.");
        }

        assertThat(receiverWallet.getLBtcBalance())
                .isEqualTo(1);

        zmqConnection.close();
    }

    private String getUnblindedAddress(ElementsdWallet wallet, String blindedAddress) {
        ElementsdGetAddressInfoResponse addressInfo = wallet.getAddressInfo(blindedAddress);
        return addressInfo.getResult().getUnconfidential();
    }
}
