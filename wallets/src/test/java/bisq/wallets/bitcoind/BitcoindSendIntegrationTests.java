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

import bisq.common.util.FileUtils;
import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.responses.ListTransactionsResponseEntry;
import bisq.wallets.bitcoind.responses.ListUnspentResponseEntry;
import bisq.wallets.bitcoind.rpc.RpcClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BitcoindSendIntegrationTests {

    private Path tmpDirPath;
    private Path walletFilePath;

    private BitcoindProcess bitcoindProcess;
    private BitcoindChainBackend chainBackend;
    private BitcoindWalletBackend walletBackend;

    @BeforeEach
    public void setUp() throws IOException {
        bitcoindProcess = BitcoindRegtestSetup.createAndStartBitcoind();
        tmpDirPath = FileUtils.createTempDir();

        walletFilePath = tmpDirPath.resolve("wallet");
        assertFalse(walletFilePath.toFile().exists());

        chainBackend = new BitcoindChainBackend(new RpcClient(BitcoindRegtestSetup.RPC_CONFIG));
        chainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        RpcClient walletRpcClient = BitcoindRegtestSetup.createWalletRpcClient(walletFilePath);
        walletBackend = new BitcoindWalletBackend(walletRpcClient);
        walletBackend.walletPassphrase(BitcoindRegtestSetup.WALLET_PASSPHRASE, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
    }

    @AfterEach
    public void cleanUp() {
        chainBackend.unloadWallet(walletFilePath);
        bitcoindProcess.stopAndWaitUntilStopped();
    }

    @Test
    public void mineInitialRegtestBlocks() {
        String address = walletBackend.getNewAddress(AddressType.BECH32, "");
        chainBackend.generateToAddress(101, address);
        assertEquals(50, walletBackend.getBalance());
    }

    @Test
    public void sendOneBtcToAddress() throws MalformedURLException {
        mineInitialRegtestBlocks();
        var receiverBackend = BitcoindRegtestSetup
                .createTestWalletBackend(chainBackend, tmpDirPath, "receiver_wallet");

        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        walletBackend.sendToAddress(receiverAddress, 1);

        String minerAddress = walletBackend.getNewAddress(AddressType.BECH32, "");
        chainBackend.generateToAddress(1, minerAddress);

        double receivedAmount = receiverBackend.getBalance();
        assertEquals(1, receivedAmount);
    }

    @Test
    public void sendBtcAndListTxs() throws MalformedURLException {
        mineInitialRegtestBlocks();
        var receiverBackend = BitcoindRegtestSetup
                .createTestWalletBackend(chainBackend, tmpDirPath, "receiver_wallet");

        String firstTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        walletBackend.sendToAddress(firstTxReceiverAddress, 1);

        String secondTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        walletBackend.sendToAddress(secondTxReceiverAddress, 1);

        String thirdTxReceiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        walletBackend.sendToAddress(thirdTxReceiverAddress, 1);

        String minerAddress = walletBackend.getNewAddress(AddressType.BECH32, "");
        chainBackend.generateToAddress(1, minerAddress);

        List<ListTransactionsResponseEntry> txs = receiverBackend.listTransactions(10);
        assertEquals(3, txs.size());

        ListTransactionsResponseEntry firstTx = txs.get(0);
        assertEquals(firstTxReceiverAddress, firstTx.getAddress());
        assertEquals("receive", firstTx.getCategory());
        assertEquals(1, firstTx.getAmount());
        assertEquals(1, firstTx.getConfirmations());
        assertEquals(102, firstTx.getBlockheight());
        assertEquals(0, firstTx.getWalletconflicts().length);
        assertEquals("no", firstTx.getBip125Replaceable());

        ListTransactionsResponseEntry secondTx = txs.get(1);
        assertEquals(secondTxReceiverAddress, secondTx.getAddress());
        assertEquals("receive", secondTx.getCategory());
        assertEquals(1, secondTx.getAmount());
        assertEquals(1, secondTx.getConfirmations());
        assertEquals(102, secondTx.getBlockheight());
        assertEquals(0, secondTx.getWalletconflicts().length);
        assertEquals("no", secondTx.getBip125Replaceable());

        ListTransactionsResponseEntry thirdTx = txs.get(2);
        assertEquals(thirdTxReceiverAddress, thirdTx.getAddress());
        assertEquals("receive", thirdTx.getCategory());
        assertEquals(1, thirdTx.getAmount());
        assertEquals(1, thirdTx.getConfirmations());
        assertEquals(102, thirdTx.getBlockheight());
        assertEquals(0, thirdTx.getWalletconflicts().length);
        assertEquals("no", thirdTx.getBip125Replaceable());
    }

    @Test
    public void listUnspent() throws MalformedURLException {
        mineInitialRegtestBlocks();
        BitcoindWalletBackend receiverBackend = BitcoindRegtestSetup
                .createTestWalletBackend(chainBackend, tmpDirPath, "receiver_wallet");

        String firstTxReceiverAddress = BitcoindRegtestSetup.sendBtcAndMineOneBlock(chainBackend, walletBackend, receiverBackend, 1);
        String secondTxReceiverAddress = BitcoindRegtestSetup.sendBtcAndMineOneBlock(chainBackend, walletBackend, receiverBackend, 1);
        String thirdTxReceiverAddress = BitcoindRegtestSetup.sendBtcAndMineOneBlock(chainBackend, walletBackend, receiverBackend, 1);

        List<ListUnspentResponseEntry> utxos = receiverBackend.listUnspent();
        assertEquals(3, utxos.size());

        Optional<ListUnspentResponseEntry> queryResult = BitcoindRegtestSetup
                .filterUtxosByAddress(utxos, firstTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        ListUnspentResponseEntry firstUtxo = queryResult.get();
        assertEquals("", firstUtxo.getLabel());
        assertEquals(1, firstUtxo.getAmount());
        assertEquals(3, firstUtxo.getConfirmations());
        assertTrue(firstUtxo.isSpendable());
        assertTrue(firstUtxo.isSolvable());
        assertTrue(firstUtxo.isSafe());

        queryResult = BitcoindRegtestSetup.filterUtxosByAddress(utxos, secondTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        ListUnspentResponseEntry secondUtxo = queryResult.get();
        assertEquals("", secondUtxo.getLabel());
        assertEquals(1, secondUtxo.getAmount());
        assertEquals(2, secondUtxo.getConfirmations());
        assertTrue(secondUtxo.isSpendable());
        assertTrue(secondUtxo.isSolvable());
        assertTrue(secondUtxo.isSafe());

        queryResult = BitcoindRegtestSetup.filterUtxosByAddress(utxos, thirdTxReceiverAddress);
        assertTrue(queryResult.isPresent());

        ListUnspentResponseEntry thirdUtxo = queryResult.get();
        assertEquals("", thirdUtxo.getLabel());
        assertEquals(1, thirdUtxo.getAmount());
        assertEquals(1, thirdUtxo.getConfirmations());
        assertTrue(thirdUtxo.isSpendable());
        assertTrue(thirdUtxo.isSolvable());
        assertTrue(thirdUtxo.isSafe());
    }
}
