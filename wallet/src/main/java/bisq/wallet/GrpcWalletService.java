package bisq.wallet;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.wallet.vo.GrpcUtxo;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.TransactionInfo;
import bisq.wallet.vo.Utxo;
import bisq.wallets.grpc.pb.EncryptWalletRequest;
import bisq.wallets.grpc.pb.GetBalanceRequest;
import bisq.wallets.grpc.pb.GetSeedWordsRequest;
import bisq.wallets.grpc.pb.GetSeedWordsResponse;
import bisq.wallets.grpc.pb.GetUnusedAddressRequest;
import bisq.wallets.grpc.pb.GetUnusedAddressResponse;
import bisq.wallets.grpc.pb.GetWalletAddressesRequest;
import bisq.wallets.grpc.pb.IsWalletEncryptedRequest;
import bisq.wallets.grpc.pb.IsWalletEncryptedResponse;
import bisq.wallets.grpc.pb.IsWalletReadyRequest;
import bisq.wallets.grpc.pb.ListTransactionsRequest;
import bisq.wallets.grpc.pb.ListUnspentRequest;
import bisq.wallets.grpc.pb.SendToAddressRequest;
import bisq.wallets.grpc.pb.SendToAddressResponse;
import bisq.wallets.grpc.pb.WalletServiceGrpc;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GrpcWalletService implements WalletService {

    public record ServerConfig(String host, int port) {
    }

    private final ManagedChannel channel;
    private final WalletServiceGrpc.WalletServiceBlockingStub blockingStub;
    private final WalletServiceGrpc.WalletServiceFutureStub futureStub;

    private final Observable<Coin> balance = new Observable<>();
    private final ObservableSet<Transaction> transactions = new ObservableSet<>();
    private final ObservableSet<String> walletAddresses = new ObservableSet<>();

    public GrpcWalletService(ServerConfig config) {
        this.channel = createChannel(config);
        this.blockingStub = WalletServiceGrpc.newBlockingStub(channel);
        this.futureStub = WalletServiceGrpc.newFutureStub(channel);
    }

    protected ManagedChannel createChannel(ServerConfig config) {
        return ManagedChannelBuilder.forAddress(config.host(), config.port())
                .usePlaintext()
                .build();
    }

    @Override
    public void encryptWallet(String password) {
        EncryptWalletRequest request = EncryptWalletRequest.newBuilder().setPassword(password).build();
        blockingStub.encryptWallet(request);
    }

    @Override
    public CompletableFuture<List<String>> getSeedWords() {
        GetSeedWordsRequest request = GetSeedWordsRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getSeedWords(request))
                .thenApply(GetSeedWordsResponse::getSeedWordsList);
    }

    @Override
    public boolean isWalletReady() {
        IsWalletReadyRequest request = IsWalletReadyRequest.newBuilder().build();
        return blockingStub.isWalletReady(request).getReady();
    }

    @Override
    public CompletableFuture<String> getUnusedAddress() {
        GetUnusedAddressRequest request = GetUnusedAddressRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getUnusedAddress(request))
                .thenApply(GetUnusedAddressResponse::getAddress);
    }

    @Override
    public ObservableSet<String> getWalletAddresses() {
        return walletAddresses;
    }

    @Override
    public CompletableFuture<ObservableSet<String>> requestWalletAddresses() {
        GetWalletAddressesRequest request = GetWalletAddressesRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getWalletAddresses(request))
                .thenApply(response -> {
                    walletAddresses.clear();
                    walletAddresses.addAll(response.getAddressesList());
                    return walletAddresses;
                });
    }

    @Override
    public CompletableFuture<List<? extends TransactionInfo>> listTransactions() {
        ListTransactionsRequest request = ListTransactionsRequest.newBuilder().build();
        return toCompletableFuture(futureStub.listTransactions(request))
                .thenApply(response -> response.getTransactionsList().stream()
                        .map(GrpcTransactionInfo::new)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        ListUnspentRequest request = ListUnspentRequest.newBuilder().build();
        return toCompletableFuture(futureStub.listUnspent(request))
                .thenApply(response -> response.getUtxosList().stream()
                        .map(GrpcUtxo::new)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount) {
        SendToAddressRequest.Builder builder = SendToAddressRequest.newBuilder();
        passphrase.ifPresent(builder::setPassphrase);
        builder.setAddress(address);
        builder.setAmount(amount);
        SendToAddressRequest request = builder.build();

        return toCompletableFuture(futureStub.sendToAddress(request))
                .thenApply(SendToAddressResponse::getTxId);
    }

    @Override
    public CompletableFuture<Boolean> isWalletEncrypted() {
        IsWalletEncryptedRequest request = IsWalletEncryptedRequest.newBuilder().build();
        return toCompletableFuture(futureStub.isWalletEncrypted(request))
                .thenApply(IsWalletEncryptedResponse::getEncrypted);
    }

    @Override
    public CompletableFuture<Coin> requestBalance() {
        GetBalanceRequest request = GetBalanceRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getBalance(request))
                .thenApply(response -> {
                    Coin newBalance = Coin.asBtcFromValue(response.getBalance());
                    balance.set(newBalance);
                    return newBalance;
                });
    }

    @Override
    public Observable<Coin> getBalance() {
        return balance;
    }

    @Override
    public ObservableSet<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public CompletableFuture<ObservableSet<Transaction>> requestTransactions() {
        ListTransactionsRequest request = ListTransactionsRequest.newBuilder().build();
        return toCompletableFuture(futureStub.listTransactions(request))
                .thenApply(response -> {
                    transactions.clear();
                    transactions.addAll(response.getTransactionsList().stream()
                            .map(this::toTransaction)
                            .toList());
                    return transactions;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        channel.shutdown();
        return CompletableFuture.completedFuture(true);
    }

    private Transaction toTransaction(bisq.wallets.grpc.pb.Transaction tx) {
        return new Transaction(
                tx.getTxId(),
                tx.getInputsList().stream().map(this::toTransactionInput).toList(),
                tx.getOutputsList().stream().map(this::toTransactionOutput).toList(),
                tx.getLockTime(),
                tx.getHeight(),
                Optional.of(new Date(tx.getDate() * 1000)),
                tx.getConfirmations(),
                tx.getAmount(),
                tx.getIncoming()
        );
    }

    private bisq.wallet.vo.TransactionInput toTransactionInput(bisq.wallets.grpc.pb.TransactionInput input) {
        return new bisq.wallet.vo.TransactionInput(
                input.getPrevOutTxId(),
                input.getPrevOutIndex(),
                input.getSequence(),
                input.getScriptSig(),
                input.getWitness()
        );
    }

    private bisq.wallet.vo.TransactionOutput toTransactionOutput(bisq.wallets.grpc.pb.TransactionOutput output) {
        return new bisq.wallet.vo.TransactionOutput(
                output.getValue(),
                output.getAddress(),
                output.getScriptPubKey()
        );
    }

    private static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> future) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        }, Executors.newSingleThreadExecutor());
        return completableFuture;
    }

    private record GrpcTransactionInfo(bisq.wallets.grpc.pb.Transaction tx) implements TransactionInfo {

        @Override
        public String getTxId() {
            return tx.getTxId();
        }

        @Override
        public long getAmount() {
            return tx.getAmount();
        }

        @Override
        public int getConfirmations() {
            return tx.getConfirmations();
        }

        @Override
        public Optional<Date> getDate() {
            return Optional.of(new Date(tx.getDate() * 1000));
        }
    }
}