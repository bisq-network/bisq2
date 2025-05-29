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

package bisq.trade.mu_sig.grpc;

import bisq.common.application.Service;
import bisq.trade.protobuf.MusigGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MusigGrpcClient implements Service {
    private final String host;
    private final int port;
    private ManagedChannel managedChannel;
    @Getter
    private MusigGrpc.MusigBlockingStub blockingStub;
    @Getter
    private MusigGrpc.MusigStub asyncStub;

    public MusigGrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

/*

    private ByteString setupTakerIsSellerTrade(String buyerTradeId, String sellerTradeId) {
        var sellerPubKeyShareResponse = asyncStub.initTrade(PubKeySharesRequest.newBuilder()
                .setTradeId(sellerTradeId)
                .setMyRole(Role.SELLER_AS_TAKER)
                .build());
        System.out.println("Got reply: " + sellerPubKeyShareResponse);

        // Seller sends Message A to buyer.

        var buyerPubKeyShareResponse = asyncStub.initTrade(PubKeySharesRequest.newBuilder()
                .setTradeId(buyerTradeId)
                .setMyRole(Role.BUYER_AS_MAKER)
                .build());
        System.out.println("Got reply: " + buyerPubKeyShareResponse);

        var buyerNonceShareMessage = asyncStub.getNonceShares(NonceSharesRequest.newBuilder()
                .setTradeId(buyerTradeId)
                .setBuyerOutputPeersPubKeyShare(sellerPubKeyShareResponse.getBuyerOutputPubKeyShare())
                .setSellerOutputPeersPubKeyShare(sellerPubKeyShareResponse.getSellerOutputPubKeyShare())
                .setDepositTxFeeRate(50_000)  // 12.5 sats per vbyte
                .setPreparedTxFeeRate(40_000) // 10.0 sats per vbyte
                .setTradeAmount(200_000)
                .setBuyersSecurityDeposit(30_000)
                .setSellersSecurityDeposit(30_000)
                .build());
        System.out.println("Got reply: " + buyerNonceShareMessage);

        // Buyer sends Message B to seller.

        var sellerNonceShareMessage = asyncStub.getNonceShares(NonceSharesRequest.newBuilder()
                .setTradeId(sellerTradeId)
                .setBuyerOutputPeersPubKeyShare(buyerPubKeyShareResponse.getBuyerOutputPubKeyShare())
                .setSellerOutputPeersPubKeyShare(buyerPubKeyShareResponse.getSellerOutputPubKeyShare())
                .setDepositTxFeeRate(50_000)  // 12.5 sats per vbyte
                .setPreparedTxFeeRate(40_000) // 10.0 sats per vbyte
                .setTradeAmount(200_000)
                .setBuyersSecurityDeposit(30_000)
                .setSellersSecurityDeposit(30_000)
                .build());
        System.out.println("Got reply: " + sellerNonceShareMessage);

        var sellerPartialSignatureMessage = asyncStub.getPartialSignatures(PartialSignaturesRequest.newBuilder()
                .setTradeId(sellerTradeId)
                .setPeersNonceShares(buyerNonceShareMessage)
                .addAllReceivers(mockReceivers())
                .build());
        System.out.println("Got reply: " + sellerPartialSignatureMessage);

        // Seller sends Message C to buyer. (Seller's swapTxInputPartialSignature is NOT withheld from it.)


        var buyerPartialSignatureMessage = asyncStub.getPartialSignatures(PartialSignaturesRequest.newBuilder()
                .setTradeId(buyerTradeId)
                .setPeersNonceShares(sellerNonceShareMessage)
                .addAllReceivers(mockReceivers())
                .build());
        System.out.println("Got reply: " + buyerPartialSignatureMessage);

        var buyerDepositPsbt = asyncStub.signDepositTx(DepositTxSignatureRequest.newBuilder()
                .setTradeId(buyerTradeId)
                .setPeersPartialSignatures(sellerPartialSignatureMessage)
                .build());
        System.out.println("Got reply: " + buyerDepositPsbt);

        // Buyer subscribes to be notified of Deposit Tx confirmation:
        var buyerDepositTxConfirmationIter = asyncStub.subscribeTxConfirmationStatus(SubscribeTxConfirmationStatusRequest.newBuilder()
                .setTradeId(buyerTradeId)
                .build());

        // Buyer sends Message D to seller. (Buyer's swapTxInputPartialSignature is withheld from it.)


 // SellerAsTaker / BuyerAsMaker

        var sellerDepositPsbt = asyncStub.signDepositTx(DepositTxSignatureRequest.newBuilder()
                .setTradeId(sellerTradeId)
                // REDACT buyer's swapTxInputPartialSignature (as not yet known by seller):
                .setPeersPartialSignatures(buyerPartialSignatureMessage.toBuilder().clearSwapTxInputPartialSignature())
                .build());
        System.out.println("Got reply: " + sellerDepositPsbt);

        // *** SELLER BROADCASTS DEPOSIT TX ***
        var sellerDepositTxConfirmationIter = asyncStub.publishDepositTx(PublishDepositTxRequest.newBuilder()
                .setTradeId(sellerTradeId)
                .build());
        // ***********************************

        // DELAY: Both traders await Deposit Tx confirmation:
        System.out.println("Awaiting Deposit Tx confirmation...\n");
        buyerDepositTxConfirmationIter.forEachRemaining(reply -> System.out.println("Got reply: " + reply));
        sellerDepositTxConfirmationIter.forEachRemaining(reply -> System.out.println("Got reply: " + reply));

        return buyerPartialSignatureMessage.getSwapTxInputPartialSignature();
    }
    
*/

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        managedChannel = Grpc.newChannelBuilderForAddress(
                host,
                port,
                InsecureChannelCredentials.create()
        ).build();

        try {
            blockingStub = MusigGrpc.newBlockingStub(managedChannel);
            asyncStub = MusigGrpc.newStub(managedChannel);
        } catch (Exception e) {
            log.error("Initializing grpc client failed", e);
            dispose();
            throw e;
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        dispose();
        return CompletableFuture.completedFuture(true);
    }

    private void dispose() {
        if (managedChannel != null) {
            managedChannel.shutdown();
            managedChannel = null;
        }
        blockingStub = null;
        asyncStub = null;
    }
}
