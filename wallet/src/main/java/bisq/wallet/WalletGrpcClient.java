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

import bisq.common.application.Service;
import bisq.wallet.protobuf.DecryptWalletRequest;
import bisq.wallet.protobuf.EncryptWalletRequest;
import bisq.wallet.protobuf.GetBalanceRequest;
import bisq.wallet.protobuf.GetBalanceResponse;
import bisq.wallet.protobuf.GetSeedWordsRequest;
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.GetUnusedAddressRequest;
import bisq.wallet.protobuf.GetUnusedAddressResponse;
import bisq.wallet.protobuf.GetNewAddressRequest;
import bisq.wallet.protobuf.GetNewAddressResponse;
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
import bisq.wallet.protobuf.WalletGrpc;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static bisq.common.util.CompletableFutureUtils.toCompletableFuture;

@Slf4j
public class WalletGrpcClient implements Service {
    private ManagedChannel managedChannel;
    private WalletGrpc.WalletBlockingStub blockingStub;
    private WalletGrpc.WalletFutureStub futureStub;
    private final String host;
    private final int port;

    public WalletGrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @VisibleForTesting
    WalletGrpcClient(ManagedChannel managedChannel) {
        this.managedChannel = managedChannel;
        this.host = null;
        this.port = 0;
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        if (managedChannel == null) {
            this.managedChannel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    // .useTransportSecurity() //Todo turn this on in prod env
                    .build();
        }
        this.blockingStub = WalletGrpc.newBlockingStub(managedChannel);
        this.futureStub = WalletGrpc.newFutureStub(managedChannel);

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (managedChannel != null) {
            managedChannel.shutdown();
            try {
                if (!managedChannel.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    managedChannel.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Thread got interrupted at while shutting down WalletGrpcClient", e);
                Thread.currentThread().interrupt(); // Restore interrupted state

                managedChannel.shutdownNow();
            }
            managedChannel = null;
            futureStub = null;
            blockingStub = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    public void encryptWallet(String password) {
        var request = EncryptWalletRequest.newBuilder().setPassword(password).build();
        callBlocking("EncryptWallet", stub -> stub.encryptWallet(request));
    }

    public void decryptWallet(String password) {
        var request = DecryptWalletRequest.newBuilder().setPassword(password).build();
        callBlocking("DecryptWallet", stub -> stub.decryptWallet(request));
    }

    public CompletableFuture<GetSeedWordsResponse> getSeedWords() {
        var request = GetSeedWordsRequest.newBuilder().build();
        return call("GetSeedWords", stub -> stub.getSeedWords(request));
    }

    public CompletableFuture<IsWalletReadyResponse> isWalletReady() {
        var request = IsWalletReadyRequest.newBuilder().build();
        return call("IsWalletReady", stub -> stub.isWalletReady(request));
    }

    public CompletableFuture<GetUnusedAddressResponse> getUnusedAddress() {
        var request = GetUnusedAddressRequest.newBuilder().build();
        return call("GetUnusedAddress", stub -> stub.getUnusedAddress(request));
    }

    public CompletableFuture<GetNewAddressResponse> getNewAddress() {
        var request = GetNewAddressRequest.newBuilder().build();
        return call("GetNewAddress", stub -> stub.getNewAddress(request));
    }

    public CompletableFuture<GetWalletAddressesResponse> requestWalletAddresses() {
        var request = GetWalletAddressesRequest.newBuilder().build();
        return call("GetWalletAddresses", stub -> stub.getWalletAddresses(request));
    }

    public CompletableFuture<ListTransactionsResponse> listTransactions() {
        var request = ListTransactionsRequest.newBuilder().build();
        return call("ListTransactions", stub -> stub.listTransactions(request));
    }

    public CompletableFuture<ListUtxosResponse> listUtxos() {
        var request = ListUtxosRequest.newBuilder().build();
        return call("ListUtxos", stub -> stub.listUtxos(request));
    }

    public CompletableFuture<SendToAddressResponse> sendToAddress(SendToAddressRequest request) {
        return call("SendToAddress", stub -> stub.sendToAddress(request));
    }

    public CompletableFuture<IsWalletEncryptedResponse> isWalletEncrypted() {
        var request = IsWalletEncryptedRequest.newBuilder().build();
        return call("IsWalletEncrypted", stub -> stub.isWalletEncrypted(request));
    }

    public CompletableFuture<GetBalanceResponse> requestBalance() {
        var request = GetBalanceRequest.newBuilder().build();
        return call("GetBalance", stub -> stub.getBalance(request));
    }

    private <T> CompletableFuture<T> call(String rpcName,
                                          Function<WalletGrpc.WalletFutureStub, ListenableFuture<T>> invocation) {
        WalletGrpc.WalletFutureStub stub = futureStub;
        if (stub == null) {
            return CompletableFuture.failedFuture(clientNotReady(rpcName));
        }
        CompletableFuture<T> source;
        try {
            source = toCompletableFuture(invocation.apply(stub));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(toWalletException(rpcName, e));
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        source.whenComplete((response, throwable) -> {
            if (throwable == null) {
                result.complete(response);
            } else if (source.isCancelled()) {
                result.cancel(false);
            } else {
                result.completeExceptionally(toWalletException(rpcName, throwable));
            }
        });
        result.whenComplete((response, throwable) -> {
            if (result.isCancelled()) {
                source.cancel(true);
            }
        });
        return result;
    }

    private <T> T callBlocking(String rpcName, Function<WalletGrpc.WalletBlockingStub, T> invocation) {
        WalletGrpc.WalletBlockingStub stub = blockingStub;
        if (stub == null) {
            throw clientNotReady(rpcName);
        }
        try {
            return invocation.apply(stub);
        } catch (RuntimeException e) {
            throw toWalletException(rpcName, e);
        }
    }

    private static WalletException clientNotReady(String rpcName) {
        String message = rpcName + " called while the wallet client is not initialized";
        log.warn(message);
        return new WalletException(WalletException.Reason.CLIENT_NOT_READY, message, null);
    }

    private static WalletException toWalletException(String rpcName, Throwable throwable) {
        Status status = Status.fromThrowable(throwable);
        WalletException.Reason reason = switch (status.getCode()) {
            case FAILED_PRECONDITION -> WalletException.Reason.WALLET_NOT_OPEN;
            case PERMISSION_DENIED -> WalletException.Reason.WRONG_PASSWORD;
            case UNAVAILABLE -> WalletException.Reason.DAEMON_UNAVAILABLE;
            case UNIMPLEMENTED -> WalletException.Reason.UNSUPPORTED_CALL;
            default -> WalletException.Reason.DAEMON_ERROR;
        };
        String message = rpcName + " failed with " + status.getCode() +
                (status.getDescription() == null ? "" : ": " + status.getDescription());
        log.warn(message);
        return new WalletException(reason, message, throwable);
    }
}
