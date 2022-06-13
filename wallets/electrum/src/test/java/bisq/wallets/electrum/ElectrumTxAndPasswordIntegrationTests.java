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
import bisq.wallets.electrum.rpc.responses.ElectrumDeserializeOutputResponse;
import bisq.wallets.electrum.rpc.responses.ElectrumDeserializeResponse;
import bisq.wallets.electrum.rpc.responses.ElectrumListUnspentResponseEntry;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ElectrumExtension.class)
public class ElectrumTxAndPasswordIntegrationTests {


    private final BitcoindRegtestSetup bitcoindRegtestSetup;
    private final ElectrumRegtestSetup electrumRegtestSetup;
    private ElectrumDaemon electrumDaemon;

    private String fundingAddress;
    private String fundingTxId;

    public ElectrumTxAndPasswordIntegrationTests(BitcoindRegtestSetup bitcoindRegtestSetup,
                                                 ElectrumRegtestSetup electrumRegtestSetup) {
        this.bitcoindRegtestSetup = bitcoindRegtestSetup;
        this.electrumRegtestSetup = electrumRegtestSetup;
    }

    @BeforeEach
    void setUp() throws MalformedURLException {
        electrumDaemon = electrumRegtestSetup.createElectrumDaemon();
    }

    @Test
    void changePasswordTest() {
        String expectedSeed = electrumDaemon.getSeed(ElectrumRegtestSetup.WALLET_PASSPHRASE);

        String newPassword = "My new password.";
        electrumDaemon.password(ElectrumRegtestSetup.WALLET_PASSPHRASE, newPassword);

        String seed = electrumDaemon.getSeed(newPassword);
        assertThat(seed).isEqualTo(expectedSeed);

        // Change back otherwise other tests could fail.
        electrumDaemon.password(newPassword, ElectrumRegtestSetup.WALLET_PASSPHRASE);
    }

    @Test
    void listUnspentAndGetTxTest() throws InterruptedException {
        fundElectrumWallet();

        List<ElectrumListUnspentResponseEntry> unspentResponseEntries = electrumDaemon.listUnspent();
        assertThat(unspentResponseEntries).hasSize(1);

        ElectrumListUnspentResponseEntry firstEntry = unspentResponseEntries.get(0);
        assertThat(firstEntry.getAddress()).isEqualTo(fundingAddress);
        assertThat(firstEntry.getValue()).isEqualTo("10");

        String tx = electrumDaemon.getTransaction(fundingTxId);
        ElectrumDeserializeResponse deserializedTx = electrumDaemon.deserialize(tx);
        ElectrumDeserializeOutputResponse[] outputs = deserializedTx.getOutputs();

        assertThat(outputs).hasSize(2);

        boolean hasFundingAddress = false;
        for (ElectrumDeserializeOutputResponse o : outputs) {
            if (o.getAddress().equals(fundingAddress) && o.getValueSats() == 1_000_000_000) {
                hasFundingAddress = true;
                break;
            }
        }
        assertThat(hasFundingAddress).isTrue();
    }

    private void fundElectrumWallet() throws InterruptedException {
        var electrumProcessedTxLatch = new CountDownLatch(1);
        ElectrumNotifyApi.registerListener((address, status) -> {
            if (status != null) {
                electrumProcessedTxLatch.countDown();
            }
        });

        int freePort = NetworkUtils.findFreeSystemPort();
        ElectrumNotifyWebServer electrumNotifyWebServer = new ElectrumNotifyWebServer(freePort);
        electrumNotifyWebServer.startServer();

        fundingAddress = electrumDaemon.getUnusedAddress();
        electrumDaemon.notify(fundingAddress, electrumNotifyWebServer.getNotifyEndpointUrl());

        fundingTxId = electrumRegtestSetup.fundAddress(fundingAddress, 10);

        // Wait until electrum sees transaction
        boolean isSuccess = electrumProcessedTxLatch.await(30, TimeUnit.SECONDS);
        assertThat(isSuccess).isTrue();

        BitcoindWallet minerWallet = bitcoindRegtestSetup.getMinerWallet();
        String receiverAddress = minerWallet.getNewAddress(AddressType.BECH32, "");

        String unsignedTx = electrumDaemon.payTo(receiverAddress, 5, ElectrumRegtestSetup.WALLET_PASSPHRASE);
        String signedTx = electrumDaemon.signTransaction(unsignedTx, ElectrumRegtestSetup.WALLET_PASSPHRASE);

        electrumDaemon.broadcast(signedTx);
        electrumNotifyWebServer.stopServer();
    }
}
