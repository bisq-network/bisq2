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

import bisq.persistence.PersistenceService;
import bisq.wallet.protobuf.GetWalletAddressesRequest;
import bisq.wallet.protobuf.GetWalletAddressesResponse;
import bisq.wallet.protobuf.ListTransactionsRequest;
import bisq.wallet.protobuf.ListTransactionsResponse;
import bisq.wallet.protobuf.Transaction;
import bisq.wallet.protobuf.TransactionOutput;
import bisq.wallet.protobuf.WalletGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletServiceTransactionsTest {
    private static final String RECIPIENT = "bcrt1qpzm8wsr0zsc9kdr6hd3zmxqyng2cwgymtn3svg";
    private static final String CHANGE = "bcrt1pdu7d0lhu2fg5vmmnch2rzead4ruwaj3nz97l3t6tfd0elm5e39ds290a9r";

    @TempDir
    private Path tempDirPath;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private WalletGrpc.WalletImplBase serviceImpl;

    private Server server;
    private ManagedChannel channel;
    private WalletService walletService;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        WalletGrpcClient client = new WalletGrpcClient(channel);
        client.initialize();

        WalletService.Config config = new WalletService.Config(true, "localhost", 50051);
        walletService = new WalletService(config, new PersistenceService(tempDirPath), client);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void publishedTransactionsMarkOutputsPaidToWalletAddresses() throws ExecutionException, InterruptedException {
        respondWithTransactions(transaction("send", CHANGE, RECIPIENT));
        respondWithAddresses(CHANGE);

        walletService.requestTransactions().get();

        var transaction = walletService.getTransactions().iterator().next();
        assertEquals(List.of(true, false), ownership(transaction));
        assertEquals(List.of(CHANGE, RECIPIENT), addresses(transaction));
    }

    @Test
    void listedTransactionsAreMarkedToo() throws ExecutionException, InterruptedException {
        respondWithTransactions(transaction("send", RECIPIENT, CHANGE));
        respondWithAddresses(CHANGE);

        var transactions = walletService.listTransactions().get();

        assertEquals(List.of(false, true), ownership(transactions.get(0)));
    }

    @Test
    void addressesAreRequestedAfterTheTransactionList() throws ExecutionException, InterruptedException {
        respondWithTransactions(transaction("send", CHANGE, RECIPIENT));
        respondWithAddresses(CHANGE);

        walletService.requestTransactions().get();

        InOrder inOrder = inOrder(serviceImpl);
        inOrder.verify(serviceImpl).listTransactions(any(ListTransactionsRequest.class), any());
        inOrder.verify(serviceImpl).getWalletAddresses(any(GetWalletAddressesRequest.class), any());
    }

    @Test
    void failedTransactionListSkipsTheAddressRequest() {
        doAnswer(invocation -> {
            StreamObserver<ListTransactionsResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.INTERNAL.asRuntimeException());
            return null;
        }).when(serviceImpl).listTransactions(any(ListTransactionsRequest.class), any());

        assertThrows(ExecutionException.class, () -> walletService.requestTransactions().get());
        verify(serviceImpl, never()).getWalletAddresses(any(GetWalletAddressesRequest.class), any());
    }

    @Test
    void failedAddressRequestKeepsThePublishedTransactions() throws ExecutionException, InterruptedException {
        respondWithTransactions(transaction("first", CHANGE, RECIPIENT));
        respondWithAddresses(CHANGE);
        walletService.requestTransactions().get();
        var published = Set.copyOf(walletService.getTransactions());

        AtomicInteger notifications = new AtomicInteger();
        walletService.getTransactions().addObserver(notifications::incrementAndGet);
        int notificationsAfterRegistration = notifications.get();
        respondWithTransactions(transaction("second", RECIPIENT));
        doAnswer(invocation -> {
            StreamObserver<GetWalletAddressesResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
            return null;
        }).when(serviceImpl).getWalletAddresses(any(GetWalletAddressesRequest.class), any());

        assertThrows(ExecutionException.class, () -> walletService.requestTransactions().get());
        assertEquals(published, Set.copyOf(walletService.getTransactions()));
        assertEquals(notificationsAfterRegistration, notifications.get());
    }

    private void respondWithTransactions(Transaction... transactions) {
        doAnswer(invocation -> {
            StreamObserver<ListTransactionsResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(ListTransactionsResponse.newBuilder().addAllTransactions(List.of(transactions)).build());
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).listTransactions(any(ListTransactionsRequest.class), any());
    }

    private void respondWithAddresses(String... addresses) {
        doAnswer(invocation -> {
            StreamObserver<GetWalletAddressesResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(GetWalletAddressesResponse.newBuilder().addAllAddresses(List.of(addresses)).build());
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getWalletAddresses(any(GetWalletAddressesRequest.class), any());
    }

    private static Transaction transaction(String txId, String... outputAddresses) {
        var builder = Transaction.newBuilder().setTxId(txId);
        for (String address : outputAddresses) {
            builder.addOutputs(TransactionOutput.newBuilder().setValue(10_000_000L).setAddress(address));
        }
        return builder.build();
    }

    private static List<Boolean> ownership(bisq.wallet.vo.Transaction transaction) {
        return transaction.getOutputs().stream().map(bisq.wallet.vo.TransactionOutput::isMine).toList();
    }

    private static List<String> addresses(bisq.wallet.vo.Transaction transaction) {
        return transaction.getOutputs().stream().map(bisq.wallet.vo.TransactionOutput::getAddress).toList();
    }
}
