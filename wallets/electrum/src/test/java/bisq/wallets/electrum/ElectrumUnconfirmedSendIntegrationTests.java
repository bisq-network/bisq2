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
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ElectrumExtension.class)
public class ElectrumUnconfirmedSendIntegrationTests {
    private final BitcoindRegtestSetup bitcoindRegtestSetup;
    private final ElectrumRegtestSetup electrumRegtestSetup;
    private ElectrumDaemon electrumDaemon;

    public ElectrumUnconfirmedSendIntegrationTests(BitcoindRegtestSetup bitcoindRegtestSetup,
                                                   ElectrumRegtestSetup electrumRegtestSetup) {
        this.bitcoindRegtestSetup = bitcoindRegtestSetup;
        this.electrumRegtestSetup = electrumRegtestSetup;
    }

    @BeforeEach
    void setUp() throws MalformedURLException {
        electrumDaemon = electrumRegtestSetup.createElectrumDaemon();
    }

    @Test
    void unconfirmedBalanceTest() throws InterruptedException {
        Map<String, CountDownLatch> addressToLatchMap = new ConcurrentHashMap<>();
        ElectrumNotifyApi.registerListener((address, status) -> {
            CountDownLatch latch = addressToLatchMap.get(address);
            if (status != null) {
                latch.countDown();
            }
        });

        int freePort = NetworkUtils.findFreeSystemPort();
        ElectrumNotifyWebServer electrumNotifyWebServer = new ElectrumNotifyWebServer(freePort);
        electrumNotifyWebServer.startServer();

        String unusedAddress = electrumDaemon.getUnusedAddress();
        var electrumProcessedTxLatch = new CountDownLatch(1);
        addressToLatchMap.put(unusedAddress, electrumProcessedTxLatch);

        electrumDaemon.notify(unusedAddress, electrumNotifyWebServer.getNotifyEndpointUrl());
        electrumRegtestSetup.fundAddress(unusedAddress, 10);

        // Wait until electrum sees transaction
        boolean isSuccess = electrumProcessedTxLatch.await(30, TimeUnit.SECONDS);
        assertThat(isSuccess).isTrue();

        assertThat(electrumDaemon.getBalance()).isEqualTo(10);

        BitcoindWallet minerWallet = bitcoindRegtestSetup.getMinerWallet();
        String newAddress = minerWallet.getNewAddress(AddressType.BECH32, "");

        var electrumTxLatch = new CountDownLatch(1);
        addressToLatchMap.put(newAddress, electrumTxLatch);
        electrumDaemon.notify(newAddress, electrumNotifyWebServer.getNotifyEndpointUrl());

        String unsignedTx = electrumDaemon.payTo(newAddress, 5, ElectrumRegtestSetup.WALLET_PASSPHRASE);
        String signedTx = electrumDaemon.signTransaction(unsignedTx, ElectrumRegtestSetup.WALLET_PASSPHRASE);

        String txId = electrumDaemon.broadcast(signedTx);
        assertThat(txId).isNotEmpty();

        isSuccess = electrumTxLatch.await(30, TimeUnit.SECONDS);
        assertThat(isSuccess).isTrue();

        double balance = electrumDaemon.getBalance();
        assertThat(balance)
                .isGreaterThan(4.9) // Because of tx fees
                .isLessThan(5);

        electrumNotifyWebServer.stopServer();
    }
}
