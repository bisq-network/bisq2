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
import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponse;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BitcoindExtension.class)
public class BitcoindSendAndListUnspentIntegrationTests {

    private final BitcoindRegtestSetup regtestSetup;

    public BitcoindSendAndListUnspentIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.regtestSetup = regtestSetup;
    }

    @Test
    public void listUnspent() throws MalformedURLException, InterruptedException {
        BitcoindWallet minerWallet = regtestSetup.getMinerWallet();

        BitcoindWallet receiverBackend = regtestSetup.createAndInitializeNewWallet("receiver_wallet_list_unspent");

        String firstTxReceiverAddress = regtestSetup.sendBtcAndMineOneBlock(minerWallet, receiverBackend, 1);
        String secondTxReceiverAddress = regtestSetup.sendBtcAndMineOneBlock(minerWallet, receiverBackend, 1);
        String thirdTxReceiverAddress = regtestSetup.sendBtcAndMineOneBlock(minerWallet, receiverBackend, 1);

        List<BitcoindListUnspentResponse.Entry> utxos = receiverBackend.listUnspent();
        assertEquals(3, utxos.size());

        Optional<BitcoindListUnspentResponse.Entry> queryResult = regtestSetup
                .filterUtxosByAddress(utxos, firstTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponse.Entry firstUtxo = queryResult.get();
        assertEquals("", firstUtxo.getLabel());
        assertEquals(1, firstUtxo.getAmount());
        assertEquals(3, firstUtxo.getConfirmations());
        assertTrue(firstUtxo.isSpendable());
        assertTrue(firstUtxo.isSolvable());
        assertTrue(firstUtxo.isSafe());

        queryResult = regtestSetup.filterUtxosByAddress(utxos, secondTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponse.Entry secondUtxo = queryResult.get();
        assertEquals("", secondUtxo.getLabel());
        assertEquals(1, secondUtxo.getAmount());
        assertEquals(2, secondUtxo.getConfirmations());
        assertTrue(secondUtxo.isSpendable());
        assertTrue(secondUtxo.isSolvable());
        assertTrue(secondUtxo.isSafe());

        queryResult = regtestSetup.filterUtxosByAddress(utxos, thirdTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponse.Entry thirdUtxo = queryResult.get();
        assertEquals("", thirdUtxo.getLabel());
        assertEquals(1, thirdUtxo.getAmount());
        assertEquals(1, thirdUtxo.getConfirmations());
        assertTrue(thirdUtxo.isSpendable());
        assertTrue(thirdUtxo.isSolvable());
        assertTrue(thirdUtxo.isSafe());
    }
}
