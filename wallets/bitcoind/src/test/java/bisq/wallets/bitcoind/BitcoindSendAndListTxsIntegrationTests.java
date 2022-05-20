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
import bisq.wallets.bitcoind.rpc.responses.BitcoindListTransactionsResponseEntry;
import bisq.wallets.core.model.AddressType;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitcoindSendAndListTxsIntegrationTests extends SharedBitcoindInstanceTests {
    @Test
    public void sendBtcAndListTxs() throws MalformedURLException {
        regtestSetup.mineInitialRegtestBlocks();
        BitcoindWallet minerWallet = regtestSetup.getMinerWallet();

        var receiverBackend = regtestSetup.createNewWallet("receiver_wallet");

        String firstTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWallet.sendToAddress(firstTxReceiverAddress, 1);

        String secondTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWallet.sendToAddress(secondTxReceiverAddress, 1);

        String thirdTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWallet.sendToAddress(thirdTxReceiverAddress, 1);

        regtestSetup.mineOneBlock();

        List<BitcoindListTransactionsResponseEntry> txs = receiverBackend.listTransactions(10);
        assertEquals(3, txs.size());

        BitcoindListTransactionsResponseEntry firstTx = txs.get(0);
        assertEquals(firstTxReceiverAddress, firstTx.getAddress());
        assertEquals("receive", firstTx.getCategory());
        assertEquals(1, firstTx.getAmount());
        assertEquals(1, firstTx.getConfirmations());
        assertEquals(102, firstTx.getBlockheight());
        assertEquals(0, firstTx.getWalletconflicts().length);
        assertEquals("no", firstTx.getBip125Replaceable());

        BitcoindListTransactionsResponseEntry secondTx = txs.get(1);
        assertEquals(secondTxReceiverAddress, secondTx.getAddress());
        assertEquals("receive", secondTx.getCategory());
        assertEquals(1, secondTx.getAmount());
        assertEquals(1, secondTx.getConfirmations());
        assertEquals(102, secondTx.getBlockheight());
        assertEquals(0, secondTx.getWalletconflicts().length);
        assertEquals("no", secondTx.getBip125Replaceable());

        BitcoindListTransactionsResponseEntry thirdTx = txs.get(2);
        assertEquals(thirdTxReceiverAddress, thirdTx.getAddress());
        assertEquals("receive", thirdTx.getCategory());
        assertEquals(1, thirdTx.getAmount());
        assertEquals(1, thirdTx.getConfirmations());
        assertEquals(102, thirdTx.getBlockheight());
        assertEquals(0, thirdTx.getWalletconflicts().length);
        assertEquals("no", thirdTx.getBip125Replaceable());
    }
}
