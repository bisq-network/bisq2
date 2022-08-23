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

package bisq.wallets.electrum;

import bisq.common.util.NetworkUtils;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.electrum.notifications.ElectrumNotifyApi;
import bisq.wallets.electrum.notifications.ElectrumNotifyWebServer;
import bisq.wallets.electrum.regtest.ElectrumExtension;
import bisq.wallets.electrum.regtest.electrum.ElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.MacLinuxElectrumRegtestSetup;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ElectrumExtension.class)
public class ElectrumSendIntegrationTests {

    private final RemoteBitcoind remoteBitcoind;
    private final ElectrumRegtestSetup electrumRegtestSetup;
    private ElectrumDaemon electrumDaemon;

    public ElectrumSendIntegrationTests(RemoteBitcoind remoteBitcoind,
                                        ElectrumRegtestSetup electrumRegtestSetup) {
        this.remoteBitcoind = remoteBitcoind;
        this.electrumRegtestSetup = electrumRegtestSetup;
    }

    @BeforeEach
    void setUp() {
        electrumDaemon = electrumRegtestSetup.getElectrumDaemon();
    }

    @Test
    void sendBtcTest() throws InterruptedException {
        var electrumProcessedTxLatch = new CountDownLatch(1);
        ElectrumNotifyApi.registerListener((address, status) -> {
            if (status != null) {
                electrumProcessedTxLatch.countDown();
            }
        });

        int freePort = NetworkUtils.findFreeSystemPort();
        ElectrumNotifyWebServer electrumNotifyWebServer = new ElectrumNotifyWebServer(freePort);
        electrumNotifyWebServer.startServer();

        String unusedAddress = electrumDaemon.getUnusedAddress();
        electrumDaemon.notify(unusedAddress, electrumNotifyWebServer.getNotifyEndpointUrl());

        electrumRegtestSetup.fundAddress(unusedAddress, 10);

        // Wait until electrum sees transaction
        boolean isSuccess = electrumProcessedTxLatch.await(30, TimeUnit.SECONDS);
        assertThat(isSuccess).isTrue();

        assertThat(electrumDaemon.getBalance()).isEqualTo(10);

        BitcoindWallet minerWallet = remoteBitcoind.getMinerWallet();
        double balanceBefore = minerWallet.getBalance();
        String receiverAddress = minerWallet.getNewAddress(AddressType.BECH32, "");

        String unsignedTx = electrumDaemon.payTo(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE, receiverAddress, 5);
        String signedTx = electrumDaemon.signTransaction(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE, unsignedTx);

        String txId = electrumDaemon.broadcast(signedTx);
        assertThat(txId).isNotEmpty();

        double receivedAmount = minerWallet.getBalance() - balanceBefore;
        assertThat(receivedAmount).isEqualTo(5);

        electrumNotifyWebServer.stopServer();
    }
}
