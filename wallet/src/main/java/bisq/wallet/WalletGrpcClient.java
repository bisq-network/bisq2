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
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    public WalletGrpcClient(ManagedChannel managedChannel) {
        this.managedChannel = managedChannel;
        this.host = null;
        this.port = 0;
    }

    public void encryptWallet(String password) {
        var request = EncryptWalletRequest.newBuilder().setPassword(password).build();
        blockingStub.encryptWallet(request);
    }

    public void decryptWallet(String password) {
        var request = DecryptWalletRequest.newBuilder().setPassword(password).build();
        blockingStub.decryptWallet(request);
    }

    public CompletableFuture<GetSeedWordsResponse> getSeedWords() {
        var request = GetSeedWordsRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getSeedWords(request));
    }

    public CompletableFuture<IsWalletReadyResponse> isWalletReady() {
        var request = IsWalletReadyRequest.newBuilder().build();
        return toCompletableFuture(futureStub.isWalletReady(request));
    }

    public CompletableFuture<GetUnusedAddressResponse> getUnusedAddress() {
        var request = GetUnusedAddressRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getUnusedAddress(request));
    }

    public CompletableFuture<GetWalletAddressesResponse> requestWalletAddresses() {
        var request = GetWalletAddressesRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getWalletAddresses(request));
    }

    public CompletableFuture<ListTransactionsResponse> listTransactions() {
        var request = ListTransactionsRequest.newBuilder().build();
        return toCompletableFuture(futureStub.listTransactions(request));
    }

    public CompletableFuture<ListUtxosResponse> listUtxos() {
        var request = ListUtxosRequest.newBuilder().build();
        return toCompletableFuture(futureStub.listUtxos(request));
    }

    public CompletableFuture<SendToAddressResponse> sendToAddress(SendToAddressRequest request) {
        return toCompletableFuture(futureStub.sendToAddress(request));
    }

    public CompletableFuture<IsWalletEncryptedResponse> isWalletEncrypted() {
        var request = IsWalletEncryptedRequest.newBuilder().build();
        return toCompletableFuture(futureStub.isWalletEncrypted(request));
    }

    public CompletableFuture<GetBalanceResponse> requestBalance() {
        var request = GetBalanceRequest.newBuilder().build();
        return toCompletableFuture(futureStub.getBalance(request));
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (managedChannel == null) {
            this.managedChannel = ManagedChannelBuilder.forAddress(this.host, this.port)
                    .usePlaintext()
//                .useTransportSecurity() //Todo turn this on in prod env
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
}
