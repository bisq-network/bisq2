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

import bisq.wallets.core.model.AddressType;
import bisq.wallets.elementsd.regtest.ElementsdRegtestSetup;
import bisq.wallets.elementsd.rpc.responses.ElementsdListTransactionsResponse;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElementsdSendAndListTxsIntegrationTests extends SharedElementsdInstanceTests {
    @Test
    public void sendLBtcAndListTxs() throws MalformedURLException, InterruptedException {
        peginBtc(20);
        var receiverBackend = elementsdRegtestSetup.createNewWallet("receiver_wallet");

        String firstTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        String firstTxId = elementsdMinerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), firstTxReceiverAddress, 1);

        String secondTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        String secondTxId = elementsdMinerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), secondTxReceiverAddress, 1);

        String thirdTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        String thirdTxId = elementsdMinerWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), thirdTxReceiverAddress, 1);

        elementsdRegtestSetup.mineOneBlock();

        List<ElementsdListTransactionsResponse.Entry> txs = receiverBackend.listTransactions(10);
        assertEquals(3, txs.size());

        ElementsdListTransactionsResponse.Entry firstTx = txs.get(0);
        assertEquals(firstTxId, firstTx.getTxId());
        assertEquals("receive", firstTx.getCategory());
        assertEquals(1, firstTx.getAmount());
        assertEquals(1, firstTx.getConfirmations());
        assertEquals(3, firstTx.getBlockheight());
        assertEquals(0, firstTx.getWalletconflicts().length);
        assertEquals("no", firstTx.getBip125Replaceable());

        ElementsdListTransactionsResponse.Entry secondTx = txs.get(1);
        assertEquals(secondTxId, secondTx.getTxId());
        assertEquals("receive", secondTx.getCategory());
        assertEquals(1, secondTx.getAmount());
        assertEquals(1, secondTx.getConfirmations());
        assertEquals(3, secondTx.getBlockheight());
        assertEquals(0, secondTx.getWalletconflicts().length);
        assertEquals("no", secondTx.getBip125Replaceable());

        ElementsdListTransactionsResponse.Entry thirdTx = txs.get(2);
        assertEquals(thirdTxId, thirdTx.getTxId());
        assertEquals("receive", thirdTx.getCategory());
        assertEquals(1, thirdTx.getAmount());
        assertEquals(1, thirdTx.getConfirmations());
        assertEquals(3, thirdTx.getBlockheight());
        assertEquals(0, thirdTx.getWalletconflicts().length);
        assertEquals("no", thirdTx.getBip125Replaceable());
    }
}
