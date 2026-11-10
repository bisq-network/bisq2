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

package bisq.wallet;

import bisq.common.monetary.Coin;
import bisq.persistence.PersistenceService;
import bisq.wallet.protobuf.GetBalanceRequest;
import bisq.wallet.protobuf.GetBalanceResponse;
import bisq.wallet.protobuf.GetSeedWordsRequest;
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.GetUnusedAddressRequest;
import bisq.wallet.protobuf.GetUnusedAddressResponse;
import bisq.wallet.protobuf.GetWalletAddressesRequest;
import bisq.wallet.protobuf.GetWalletAddressesResponse;
import bisq.wallet.protobuf.ListTransactionsRequest;
import bisq.wallet.protobuf.ListTransactionsResponse;
import bisq.wallet.protobuf.ListUtxosRequest;
import bisq.wallet.protobuf.ListUtxosResponse;
import bisq.wallet.protobuf.OpenOrCreateWalletRequest;
import bisq.wallet.protobuf.OpenOrCreateWalletResponse;
import bisq.wallet.protobuf.PubAddressInfo;
import bisq.wallet.protobuf.SendToAddressRequest;
import bisq.wallet.protobuf.SendToAddressResponse;
import bisq.wallet.protobuf.TransactionOutput;
import bisq.wallet.protobuf.Utxo;
import bisq.wallet.protobuf.WalletGrpc;
import bisq.wallet.receive_address.ReceiveAddressEntry;
import bisq.wallet.vo.BalanceBreakdown;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The real WalletService against an in-process daemon stand-in: what goes into the requests, what comes out of
// the responses.
class WalletServiceGrpcTest {
    @TempDir
    private Path tempDirPath;

