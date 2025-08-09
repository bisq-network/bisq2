package bisq.wallet;

import bisq.common.monetary.Coin;
import bisq.wallet.vo.TransactionInfo;
import bisq.wallet.vo.Utxo;
import bisq.wallets.grpc.pb.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GrpcWalletServiceIT {

    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private GrpcWalletService walletService;

    private final WalletServiceGrpc.WalletServiceImplBase serviceImpl =
            new WalletServiceGrpc.WalletServiceImplBase() {
                @Override
                public void getUnusedAddress(bisq.wallets.grpc.pb.GetUnusedAddressRequest request,
                                             io.grpc.stub.StreamObserver<bisq.wallets.grpc.pb.GetUnusedAddressResponse> responseObserver) {
                    GetUnusedAddressResponse response = GetUnusedAddressResponse.newBuilder()
                            .setAddress("test_address")
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

                @Override
                public void getBalance(bisq.wallets.grpc.pb.GetBalanceRequest request,
                                       io.grpc.stub.StreamObserver<bisq.wallets.grpc.pb.GetBalanceResponse> responseObserver) {
                    GetBalanceResponse response = GetBalanceResponse.newBuilder()
                            .setBalance(1000L)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

                @Override
                public void isWalletEncrypted(bisq.wallets.grpc.pb.IsWalletEncryptedRequest request,
                                              io.grpc.stub.StreamObserver<bisq.wallets.grpc.pb.IsWalletEncryptedResponse> responseObserver) {
                    IsWalletEncryptedResponse response = IsWalletEncryptedResponse.newBuilder()
                            .setEncrypted(true)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

                @Override
                public void getSeedWords(GetSeedWordsRequest request,
                                         StreamObserver<GetSeedWordsResponse> responseObserver) {
                    GetSeedWordsResponse response = GetSeedWordsResponse.newBuilder()
                            .addSeedWords("word1")
                            .addSeedWords("word2")
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

                @Override
                public void listTransactions(ListTransactionsRequest request,
                                             StreamObserver<ListTransactionsResponse> responseObserver) {
                    ListTransactionsResponse response = ListTransactionsResponse.newBuilder()
                            .addTransactions(Transaction.newBuilder().setTxId("tx1").build())
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

                @Override
                public void listUnspent(ListUnspentRequest request,
                                        StreamObserver<ListUnspentResponse> responseObserver) {
                    ListUnspentResponse response = ListUnspentResponse.newBuilder()
                            .addUtxos(bisq.wallets.grpc.pb.Utxo.newBuilder().setTxId("utxo1").build())
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            };

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start());

        walletService = new GrpcWalletService(new GrpcWalletService.ServerConfig("localhost", 0)) {
            @Override
            protected io.grpc.ManagedChannel createChannel(ServerConfig config) {
                return grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
            }
        };
    }

    @Test
    void getUnusedAddress() throws ExecutionException, InterruptedException {
        CompletableFuture<String> unusedAddress = walletService.getUnusedAddress();
        assertEquals("test_address", unusedAddress.get());
    }

    @Test
    void getBalance() throws ExecutionException, InterruptedException {
        CompletableFuture<Coin> balance = walletService.requestBalance();
        assertEquals(Coin.asBtcFromValue(1000L), balance.get());
    }

    @Test
    void isWalletEncrypted() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> walletEncrypted = walletService.isWalletEncrypted();
        assertTrue(walletEncrypted.get());
    }

    @Test
    void getSeedWords() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> seedWords = walletService.getSeedWords();
        assertEquals(List.of("word1", "word2"), seedWords.get());
    }

    @Test
    void listTransactions() throws ExecutionException, InterruptedException {
        CompletableFuture<List<? extends TransactionInfo>> transactions = walletService.listTransactions();
        assertEquals(1, transactions.get().size());
        assertEquals("tx1", transactions.get().get(0).getTxId());
    }

    @Test
    void listUnspent() throws ExecutionException, InterruptedException {
        CompletableFuture<List<? extends Utxo>> utxos = walletService.listUnspent();
        assertEquals(1, utxos.get().size());
        assertEquals("utxo1", utxos.get().get(0).getTxId());
    }
}
