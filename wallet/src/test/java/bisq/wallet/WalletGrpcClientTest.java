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

import bisq.wallet.protobuf.GetBalanceRequest;
import bisq.wallet.protobuf.GetBalanceResponse;
import bisq.wallet.protobuf.GetSeedWordsRequest;
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.GetUnusedAddressRequest;
import bisq.wallet.protobuf.GetUnusedAddressResponse;
import bisq.wallet.protobuf.GetWalletAddressesRequest;
import bisq.wallet.protobuf.GetWalletAddressesResponse;
import bisq.wallet.protobuf.IsWalletEncryptedRequest;
import bisq.wallet.protobuf.IsWalletEncryptedResponse;
import bisq.wallet.protobuf.IsWalletReadyRequest;
import bisq.wallet.protobuf.IsWalletReadyResponse;
import bisq.wallet.protobuf.ListTransactionsRequest;
import bisq.wallet.protobuf.ListTransactionsResponse;
import bisq.wallet.protobuf.ListUtxosRequest;
import bisq.wallet.protobuf.ListUtxosResponse;
import bisq.wallet.protobuf.SendToAddressRequest;
import bisq.wallet.protobuf.SendToAddressResponse;
import bisq.wallet.protobuf.Transaction;
import bisq.wallet.protobuf.WalletGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class WalletGrpcClientTest {

    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private WalletGrpc.WalletImplBase serviceImpl;

    private WalletGrpcClient client;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start());

        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());
        client = new WalletGrpcClient(channel);
        client.initialize();
    }

    @Test
    void getUnusedAddress() throws ExecutionException, InterruptedException {
        var response = GetUnusedAddressResponse.newBuilder()
                .setAddress("test_address")
                .build();
        doAnswer(invocation -> {
            StreamObserver<GetUnusedAddressResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getUnusedAddress(any(GetUnusedAddressRequest.class), any());

        var result = client.getUnusedAddress().get();
        assertEquals("test_address", result.getAddress());
    }

    @Test
    void requestBalance() throws ExecutionException, InterruptedException {
        var response = GetBalanceResponse.newBuilder()
                .setBalance(1000L)
                .build();
        doAnswer(invocation -> {
            StreamObserver<GetBalanceResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());

        var result = client.requestBalance().get();
        assertEquals(1000L, result.getBalance());
    }

    @Test
    void isWalletEncrypted() throws ExecutionException, InterruptedException {
        var response = IsWalletEncryptedResponse.newBuilder()
                .setEncrypted(true)
                .build();
        doAnswer(invocation -> {
            StreamObserver<IsWalletEncryptedResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).isWalletEncrypted(any(IsWalletEncryptedRequest.class), any());

        var result = client.isWalletEncrypted().get();
        assertTrue(result.getEncrypted());
    }

    @Test
    void getSeedWords() throws ExecutionException, InterruptedException {
        var response = GetSeedWordsResponse.newBuilder()
                .addSeedWords("word1")
                .addSeedWords("word2")
                .build();
        doAnswer(invocation -> {
            StreamObserver<GetSeedWordsResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getSeedWords(any(GetSeedWordsRequest.class), any());

        var result = client.getSeedWords().get();
        assertEquals(2, result.getSeedWordsList().size());
        assertEquals("word1", result.getSeedWords(0));
        assertEquals("word2", result.getSeedWords(1));
    }

    @Test
    void listTransactions() throws ExecutionException, InterruptedException {
        var response = ListTransactionsResponse.newBuilder()
                .addTransactions(Transaction.newBuilder().setTxId("tx1").build())
                .build();
        doAnswer(invocation -> {
            StreamObserver<ListTransactionsResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).listTransactions(any(ListTransactionsRequest.class), any());

        var result = client.listTransactions().get();
        assertEquals(1, result.getTransactionsList().size());
        assertEquals("tx1", result.getTransactions(0).getTxId());
    }

    @Test
    void listUtxos() throws ExecutionException, InterruptedException {
        var response = ListUtxosResponse.newBuilder()
                .addUtxos(bisq.wallet.protobuf.Utxo.newBuilder().setTxId("utxo1").build())
                .build();
        doAnswer(invocation -> {
            StreamObserver<ListUtxosResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).listUtxos(any(ListUtxosRequest.class), any());

        var result = client.listUtxos().get();
        assertEquals(1, result.getUtxosList().size());
        assertEquals("utxo1", result.getUtxos(0).getTxId());
    }

    @Test
    void requestWalletAddresses() throws ExecutionException, InterruptedException {
        var response = GetWalletAddressesResponse.newBuilder()
                .addAddresses("address1")
                .addAddresses("address2")
                .build();
        doAnswer(invocation -> {
            StreamObserver<GetWalletAddressesResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getWalletAddresses(any(GetWalletAddressesRequest.class), any());

        var result = client.requestWalletAddresses().get();
        assertEquals(2, result.getAddressesList().size());
        assertEquals("address1", result.getAddresses(0));
        assertEquals("address2", result.getAddresses(1));
    }

    @Test
    void isWalletReady() throws ExecutionException, InterruptedException {
        var response = IsWalletReadyResponse.newBuilder().setReady(true).build();
        doAnswer(invocation -> {
            StreamObserver<IsWalletReadyResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).isWalletReady(any(IsWalletReadyRequest.class), any());

        var result = client.isWalletReady().get();
        assertTrue(result.getReady());
    }

    @Test
    void sendToAddress() throws ExecutionException, InterruptedException {
        var response = SendToAddressResponse.newBuilder().setTxId("sent_tx_id").build();
        doAnswer(invocation -> {
            StreamObserver<SendToAddressResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).sendToAddress(any(SendToAddressRequest.class), any());

        var request = SendToAddressRequest.newBuilder().build();
        var result = client.sendToAddress(request).get();
        assertEquals("sent_tx_id", result.getTxId());
    }
}

