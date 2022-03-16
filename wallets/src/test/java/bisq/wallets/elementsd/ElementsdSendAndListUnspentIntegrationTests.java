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

import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponseEntry;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdListUnspentResponseEntry;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElementsdSendAndListUnspentIntegrationTests extends SharedElementsdInstanceTests {
    @Test
    public void listUnspent() throws MalformedURLException {
        peginBtc(20);
        ElementsdWallet receiverBackend = elementsdRegtestSetup.createNewWallet("receiver_wallet");

        String firstTxId = elementsdRegtestSetup.sendBtcAndMineOneBlock(elementsdMinerWallet, receiverBackend, 1);
        String secondTxId = elementsdRegtestSetup.sendBtcAndMineOneBlock(elementsdMinerWallet, receiverBackend, 1);
        String thirdTxId = elementsdRegtestSetup.sendBtcAndMineOneBlock(elementsdMinerWallet, receiverBackend, 1);

        List<ElementsdListUnspentResponseEntry> utxos = receiverBackend.listUnspent();
        assertEquals(3, utxos.size());

        Optional<ElementsdListUnspentResponseEntry> queryResult = elementsdRegtestSetup
                .filterUtxosByTxId(utxos, firstTxId);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponseEntry firstUtxo = queryResult.get();
        assertEquals("", firstUtxo.getLabel());
        assertEquals(1, firstUtxo.getAmount());
        assertEquals(3, firstUtxo.getConfirmations());
        assertTrue(firstUtxo.isSpendable());
        assertTrue(firstUtxo.isSolvable());
        assertTrue(firstUtxo.isSafe());

        queryResult = elementsdRegtestSetup.filterUtxosByTxId(utxos, secondTxId);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponseEntry secondUtxo = queryResult.get();
        assertEquals("", secondUtxo.getLabel());
        assertEquals(1, secondUtxo.getAmount());
        assertEquals(2, secondUtxo.getConfirmations());
        assertTrue(secondUtxo.isSpendable());
        assertTrue(secondUtxo.isSolvable());
        assertTrue(secondUtxo.isSafe());

        queryResult = elementsdRegtestSetup.filterUtxosByTxId(utxos, thirdTxId);
        assertTrue(queryResult.isPresent());

        BitcoindListUnspentResponseEntry thirdUtxo = queryResult.get();
        assertEquals("", thirdUtxo.getLabel());
        assertEquals(1, thirdUtxo.getAmount());
        assertEquals(1, thirdUtxo.getConfirmations());
        assertTrue(thirdUtxo.isSpendable());
        assertTrue(thirdUtxo.isSolvable());
        assertTrue(thirdUtxo.isSafe());
    }
}