    private final List<Object> requests = new ArrayList<>();
    private final List<String> unusedAddresses = new ArrayList<>();
    private Server server;
    private ManagedChannel channel;
    private PersistenceService persistenceService;
    private WalletService walletService;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new RecordingWallet())
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        WalletGrpcClient client = new WalletGrpcClient(channel);
        client.initialize();
        persistenceService = new PersistenceService(tempDirPath);
        walletService = new WalletService(new WalletService.Config(true, "localhost", 0, false), persistenceService, client);
    }

    // The store writes asynchronously and JUnit fails when a write still creates files in the temp dir after the test.
    // A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        persistenceService.persistAllClients().join();
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void openOrCreateWalletSendsThePassword() throws ExecutionException, InterruptedException {
        walletService.openOrCreateWallet("Wallet-Pa55!").get();

        assertEquals("Wallet-Pa55!", ((OpenOrCreateWalletRequest) requests.get(0)).getPassword());
    }

    @Test
    void getSeedWordsSendsThePassword() throws ExecutionException, InterruptedException {
        List<String> seedWords = walletService.getSeedWords("Wallet-Pa55!").get();

        assertEquals("Wallet-Pa55!", ((GetSeedWordsRequest) requests.get(0)).getPassword());
        assertEquals(List.of("word1", "word2"), seedWords);
    }

    @Test
    void sendToAddressWrapsTheAddressAndSendsThePassphrase() throws ExecutionException, InterruptedException {
        String txId = walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L).get();

        SendToAddressRequest request = (SendToAddressRequest) requests.get(0);
        assertEquals("Wallet-Pa55!", request.getPassphrase());
        assertEquals("bcrt1qreceiver", request.getAddress().getAddress());
        assertEquals(1234L, request.getAmount());
        assertEquals("sent_tx_id", txId);
    }

    @Test
    void aFeeRateGoesOutOnlyWhenGiven() throws ExecutionException, InterruptedException {
        walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L, OptionalLong.of(750L)).get();
        walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L).get();

        SendToAddressRequest withFeeRate = (SendToAddressRequest) requests.get(0);
        SendToAddressRequest withoutFeeRate = (SendToAddressRequest) requests.get(1);
        assertTrue(withFeeRate.hasFeeRatePerKwu());
        assertEquals(750L, withFeeRate.getFeeRatePerKwu());
        assertFalse(withoutFeeRate.hasFeeRatePerKwu());
    }

    @Test
    void requestBalanceKeepsTheSpendableTotalAndPublishesTheBreakdown() throws ExecutionException, InterruptedException {
        Coin total = walletService.requestBalance().get();

        assertEquals(2000L, total.getValue());
        assertEquals(Coin.asBtcFromValue(2000), walletService.getBalance().get());
        assertEquals(new BalanceBreakdown(2000, 1500, 500, 7, 3), walletService.getBalanceBreakdown().get());
    }

    @Test
    void anAbsentPassphraseIsSentEmpty() throws ExecutionException, InterruptedException {
        walletService.sendToAddress(Optional.empty(), "bcrt1qreceiver", 1234L).get();

        assertEquals("", ((SendToAddressRequest) requests.get(0)).getPassphrase());
    }

    @Test
    void getUnusedAddressStoresTheAddressOnce() throws ExecutionException, InterruptedException {
        unusedAddresses.add("bcrt1qfirst");
        unusedAddresses.add("bcrt1qfirst");

        ReceiveAddressEntry first = walletService.getUnusedAddress().get();
        ReceiveAddressEntry again = walletService.getUnusedAddress().get();

        assertEquals("bcrt1qfirst", first.getAddress());
        assertSame(first, again);
        assertEquals(Optional.of(first), walletService.findReceiveAddressEntry("bcrt1qfirst"));
    }

    @Test
    void createReceiveAddressKeepsTheNoteOfAnAddressTheDaemonHandsOutAgain() throws ExecutionException, InterruptedException {
        unusedAddresses.add("bcrt1qcycled");
        unusedAddresses.add("bcrt1qcycled");
        ReceiveAddressEntry first = walletService.createReceiveAddress().get();
        walletService.updateReceiveAddress(first, Optional.of("rent"));

        ReceiveAddressEntry again = walletService.createReceiveAddress().get();

        assertEquals("bcrt1qcycled", again.getAddress());
        assertEquals(Optional.of("rent"), again.getNote());
    }

    @Test
    void requestWalletAddressesUnwrapsTheAddresses() throws ExecutionException, InterruptedException {
        var addresses = walletService.requestWalletAddresses().get();

        assertTrue(addresses.contains("bcrt1qone"));
        assertTrue(addresses.contains("bcrt1qtwo"));
        assertEquals(2, addresses.size());
    }

    @Test
    void outputsAndUtxosWithoutAStandardAddressHaveAnEmptyAddress() throws ExecutionException, InterruptedException {
        List<bisq.wallet.vo.Transaction> transactions = walletService.listTransactions().get();
        List<bisq.wallet.vo.Utxo> utxos = walletService.listUtxos().get();

        assertEquals("bcrt1qpaid", transactions.get(0).getOutputs().get(0).getAddress());
        assertEquals("", transactions.get(0).getOutputs().get(1).getAddress());
        assertEquals("bcrt1qowned", utxos.get(0).getAddress());
        assertEquals("", utxos.get(1).getAddress());
    }

    private class RecordingWallet extends WalletGrpc.WalletImplBase {
        @Override
        public void openOrCreateWallet(OpenOrCreateWalletRequest request, StreamObserver<OpenOrCreateWalletResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, OpenOrCreateWalletResponse.newBuilder().setSuccess(true).build());
        }

        @Override
        public void getSeedWords(GetSeedWordsRequest request, StreamObserver<GetSeedWordsResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, GetSeedWordsResponse.newBuilder().addSeedWords("word1").addSeedWords("word2").build());
        }

        @Override
        public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, GetBalanceResponse.newBuilder()
                    .setBalance(2000)
                    .setConfirmed(1500)
                    .setTrustedPending(500)
                    .setUntrustedPending(7)
                    .setImmature(3)
                    .build());
        }

        @Override
        public void sendToAddress(SendToAddressRequest request, StreamObserver<SendToAddressResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, SendToAddressResponse.newBuilder().setTxId("sent_tx_id").build());
        }

        @Override
        public void getUnusedAddress(GetUnusedAddressRequest request, StreamObserver<GetUnusedAddressResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, GetUnusedAddressResponse.newBuilder()
                    .setAddress(PubAddressInfo.newBuilder().setAddress(unusedAddresses.remove(0)))
                    .build());
        }

        @Override
        public void getWalletAddresses(GetWalletAddressesRequest request, StreamObserver<GetWalletAddressesResponse> responseObserver) {
            reply(responseObserver, GetWalletAddressesResponse.newBuilder()
                    .addAddresses(PubAddressInfo.newBuilder().setAddress("bcrt1qone"))
                    .addAddresses(PubAddressInfo.newBuilder().setAddress("bcrt1qtwo"))
                    .build());
        }

        @Override
        public void listTransactions(ListTransactionsRequest request, StreamObserver<ListTransactionsResponse> responseObserver) {
            reply(responseObserver, ListTransactionsResponse.newBuilder()
                    .addTransactions(bisq.wallet.protobuf.Transaction.newBuilder()
                            .setTxId("tx1")
                            .addOutputs(TransactionOutput.newBuilder().setValue(1L)
                                    .setAddress(PubAddressInfo.newBuilder().setAddress("bcrt1qpaid")))
                            .addOutputs(TransactionOutput.newBuilder().setValue(2L)))
                    .build());
        }

        @Override
        public void listUtxos(ListUtxosRequest request, StreamObserver<ListUtxosResponse> responseObserver) {
            reply(responseObserver, ListUtxosResponse.newBuilder()
                    .addUtxos(Utxo.newBuilder().setTxId("tx1").setAmount(1L)
                            .setAddress(PubAddressInfo.newBuilder().setAddress("bcrt1qowned")))
                    .addUtxos(Utxo.newBuilder().setTxId("tx2").setAmount(2L))
                    .build());
        }

        private <T> void reply(StreamObserver<T> responseObserver, T response) {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
