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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class WalletGrpcClientTest {

    private final List<ManagedChannel> channels = new ArrayList<>();

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private WalletGrpc.WalletImplBase serviceImpl;

    private String serverName;
    private Server server;
    private WalletGrpcClient client;

    @BeforeEach
    void setUp() throws Exception {
        serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start();

        client = new WalletGrpcClient(createChannel(serverName));
        client.initialize();
    }

    @AfterEach
    void tearDown() {
        channels.forEach(ManagedChannel::shutdownNow);
        server.shutdownNow();
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

    @Test
    void closedWalletFailsWithWalletNotOpen() {
        doAnswer(invocation -> {
            StreamObserver<GetBalanceResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("wallet is not open; call OpenOrCreateWallet first")
                    .asRuntimeException());
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());

        WalletException exception = getWalletException(client.requestBalance());

        assertEquals(WalletException.Reason.WALLET_NOT_OPEN, exception.getReason());
        StatusRuntimeException cause = assertInstanceOf(StatusRuntimeException.class, exception.getCause());
        assertEquals(Status.Code.FAILED_PRECONDITION, cause.getStatus().getCode());
    }

    @Test
    void rejectedPasswordFailsWithWrongPassword() {
        doAnswer(invocation -> {
            StreamObserver<SendToAddressResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("invalid wallet password")
                    .asRuntimeException());
            return null;
        }).when(serviceImpl).sendToAddress(any(SendToAddressRequest.class), any());

        var request = SendToAddressRequest.newBuilder().setPassphrase("wrong").build();
        WalletException exception = getWalletException(client.sendToAddress(request));

        assertEquals(WalletException.Reason.WRONG_PASSWORD, exception.getReason());
    }

    @Test
    void internalDaemonErrorFailsWithDaemonError() {
        doAnswer(invocation -> {
            StreamObserver<ListTransactionsResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.INTERNAL.withDescription("sync failed").asRuntimeException());
            return null;
        }).when(serviceImpl).listTransactions(any(ListTransactionsRequest.class), any());

        WalletException exception = getWalletException(client.listTransactions());

        assertEquals(WalletException.Reason.DAEMON_ERROR, exception.getReason());
    }

    @Test
    void unreachableDaemonFailsWithDaemonUnavailable() {
        WalletGrpcClient unreachableClient = new WalletGrpcClient(createChannel(InProcessServerBuilder.generateName()));
        unreachableClient.initialize();

        WalletException exception = getWalletException(unreachableClient.requestBalance());

        assertEquals(WalletException.Reason.DAEMON_UNAVAILABLE, exception.getReason());
    }

    @Test
    void callRemovedOnTheDaemonFailsWithUnsupportedCall() {
        WalletException exception = assertThrows(WalletException.class, () -> client.encryptWallet("password"));

        assertEquals(WalletException.Reason.UNSUPPORTED_CALL, exception.getReason());
    }

    @Test
    void asyncCallRemovedOnTheDaemonFailsWithUnsupportedCall() {
        WalletException exception = getWalletException(client.getNewAddress());

        assertEquals(WalletException.Reason.UNSUPPORTED_CALL, exception.getReason());
    }

    @Test
    void blockingDecryptRemovedOnTheDaemonFailsWithUnsupportedCall() {
        WalletException exception = assertThrows(WalletException.class, () -> client.decryptWallet("password"));

        assertEquals(WalletException.Reason.UNSUPPORTED_CALL, exception.getReason());
    }

    @Test
    void callsBeforeInitializeFailWithClientNotReady() {
        WalletGrpcClient uninitializedClient = new WalletGrpcClient(createChannel(serverName));

        assertEquals(WalletException.Reason.CLIENT_NOT_READY,
                getWalletException(uninitializedClient.requestBalance()).getReason());
        assertEquals(WalletException.Reason.CLIENT_NOT_READY,
                assertThrows(WalletException.class, () -> uninitializedClient.encryptWallet("password")).getReason());
    }

    @Test
    void callsAfterShutdownFailWithClientNotReady() {
        client.shutdown();

        assertEquals(WalletException.Reason.CLIENT_NOT_READY,
                getWalletException(client.requestBalance()).getReason());
        assertEquals(WalletException.Reason.CLIENT_NOT_READY,
                assertThrows(WalletException.class, () -> client.decryptWallet("password")).getReason());
    }

    @Test
    void cancellingTheReturnedFutureCancelsTheCall() throws InterruptedException {
        CountDownLatch callCancelled = new CountDownLatch(1);
        doAnswer(invocation -> {
            Context.current().addListener(context -> callCancelled.countDown(), MoreExecutors.directExecutor());
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());

        CompletableFuture<GetBalanceResponse> future = client.requestBalance();
        future.cancel(true);

        assertTrue(future.isCancelled());
        assertTrue(callCancelled.await(5, TimeUnit.SECONDS));
    }

    @Test
    void cancelledStatusFromTheDaemonFailsWithDaemonError() {
        doAnswer(invocation -> {
            StreamObserver<GetBalanceResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.CANCELLED.asRuntimeException());
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());

        CompletableFuture<GetBalanceResponse> future = client.requestBalance();

        assertEquals(WalletException.Reason.DAEMON_ERROR, getWalletException(future).getReason());
        assertFalse(future.isCancelled());
    }

    @Test
    void dependentStagesSeeTheWalletExceptionAsCause() {
        doAnswer(invocation -> {
            StreamObserver<GetBalanceResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.FAILED_PRECONDITION.asRuntimeException());
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());

        CompletableFuture<Long> balance = client.requestBalance().thenApply(GetBalanceResponse::getBalance);

        CompletionException exception = assertThrows(CompletionException.class, balance::join);
        WalletException walletException = assertInstanceOf(WalletException.class, exception.getCause());
        assertEquals(WalletException.Reason.WALLET_NOT_OPEN, walletException.getReason());
    }

    @Test
    void eachFailedCallLogsOneWarningWithoutTheRequest() {
        doAnswer(invocation -> {
            StreamObserver<SendToAddressResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("invalid wallet password").asRuntimeException());
            return null;
        }).when(serviceImpl).sendToAddress(any(SendToAddressRequest.class), any());
        doAnswer(invocation -> {
            StreamObserver<GetBalanceResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(GetBalanceResponse.newBuilder().setBalance(1000L).build());
            responseObserver.onCompleted();
            return null;
        }).when(serviceImpl).getBalance(any(GetBalanceRequest.class), any());
        doAnswer(invocation -> null).when(serviceImpl).isWalletEncrypted(any(IsWalletEncryptedRequest.class), any());

        Logger logger = (Logger) LoggerFactory.getLogger(WalletGrpcClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = SendToAddressRequest.newBuilder().setPassphrase("secret-passphrase").setAddress("bcrt1qaddress").build();
            getWalletException(client.sendToAddress(request));
            client.requestBalance().join();
            client.isWalletEncrypted().cancel(true);
            getWalletException(client.sendToAddress(request));
        } finally {
            logger.detachAppender(appender);
        }

        List<String> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertEquals(2, warnings.size());
        warnings.forEach(warning -> {
            assertTrue(warning.contains("SendToAddress"));
            assertFalse(warning.contains("secret-passphrase"));
        });
    }

    private ManagedChannel createChannel(String name) {
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        channels.add(channel);
        return channel;
    }

    private static WalletException getWalletException(CompletableFuture<?> future) {
        ExecutionException executionException = assertThrows(ExecutionException.class, future::get);
        return assertInstanceOf(WalletException.class, executionException.getCause());
    }
}
